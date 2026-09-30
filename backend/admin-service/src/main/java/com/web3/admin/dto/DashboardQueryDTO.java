package com.web3.admin.dto;

import lombok.Data;

/**
 * 类名：DashboardQueryDTO
 * 所属模块：admin-service（管理后台服务）
 * 职责：控制台统计查询参数对象，承载按分类、日期范围及分页查询统计数据的入参（当前预留）。
 */
@Data
public class DashboardQueryDTO {
    private String category;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
