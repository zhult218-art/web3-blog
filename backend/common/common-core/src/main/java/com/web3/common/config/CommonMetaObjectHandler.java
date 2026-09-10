package com.web3.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 类名：CommonMetaObjectHandler
 * 所属模块：common-core（通用核心模块）
 * 职责：全局 MyBatis-Plus 字段自动填充处理器。
 * 关键注解：@Component 声明为 Spring 组件，由各微服务通过 common 依赖扫描自动加载生效。
 * 说明：所有服务实体上标注 @TableField(fill = INSERT / INSERT_UPDATE) 的字段（createdAt/updatedAt）依赖本 Handler 自动注入时间戳。
 */
@Component
public class CommonMetaObjectHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonMetaObjectHandler.class);

    public CommonMetaObjectHandler() {
        log.info("[CommonMetaObjectHandler] registered OK");
    }

    /**
     * 插入操作填充：自动为实体的 createdAt、updatedAt 字段写入当前时间。
     *
     * @param metaObject MyBatis 元数据对象，用于读取/写入目标实体的填充字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新操作填充：自动将实体的 updatedAt 字段刷新为当前时间。
     *
     * @param metaObject MyBatis 元数据对象，用于读取/写入目标实体的填充字段
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
