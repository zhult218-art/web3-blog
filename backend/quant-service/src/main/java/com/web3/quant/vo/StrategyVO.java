/*
 * Decompiled with CFR 0.152.
 */
package com.web3.quant.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略展示层 VO(quant-service 模块)。
 *
 * <p>策略接口返回给前端的模型,字段与 {@link com.web3.quant.entity.Strategy}
 * 一致但不包含逻辑删除标记 deleted。</p>
 */
public class StrategyVO {
    /** 策略 ID */
    private Long id;
    /** 策略名称 */
    private String name;
    /** 策略描述 */
    private String description;
    /** 策略状态 */
    private String status;
    /** 策略代码内容 */
    private String code;
    /** 策略收益率 */
    private BigDecimal returns;
    /** 风险等级 */
    private String riskLevel;
    /** 策略标签 */
    private String tags;
    /** 回测数据(JSON) */
    private String backtestData;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public String getCode() {
        return this.code;
    }

    public BigDecimal getReturns() {
        return this.returns;
    }

    public String getRiskLevel() {
        return this.riskLevel;
    }

    public String getTags() {
        return this.tags;
    }

    public String getBacktestData() {
        return this.backtestData;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setReturns(BigDecimal returns) {
        this.returns = returns;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setBacktestData(String backtestData) {
        this.backtestData = backtestData;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StrategyVO)) {
            return false;
        }
        StrategyVO other = (StrategyVO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$code = this.getCode();
        String other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
            return false;
        }
        BigDecimal this$returns = this.getReturns();
        BigDecimal other$returns = other.getReturns();
        if (this$returns == null ? other$returns != null : !((Object)this$returns).equals(other$returns)) {
            return false;
        }
        String this$riskLevel = this.getRiskLevel();
        String other$riskLevel = other.getRiskLevel();
        if (this$riskLevel == null ? other$riskLevel != null : !this$riskLevel.equals(other$riskLevel)) {
            return false;
        }
        String this$tags = this.getTags();
        String other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) {
            return false;
        }
        String this$backtestData = this.getBacktestData();
        String other$backtestData = other.getBacktestData();
        if (this$backtestData == null ? other$backtestData != null : !this$backtestData.equals(other$backtestData)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StrategyVO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        BigDecimal $returns = this.getReturns();
        result = result * 59 + ($returns == null ? 43 : ((Object)$returns).hashCode());
        String $riskLevel = this.getRiskLevel();
        result = result * 59 + ($riskLevel == null ? 43 : $riskLevel.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        String $backtestData = this.getBacktestData();
        result = result * 59 + ($backtestData == null ? 43 : $backtestData.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "StrategyVO(id=" + this.getId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", code=" + this.getCode() + ", returns=" + this.getReturns() + ", riskLevel=" + this.getRiskLevel() + ", tags=" + this.getTags() + ", backtestData=" + this.getBacktestData() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
