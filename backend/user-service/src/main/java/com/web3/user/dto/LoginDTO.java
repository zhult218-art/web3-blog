package com.web3.user.dto;

/**
 * 类名：LoginDTO
 * 所属模块：user-service（用户服务）
 * 职责：登录请求参数对象，承载前端 POST /user/login 提交的账号（用户名/邮箱/手机号）与密码，
 *       以及图形验证码凭据（captchaId + captchaCode，开启人机校验时必填）。
 */
public class LoginDTO {
    /** 账号：支持用户名、邮箱、手机号三种形式自动识别 */
    private String username;
    private String password;
    /** 图形验证码 ID（GET /user/captcha 返回） */
    private String captchaId;
    /** 用户输入的图形验证码 */
    private String captchaCode;

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaId() {
        return this.captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaCode() {
        return this.captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
