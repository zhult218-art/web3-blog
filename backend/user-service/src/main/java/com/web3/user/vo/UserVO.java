package com.web3.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 类名：UserVO
 * 所属模块：user-service（用户服务）
 * 职责：用户信息视图对象，向前端返回用户资料（不含密码等敏感字段），权限以字符串列表形式呈现。
 */
@Data
public class UserVO {
    /** 雪花 ID 超出 JS Number 安全范围，序列化为字符串防止前端精度丢失（如权限回查 404） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String userClass;
    /** 实名认证状态：0=未认证, 1=审核中, 2=已认证, 3=已拒绝 */
    private Integer verified;
    private String bio;
    private String website;
    private String role;
    private List<String> permissions;
    private Integer status;
    private LocalDateTime createdAt;
}
