/*
 * Decompiled with CFR 0.152.
 */
package com.web3.common.core;

import java.util.List;

/**
 * 类名：PageResult
 * 所属模块：common-core（通用核心模块）
 * 职责：统一的分页返回结构，供各微服务分页查询接口使用。
 * 结构：total=总记录数，current=当前页码，size=每页条数，records=当前页数据列表。
 */
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
        PageResult<T> result = new PageResult<T>();
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setRecords(records);
        return result;
    }

    public Long getTotal() {
        return this.total;
    }

    public Long getCurrent() {
        return this.current;
    }

    public Long getSize() {
        return this.size;
    }

    public List<T> getRecords() {
        return this.records;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PageResult)) {
            return false;
        }
        PageResult other = (PageResult)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$total = this.getTotal();
        Long other$total = other.getTotal();
        if (this$total == null ? other$total != null : !((Object)this$total).equals(other$total)) {
            return false;
        }
        Long this$current = this.getCurrent();
        Long other$current = other.getCurrent();
        if (this$current == null ? other$current != null : !((Object)this$current).equals(other$current)) {
            return false;
        }
        Long this$size = this.getSize();
        Long other$size = other.getSize();
        if (this$size == null ? other$size != null : !((Object)this$size).equals(other$size)) {
            return false;
        }
        List<T> this$records = this.getRecords();
        List<T> other$records = other.getRecords();
        return !(this$records == null ? other$records != null : !((Object)this$records).equals(other$records));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PageResult;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $total = this.getTotal();
        result = result * 59 + ($total == null ? 43 : ((Object)$total).hashCode());
        Long $current = this.getCurrent();
        result = result * 59 + ($current == null ? 43 : ((Object)$current).hashCode());
        Long $size = this.getSize();
        result = result * 59 + ($size == null ? 43 : ((Object)$size).hashCode());
        List<T> $records = this.getRecords();
        result = result * 59 + ($records == null ? 43 : ((Object)$records).hashCode());
        return result;
    }

    public String toString() {
        return "PageResult(total=" + this.getTotal() + ", current=" + this.getCurrent() + ", size=" + this.getSize() + ", records=" + this.getRecords() + ")";
    }
}
