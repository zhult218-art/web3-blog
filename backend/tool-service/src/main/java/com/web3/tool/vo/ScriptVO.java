package com.web3.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 脚本展示层 VO(tool-service 模块)。
 *
 * <p>脚本接口返回给前端的模型,字段与 {@link com.web3.tool.entity.Script}
 * 一致但不包含逻辑删除标记 deleted。</p>
 */
@Data
public class ScriptVO {
    /** 脚本 ID */
    private Long id;
    /** 脚本名称 */
    private String name;
    /** 脚本描述 */
    private String description;
    /** 脚本源码内容 */
    private String content;
    /** 脚本分类 */
    private String category;
    /** 脚本语言 */
    private String language;
    /** 脚本版本号 */
    private String version;
    /** 下载次数 */
    private Long downloadCount;
    /** 脚本标签 */
    private String tags;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
