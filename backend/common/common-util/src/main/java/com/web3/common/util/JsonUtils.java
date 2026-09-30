package com.web3.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 类名：JsonUtils
 * 所属模块：common-util（通用工具模块）
 * 职责：JSON 序列化/反序列化工具类，基于 Jackson ObjectMapper 封装。
 * 说明：静态初始化 ObjectMapper，忽略未知属性（FAIL_ON_UNKNOWN_PROPERTIES=false），
 *       并注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 时间类型。
 *
 * <p>2026-09-17 重写（CFR 反编译产物 → 语法清洁化）：
 * 去除 CFR 风格的冗余强转（{@code (T)}、{@code (Module)}），并删除随之失效的 Module import。</p>
 */
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());

    /**
     * JSON 字符串反序列化为指定类型对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型 Class
     * @return 反序列化得到的对象
     * @throws RuntimeException 解析失败时抛出
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON parse error", e);
        }
    }

    /**
     * 对象序列化为 JSON 字符串。
     *
     * @param obj 任意对象
     * @return JSON 字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON write error", e);
        }
    }
}
