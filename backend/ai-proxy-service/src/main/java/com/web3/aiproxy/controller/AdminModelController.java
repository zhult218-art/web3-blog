package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.entity.ProxyModel;
import com.web3.aiproxy.mapper.ProxyModelMapper;
import com.web3.aiproxy.service.RedisCacheService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 类名：AdminModelController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：模型倍率管理接口（需管理员 JWT）—— 列表/新增/修改/删除；
 *       支持单倍率与输入输出分离两种模式；变更即时失效 Redis 模型缓存。
 */
@RestController
@RequestMapping("/admin/ai/models")
@RequiredArgsConstructor
public class AdminModelController {

    private final ProxyModelMapper modelMapper;
    private final RedisCacheService redisCacheService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @GetMapping
    public ApiResponse<List<ProxyModel>> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(modelMapper.selectList(
                Wrappers.<ProxyModel>lambdaQuery()
                        .orderByAsc(ProxyModel::getSortOrder)
                        .orderByAsc(ProxyModel::getModelName)));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProxyModel body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        if (!StringUtils.hasText(body.getModelName())) return ApiResponse.fail(400, "modelName is required");
        if (modelMapper.selectCount(Wrappers.<ProxyModel>lambdaQuery()
                .eq(ProxyModel::getModelName, body.getModelName())) > 0) {
            return ApiResponse.fail(409, "model already exists");
        }
        if (body.getModelRate() == null) body.setModelRate(BigDecimal.ONE);
        if (body.getCompletionRate() == null) body.setCompletionRate(BigDecimal.ONE);
        if (body.getRateType() == null) body.setRateType(0);
        if (body.getStatus() == null) body.setStatus(1);
        if (body.getSortOrder() == null) body.setSortOrder(100);
        modelMapper.insert(body);
        redisCacheService.evictModels();
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyModel exist = modelMapper.selectById(id);
        if (exist == null) return ApiResponse.fail(404, "model not found");
        if (body.containsKey("modelRate")) exist.setModelRate(asDecimal(body.get("modelRate"), exist.getModelRate()));
        if (body.containsKey("completionRate")) exist.setCompletionRate(asDecimal(body.get("completionRate"), exist.getCompletionRate()));
        if (body.containsKey("rateType")) exist.setRateType(asInt(body.get("rateType"), exist.getRateType()));
        if (body.containsKey("inputRate")) exist.setInputRate(asDecimal(body.get("inputRate"), exist.getInputRate()));
        if (body.containsKey("outputRate")) exist.setOutputRate(asDecimal(body.get("outputRate"), exist.getOutputRate()));
        if (body.containsKey("maxTokens")) exist.setMaxTokens(asIntBox(body.get("maxTokens")));
        if (body.containsKey("rpmLimit")) exist.setRpmLimit(asIntBox(body.get("rpmLimit")));
        if (body.containsKey("status")) exist.setStatus(asInt(body.get("status"), exist.getStatus()));
        if (body.containsKey("tags")) exist.setTags((String) body.get("tags"));
        if (body.containsKey("sortOrder")) exist.setSortOrder(asInt(body.get("sortOrder"), exist.getSortOrder()));
        if (body.containsKey("remark")) exist.setRemark((String) body.get("remark"));
        modelMapper.updateById(exist);
        redisCacheService.evictModels();
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        modelMapper.deleteById(id);
        redisCacheService.evictModels();
        return ApiResponse.ok();
    }

    // ---------------- 工具 ----------------

    private BigDecimal asDecimal(Object v, BigDecimal def) {
        if (v == null) return def;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Integer asInt(Object v, Integer def) {
        try {
            return v == null ? def : Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private Integer asIntBox(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        return asInt(v, null);
    }
}
