package com.web3.common.core;

import lombok.Data;

import java.util.List;

/**
 * 类名：PageResult
 * 所属模块：common-core（通用核心模块）
 * 职责：统一的分页返回结构，供各微服务分页查询接口使用。
 * 结构：total=总记录数，current=当前页码，size=每页条数，records=当前页数据列表。
 *
 * <p>2026-09-17 重写（CFR 反编译产物 → Lombok @Data）：
 * 原 CFR 手写的 getter/setter/equals/hashCode/toString 全部由 @Data 生成，行为等价。</p>
 */
@Data
public class PageResult<T> {
    private Long total;
    private Long current;
    private Long size;
    private List<T> records;

    /**
     * 静态工厂方法：构造分页结果对象。
     *
     * @param total   总记录数
     * @param current 当前页码
     * @param size    每页条数
     * @param records 当前页记录列表
     * @return 组装完毕的 PageResult 对象
     */
    public static <T> PageResult<T> of(Long total, Long current, Long size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setRecords(records);
        return result;
    }
}
