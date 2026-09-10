package com.web3.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 类名：VisitLog
 * 所属模块：admin-service（管理后台服务）
 * 职责：站点访问日志实体类，对应数据库表 visit_log，记录每次页面访问的 IP、地理信息、经纬度、UA 与页面路径，用于访客分析。
 * 关键注解：@TableName("visit_log") 指定表名；@TableId(type = AUTO) 自增主键；deleted=逻辑删除标记（@TableLogic）。
 */
@TableName(value = "visit_log")
public class VisitLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ip;
    private String country;
    private String region;
    private String city;
    private String isp;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String userAgent;
    private String pagePath;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;

    public Long getId() { return this.id; }
    public String getIp() { return this.ip; }
    public String getCountry() { return this.country; }
    public String getRegion() { return this.region; }
    public String getCity() { return this.city; }
    public String getIsp() { return this.isp; }
    public BigDecimal getLatitude() { return this.latitude; }
    public BigDecimal getLongitude() { return this.longitude; }
    public String getUserAgent() { return this.userAgent; }
    public String getPagePath() { return this.pagePath; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public Integer getDeleted() { return this.deleted; }

    public void setId(Long id) { this.id = id; }
    public void setIp(String ip) { this.ip = ip; }
    public void setCountry(String country) { this.country = country; }
    public void setRegion(String region) { this.region = region; }
    public void setCity(String city) { this.city = city; }
    public void setIsp(String isp) { this.isp = isp; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setPagePath(String pagePath) { this.pagePath = pagePath; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}