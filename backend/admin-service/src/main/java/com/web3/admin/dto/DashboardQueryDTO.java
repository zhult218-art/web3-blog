/*
 * Decompiled with CFR 0.152.
 */
package com.web3.admin.dto;

/**
 * 类名：DashboardQueryDTO
 * 所属模块：admin-service（管理后台服务）
 * 职责：控制台统计查询参数对象，承载按分类、日期范围及分页查询统计数据的入参（当前预留）。
 */
public class DashboardQueryDTO {
    private String category;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public String getCategory() {
        return this.category;
    }

    public String getStartDate() {
        return this.startDate;
    }

    public String getEndDate() {
        return this.endDate;
    }

    public Integer getPageNum() {
        return this.pageNum;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DashboardQueryDTO)) {
            return false;
        }
        DashboardQueryDTO other = (DashboardQueryDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$pageNum = this.getPageNum();
        Integer other$pageNum = other.getPageNum();
        if (this$pageNum == null ? other$pageNum != null : !((Object)this$pageNum).equals(other$pageNum)) {
            return false;
        }
        Integer this$pageSize = this.getPageSize();
        Integer other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !((Object)this$pageSize).equals(other$pageSize)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$startDate = this.getStartDate();
        String other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) {
            return false;
        }
        String this$endDate = this.getEndDate();
        String other$endDate = other.getEndDate();
        return !(this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DashboardQueryDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $pageNum = this.getPageNum();
        result = result * 59 + ($pageNum == null ? 43 : ((Object)$pageNum).hashCode());
        Integer $pageSize = this.getPageSize();
        result = result * 59 + ($pageSize == null ? 43 : ((Object)$pageSize).hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $startDate = this.getStartDate();
        result = result * 59 + ($startDate == null ? 43 : $startDate.hashCode());
        String $endDate = this.getEndDate();
        result = result * 59 + ($endDate == null ? 43 : $endDate.hashCode());
        return result;
    }

    public String toString() {
        return "DashboardQueryDTO(category=" + this.getCategory() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", pageNum=" + this.getPageNum() + ", pageSize=" + this.getPageSize() + ")";
    }
}
