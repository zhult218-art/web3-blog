package com.web3.forum.dto;

import java.util.List;

/**
 * BatchModerateRequest —— 管理端批量审核/删除请求体
 * <p>
 * ids：目标帖子/评论 ID 列表；action：批量动作（approve=通过 / reject=驳回 / delete=删除）。
 */
public class BatchModerateRequest {
    /** 目标内容 ID 列表 */
    private List<Long> ids;
    /** 批量动作：approve / reject / delete */
    private String action;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
