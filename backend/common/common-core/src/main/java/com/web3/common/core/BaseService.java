/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.extension.service.IService
 */
package com.web3.common.core;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 类名：BaseService
 * 所属模块：common-core（通用核心模块）
 * 职责：各服务业务 Service 的通用基类接口，继承 MyBatis-Plus 的 IService，直接获得 CRUD、分页等通用能力。
 * 用法：各微服务业务接口（如 UserService）继承本接口后即可复用 MyBatis-Plus 内置方法。
 *
 * @param <T> 对应的实体类型（Entity）
 */
public interface BaseService<T>
extends IService<T> {
}
