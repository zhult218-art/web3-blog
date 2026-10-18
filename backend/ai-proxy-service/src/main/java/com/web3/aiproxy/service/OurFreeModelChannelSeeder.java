package com.web3.aiproxy.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web3.aiproxy.config.AiProxyProperties;
import com.web3.aiproxy.entity.ProxyChannel;
import com.web3.aiproxy.entity.ProxyModel;
import com.web3.aiproxy.mapper.ProxyChannelMapper;
import com.web3.aiproxy.mapper.ProxyModelMapper;
import com.web3.aiproxy.util.AesGcmUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 类名：OurFreeModelChannelSeeder
 * 所属模块：ai-proxy-service（中转站）
 * 职责：启动期幂等播种本机免费模型池渠道（dsh-our-free-model，默认 127.0.0.1:18900）。
 *       把免费池接入计费中转链路：用户 sk- 令牌经中转转发到本机免费上游（上游成本≈0，
 *       用户照常计费）。密钥取 OUR_FREE_MODEL_KEY 环境变量，否则读 OFM 数据目录
 *       settings.json 的 forwardKey；地址取 OUR_FREE_MODEL_BASE_URL。解析不到密钥时
 *       静默跳过（info 级）。全程 try/catch，绝不抛异常、绝不阻塞或拖慢应用启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OurFreeModelChannelSeeder implements ApplicationRunner {

    /** 渠道名（唯一键 uk_name，据此幂等判断是否存在） */
    private static final String CHANNEL_NAME = "Our Free Model(免费池)";
    /** 免费池默认上游地址（可用 OUR_FREE_MODEL_BASE_URL 覆盖） */
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:18900/v1";
    /** 数据集聚合服务的模型池 id（与 py-service 侧 quant 预设保持一致） */
    private static final List<String> MODEL_IDS = List.of(
            "deepseek-v4.1-flash-free",
            "glm-5.3-flash-free",
            "mimo-v2.6-flash-free");
    /** 免费渠道优先级：高于既有渠道默认 0，确保优先被路由选中 */
    private static final int CHANNEL_PRIORITY = 100;

    private final ProxyChannelMapper channelMapper;
    private final ProxyModelMapper modelMapper;
    private final AiProxyProperties properties;
    private final ChannelRouterService channelRouter;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String key = resolveKey();
            if (!StringUtils.hasText(key)) {
                log.info("[ofm-seed] 未解析到 Our Free Model 密钥（OUR_FREE_MODEL_KEY 为空且 settings.json 不可读），跳过播种");
                return;
            }
            String baseUrl = resolveBaseUrl();
            upsertChannel(baseUrl.trim(), key.trim());
            ensureModels();
        } catch (Exception e) {
            // 播种失败绝不能影响中转主链路，故只记 warn
            log.warn("[ofm-seed] 播种 Our Free Model 渠道失败，已忽略: {}", e.getMessage());
        }
    }

    /** 幂等 upsert 唯一渠道：存在则刷新 cipher/base_url/models/status/priority，否则插入 */
    private void upsertChannel(String baseUrl, String plainKey) {
        String modelsJson = toJsonArray(MODEL_IDS);
        String cipher = AesGcmUtils.encrypt(properties.getChannelKeySecret(), plainKey);

        ProxyChannel exist = channelMapper.selectOne(Wrappers.<ProxyChannel>lambdaQuery()
                .eq(ProxyChannel::getName, CHANNEL_NAME));
        if (exist != null) {
            exist.setType(1);
            exist.setBaseUrl(baseUrl);
            exist.setApiKeyCipher(cipher);
            exist.setModels(modelsJson);
            exist.setStatus(1);
            exist.setPriority(CHANNEL_PRIORITY);
            exist.setUpdatedAt(LocalDateTime.now());
            channelMapper.updateById(exist);
            log.info("[ofm-seed] 已刷新免费池渠道 '{}' (id={})", CHANNEL_NAME, exist.getId());
        } else {
            ProxyChannel ch = new ProxyChannel();
            ch.setName(CHANNEL_NAME);
            ch.setType(1);
            ch.setBaseUrl(baseUrl);
            ch.setApiKeyCipher(cipher);
            ch.setModels(modelsJson);
            ch.setWeight(1);
            ch.setPriority(CHANNEL_PRIORITY);
            ch.setStatus(1);
            ch.setFailCount(0);
            ch.setSuccessCount(0L);
            ch.setAutoDisabled(0);
            ch.setRemark("本机免费模型池（dsh-our-free-model）自动播种");
            ch.setCreatedAt(LocalDateTime.now());
            ch.setUpdatedAt(LocalDateTime.now());
            channelMapper.insert(ch);
            log.info("[ofm-seed] 已创建免费池渠道 '{}' (id={})", CHANNEL_NAME, ch.getId());
        }
        channelRouter.evictCache();
    }

    /** 幂等补齐免费池各模型的倍率行（已存在的 model_name 跳过） */
    private void ensureModels() {
        for (String modelId : MODEL_IDS) {
            Long count = modelMapper.selectCount(Wrappers.<ProxyModel>lambdaQuery()
                    .eq(ProxyModel::getModelName, modelId));
            if (count != null && count > 0) {
                continue;
            }
            ProxyModel m = new ProxyModel();
            m.setModelName(modelId);
            m.setRateType(0);
            // 倍率约定同既有种子（如 deepseek-chat）：单倍率 1.0/1.0
            m.setModelRate(new BigDecimal("1.0000"));
            m.setCompletionRate(new BigDecimal("1.0000"));
            m.setStatus(1);
            m.setSortOrder(95);
            m.setRemark("本机免费模型池（自动播种）");
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            modelMapper.insert(m);
            log.info("[ofm-seed] 已创建模型倍率行 '{}'", modelId);
        }
    }

    /** 密钥：OUR_FREE_MODEL_KEY 环境变量优先，否则读 OFM settings.json 的 forwardKey */
    private String resolveKey() throws Exception {
        String envKey = System.getenv("OUR_FREE_MODEL_KEY");
        if (StringUtils.hasText(envKey)) {
            return envKey.trim();
        }
        Path settings = ofmHome().resolve("settings.json");
        if (!Files.isRegularFile(settings)) {
            return null;
        }
        JsonNode root = objectMapper.readTree(Files.readString(settings, StandardCharsets.UTF_8));
        String key = root.path("forwardKey").asText("");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String resolveBaseUrl() {
        String envBase = System.getenv("OUR_FREE_MODEL_BASE_URL");
        return StringUtils.hasText(envBase) ? envBase.trim() : DEFAULT_BASE_URL;
    }

    /** OFM 数据目录：OFM_HOME 优先，否则用户主目录下的 .our-free-model（与 py-service 一致） */
    private Path ofmHome() {
        String envHome = System.getenv("OFM_HOME");
        if (StringUtils.hasText(envHome)) {
            return Path.of(envHome.trim());
        }
        return Path.of(System.getProperty("user.home", new File(".").getAbsolutePath()), ".our-free-model");
    }

    /** 生成 ChannelRouterService#parseArray 期望的 JSON 数组字符串，如 ["a","b"] */
    private String toJsonArray(List<String> ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(ids.get(i).replace("\"", "")).append('"');
        }
        return sb.append(']').toString();
    }
}
