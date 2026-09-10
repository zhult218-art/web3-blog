package com.web3.user.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.BusinessException;
import com.web3.common.core.PageResult;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.user.config.AuthProperties;
import com.web3.user.dto.LoginDTO;
import com.web3.user.dto.UserRegisterDTO;
import com.web3.user.service.CaptchaService;
import com.web3.user.service.GoogleOAuthService;
import com.web3.user.service.OtpService;
import com.web3.user.service.SupabaseAuthService;
import com.web3.user.service.UserService;
import com.web3.user.vo.JwtVO;
import com.web3.user.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 类名：UserController
 * 所属模块：user-service（用户服务）
 * 职责：用户管理控制器，管理用户注册、登录、个人资料查询/修改、用户列表分页、用户状态变更、管理员增改删用户、权限查询/设置等功能。
 * 关键注解：@RestController 声明为 REST 控制器；@RequestMapping("/user") 对外接口前缀为 /user（经网关转发）。
 * 说明：部分接口通过 AuthUtils.requireAdmin 校验管理员身份（需携带 Authorization 头）。
 */
@RestController
@RequestMapping(value = {"/user"})
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final CaptchaService captchaService;
    private final OtpService otpService;
    private final GoogleOAuthService googleOAuthService;
    private final AuthProperties authProperties;
    private final SupabaseAuthService supabaseAuthService;

    public UserController(UserService userService, JwtUtil jwtUtil, JwtProperties jwtProperties,
                          CaptchaService captchaService, OtpService otpService,
                          GoogleOAuthService googleOAuthService, AuthProperties authProperties,
                          SupabaseAuthService supabaseAuthService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.captchaService = captchaService;
        this.otpService = otpService;
        this.googleOAuthService = googleOAuthService;
        this.authProperties = authProperties;
        this.supabaseAuthService = supabaseAuthService;
    }

        /**
     * 前端调用路由：POST /user/register。
     * 职责：用户注册，校验用户名非空、密码至少 6 位后创建账号，成功返回令牌与用户信息。
     *
     * @param dto 注册参数（username/password/nickname/email）
     * @return code=200 时 data 为 JwtVO（token + 用户信息），否则为失败提示
     */
    @PostMapping(value = {"/register"})
    public ApiResponse<JwtVO> register(@RequestBody UserRegisterDTO dto, HttpServletResponse response) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            return ApiResponse.fail(400, "用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            return ApiResponse.fail(400, "密码至少6位");
        }
        ApiResponse<JwtVO> result = this.userService.register(dto);
        this.setAuthCookie(response, result);
        return result;
    }

    /**
     * 密码登录：POST /user/login
     * 支持用户名/邮箱/手机号（自动识别），captchaId/captchaCode 开启人机校验时必填。
     */
    @PostMapping(value = {"/login"})
    public ApiResponse<JwtVO> login(@RequestBody LoginDTO dto, HttpServletResponse response) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            return ApiResponse.fail(400, "用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            return ApiResponse.fail(400, "密码不能为空");
        }
        if (authProperties.isCaptchaRequired()
                && (dto.getCaptchaId() == null || dto.getCaptchaId().isBlank()
                    || dto.getCaptchaCode() == null || dto.getCaptchaCode().isBlank())) {
            return ApiResponse.fail(400, "请完成图形验证码");
        }
        ApiResponse<JwtVO> result = this.userService.login(dto);
        this.setAuthCookie(response, result);
        return result;
    }

    /**
     * 会话刷新：POST /user/refresh
     * 兼容 Cookie 与 Authorization 头两种凭证来源；刷新成功后重新签发 JWT 并回种 Cookie。
     */
    @PostMapping(value = {"/refresh"})
    public ApiResponse<JwtVO> refresh(@CookieValue(value = "token", required = false) String cookieToken,
                                      @RequestHeader(value = "Authorization", required = false) String auth,
                                      HttpServletResponse response) {
        ApiResponse<JwtVO> result;
        if (cookieToken != null && !cookieToken.isBlank()) {
            result = this.userService.refresh(cookieToken);
        } else if (auth != null && !auth.isBlank()) {
            result = this.userService.refresh(this.resolveTokenHeader(auth));
        } else {
            return ApiResponse.fail(401, "未登录");
        }
        this.setAuthCookie(response, result);
        return result;
    }

    /**
     * 退出登录：POST /user/logout
     * 作废 Redis 会话并清除前端 HttpOnly Cookie。
     */
    @PostMapping(value = {"/logout"})
    public ApiResponse<Object> logout(@CookieValue(value = "token", required = false) String cookieToken,
                                      @RequestHeader(value = "Authorization", required = false) String auth,
                                      HttpServletResponse response) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            this.userService.logout(cookieToken);
        } else if (auth != null && !auth.isBlank()) {
            this.userService.logout(this.resolveTokenHeader(auth));
        }
        this.clearAuthCookie(response);
        return ApiResponse.ok();
    }

    private String resolveTokenHeader(String auth) {
        return auth.startsWith(this.jwtProperties.getPrefix())
                ? auth.substring(this.jwtProperties.getPrefix().length()).trim()
                : auth.trim();
    }

    private void setAuthCookie(HttpServletResponse response, ApiResponse<JwtVO> result) {
        if (response == null || result == null || result.getData() == null || result.getData().getToken() == null) {
            return;
        }
        String token = result.getData().getToken();
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMinutes(jwtProperties.getExpire()))
                .build();
        // 生产环境走 HTTPS 时应追加 .secure(true)。若 CORS 跨域部署，SameSite 建议改为 "None"+secure。
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

        /**
     * 前端调用路由：GET /user/profile。
     * 职责：根据 Authorization 令牌查询当前登录用户信息。
     *
     * @param auth Authorization 请求头（Bearer 令牌），未携带时返回 401
     * @return code=200 时 data 为当前用户 UserVO
     */
    @GetMapping(value = {"/profile"})
    public ApiResponse<UserVO> profile(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || auth.isBlank()) {
            return ApiResponse.fail(401, "未登录");
        }
        return this.userService.profile(auth);
    }

        /**
     * 前端调用路由：PUT /user/profile。
     * 职责：修改当前登录用户的资料（昵称/邮箱）。
     *
     * @param auth Authorization 请求头（Bearer 令牌）
     * @param dto  待更新的用户资料
     * @return code=200 时 data 为更新后的 UserVO
     */
    @PutMapping(value = {"/profile"})
    public ApiResponse<UserVO> updateProfile(@RequestHeader(value = "Authorization") String auth, @RequestBody UserRegisterDTO dto) {
        Long userId = AuthUtils.requireUserId(auth, this.jwtUtil, this.jwtProperties);
        return this.userService.updateProfile(userId, dto);
    }

        /**
     * 前端调用路由：GET /user/count。
     * 职责：统计正常（status=1）用户总数，供站点统计使用。
     *
     * @return code=200 时 data 为用户总数
     */
    @GetMapping(value = {"/count"})
    public ApiResponse<Long> count() {
        return ApiResponse.ok(this.userService.countUsers());
    }

        /**
     * 前端调用路由：GET /user/list（需管理员权限）。
     * 职责：分页查询用户列表，可按用户名/昵称关键字模糊搜索。
     *
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @param keyword 搜索关键字（可选）
     * @param auth    Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为 PageResult<UserVO> 分页结果
     */
    @GetMapping(value = {"/list"})
    public ApiResponse<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return ApiResponse.ok(this.userService.list(page, size, keyword));
    }

    /**
     * 前端调用路由：PUT /user/{id}/status（需管理员权限）。
     * 职责：启用/禁用指定用户（status 1=启用，0=禁用）。
     *
     * @param id     目标用户 ID
     * @param status 目标状态
     * @param auth   Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 表示更新成功
     */
    @PutMapping(value = {"/{id}/status"})
    public ApiResponse<Object> updateStatus(@PathVariable Long id, @RequestParam Integer status,
                                            @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        this.userService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    /**
     * 前端调用路由：POST /user（需管理员权限）。
     * 职责：管理员直接创建用户，可指定角色（ADMIN/USER）。
     *
     * @param dto  新用户信息（username/password/nickname/email）
     * @param role 指定角色，可选，非 ADMIN 一律为 USER
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为新建用户的 UserVO
     */
    @PostMapping
    public ApiResponse<UserVO> createByAdmin(@RequestBody UserRegisterDTO dto,
                                             @RequestParam(required = false) String role,
                                             @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return this.userService.createByAdmin(dto, role);
    }

    /**
     * 前端调用路由：PUT /user/{id}（需管理员权限）。
     * 职责：管理员修改用户资料、密码、角色。
     *
     * @param id   目标用户 ID
     * @param dto  待更新信息（昵称/邮箱/密码）
     * @param role 指定角色（可选）
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为更新后的 UserVO
     */
    @PutMapping(value = {"/{id}"})
    public ApiResponse<UserVO> updateByAdmin(@PathVariable Long id, @RequestBody UserRegisterDTO dto,
                                             @RequestParam(required = false) String role,
                                             @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return this.userService.updateByAdmin(id, dto, role);
    }

    /**
     * 前端调用路由：GET /user/{id}/permissions（需管理员权限）。
     * 职责：查询指定用户的权限列表。
     *
     * @param id   目标用户 ID
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 时 data 为权限字符串列表
     */
    @GetMapping(value = {"/{id}/permissions"})
    public ApiResponse<Object> permissions(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return this.userService.permissions(id);
    }

    /**
     * 前端调用路由：PUT /user/{id}/permissions（需管理员权限）。
     * 职责：覆盖式设置指定用户的权限列表。
     *
     * @param id          目标用户 ID
     * @param permissions 新的权限字符串列表
     * @param auth        Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 表示设置成功
     */
    @PutMapping(value = {"/{id}/permissions"})
    public ApiResponse<Object> updatePermissions(@PathVariable Long id, @RequestBody java.util.List<String> permissions,
                                                 @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        return this.userService.setPermissions(id, permissions);
    }

    /**
     * 前端调用路由：DELETE /user/{id}（需管理员权限）。
     * 职责：逻辑删除指定用户（@TableLogic 软删除）。
     *
     * @param id   目标用户 ID
     * @param auth Authorization 请求头（Bearer 令牌），非管理员返回 403
     * @return code=200 表示删除成功
     */
    @DeleteMapping(value = {"/{id}"})
    public ApiResponse<Object> delete(@PathVariable Long id, @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireAdmin(auth, this.jwtUtil, this.jwtProperties);
        this.userService.deleteById(id);
        return ApiResponse.ok();
    }

    // ───────── 图形验证码 ─────────

    /** GET /user/captcha → { captchaId, image: "data:image/png;base64,..." } */
    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> captcha() {
        return ApiResponse.ok(captchaService.generate());
    }

    // ───────── 邮箱验证码登录 ─────────

    /** POST /user/email/code { email, captchaId, captchaCode } → 发送验证码；dev 模式 data.devCode 返回验证码 */
    @PostMapping("/email/code")
    public ApiResponse<Map<String, String>> sendEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String captchaId = body.get("captchaId");
        String captchaCode = body.get("captchaCode");
        if (email == null || email.isBlank()) return ApiResponse.fail(400, "邮箱不能为空");
        if (authProperties.isCaptchaRequired()) {
            if (captchaId == null || captchaCode == null) return ApiResponse.fail(400, "请完成图形验证码");
            if (!captchaService.verify(captchaId, captchaCode)) return ApiResponse.fail("验证码错误");
        }
        String devCode = otpService.sendEmailCode(email);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
        if (devCode != null) result.put("devCode", devCode);
        return ApiResponse.ok(result);
    }

    /** POST /user/email/login { email, code } → 邮箱验证码登录（成功自动注册） */
    @PostMapping("/email/login")
    public ApiResponse<JwtVO> emailLogin(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || email.isBlank()) return ApiResponse.fail(400, "邮箱不能为空");
        if (code == null || code.isBlank()) return ApiResponse.fail(400, "验证码不能为空");
        if (!otpService.verifyEmailCode(email, code)) return ApiResponse.fail("验证码错误或已过期");
        ApiResponse<JwtVO> result = userService.emailLogin(email);
        this.setAuthCookie(response, result);
        return result;
    }

    // ───────── 手机验证码登录 ─────────

    /** POST /user/phone/code { phone, captchaId, captchaCode } → 发送短信验证码 */
    @PostMapping("/phone/code")
    public ApiResponse<Map<String, String>> sendPhoneCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String captchaId = body.get("captchaId");
        String captchaCode = body.get("captchaCode");
        if (phone == null || phone.isBlank()) return ApiResponse.fail(400, "手机号不能为空");
        if (authProperties.isCaptchaRequired()) {
            if (captchaId == null || captchaCode == null) return ApiResponse.fail(400, "请完成图形验证码");
            if (!captchaService.verify(captchaId, captchaCode)) return ApiResponse.fail("验证码错误");
        }
        String devCode = otpService.sendPhoneCode(phone);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
        if (devCode != null) result.put("devCode", devCode);
        return ApiResponse.ok(result);
    }

    /** POST /user/phone/login { phone, code } → 手机验证码登录（成功自动注册） */
    @PostMapping("/phone/login")
    public ApiResponse<JwtVO> phoneLogin(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String phone = body.get("phone");
        String code = body.get("code");
        if (phone == null || phone.isBlank()) return ApiResponse.fail(400, "手机号不能为空");
        if (code == null || code.isBlank()) return ApiResponse.fail(400, "验证码不能为空");
        if (!otpService.verifyPhoneCode(phone, code)) return ApiResponse.fail("验证码错误或已过期");
        ApiResponse<JwtVO> result = userService.phoneLogin(phone);
        this.setAuthCookie(response, result);
        return result;
    }

    // ───────── Supabase 免密登录（Magic Link） ─────────

    /**
     * POST /user/supabase/exchange { accessToken } → 用 Supabase 令牌换取本应用 JWT
     * 流程：前端魔发链接回跳后拿到 Supabase access_token，本服务交给 Supabase
     *      服务端校验（GET /auth/v1/user）得到 email，再走邮箱自动注册/登录链路。
     */
    @PostMapping("/supabase/exchange")
    public ApiResponse<JwtVO> supabaseExchange(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String accessToken = body.get("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            return ApiResponse.fail(400, "缺少 Supabase 访问令牌");
        }
        try {
            String email = supabaseAuthService.resolveEmail(accessToken);
            ApiResponse<JwtVO> result = userService.emailLogin(email);
            this.setAuthCookie(response, result);
            return result;
        } catch (IllegalStateException e) {
            return ApiResponse.fail(401, e.getMessage());
        }
    }

    // ───────── 绑定邮箱/手机（已登录用户） ─────────

    /** POST /user/bind/email/code { email, captchaId, captchaCode } → 发送绑定验证码 */
    @PostMapping("/bind/email/code")
    public ApiResponse<Map<String, String>> sendBindEmailCode(@RequestBody Map<String, String> body,
                                                              @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String email = body.get("email");
        String captchaId = body.get("captchaId");
        String captchaCode = body.get("captchaCode");
        if (email == null || email.isBlank()) return ApiResponse.fail(400, "邮箱不能为空");
        if (authProperties.isCaptchaRequired()) {
            if (captchaId == null || captchaCode == null) return ApiResponse.fail(400, "请完成图形验证码");
            if (!captchaService.verify(captchaId, captchaCode)) return ApiResponse.fail("验证码错误");
        }
        String devCode = otpService.sendEmailCode(email);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
        if (devCode != null) result.put("devCode", devCode);
        return ApiResponse.ok(result);
    }

    /** POST /user/bind/email { email, code } → 绑定邮箱到当前用户 */
    @PostMapping("/bind/email")
    public ApiResponse<UserVO> bindEmail(@RequestBody Map<String, String> body,
                                          @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String email = body.get("email");
        String code = body.get("code");
        if (email == null || email.isBlank()) return ApiResponse.fail(400, "邮箱不能为空");
        if (code == null || code.isBlank()) return ApiResponse.fail(400, "验证码不能为空");
        if (!otpService.verifyEmailCode(email, code)) return ApiResponse.fail("验证码错误或已过期");
        return userService.bindEmail(userId, email);
    }

    /** POST /user/bind/phone/code { phone, captchaId, captchaCode } → 发送绑定短信验证码 */
    @PostMapping("/bind/phone/code")
    public ApiResponse<Map<String, String>> sendBindPhoneCode(@RequestBody Map<String, String> body,
                                                               @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String phone = body.get("phone");
        String captchaId = body.get("captchaId");
        String captchaCode = body.get("captchaCode");
        if (phone == null || phone.isBlank()) return ApiResponse.fail(400, "手机号不能为空");
        if (authProperties.isCaptchaRequired()) {
            if (captchaId == null || captchaCode == null) return ApiResponse.fail(400, "请完成图形验证码");
            if (!captchaService.verify(captchaId, captchaCode)) return ApiResponse.fail("验证码错误");
        }
        String devCode = otpService.sendPhoneCode(phone);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
        if (devCode != null) result.put("devCode", devCode);
        return ApiResponse.ok(result);
    }

    /** POST /user/bind/phone { phone, code } → 绑定手机号到当前用户 */
    @PostMapping("/bind/phone")
    public ApiResponse<UserVO> bindPhone(@RequestBody Map<String, String> body,
                                          @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String phone = body.get("phone");
        String code = body.get("code");
        if (phone == null || phone.isBlank()) return ApiResponse.fail(400, "手机号不能为空");
        if (code == null || code.isBlank()) return ApiResponse.fail(400, "验证码不能为空");
        if (!otpService.verifyPhoneCode(phone, code)) return ApiResponse.fail("验证码错误或已过期");
        return userService.bindPhone(userId, phone);
    }

    /** PUT /user/profile/avatar { avatar: "https://..." } → 更新头像 */
    @PutMapping("/profile/avatar")
    public ApiResponse<UserVO> updateAvatar(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String avatar = body.get("avatar");
        if (avatar == null || avatar.isBlank()) return ApiResponse.fail(400, "头像地址不能为空");
        return userService.updateAvatar(userId, avatar);
    }

    /** PUT /user/profile/class { userClass: "WARRIOR" } → 选择异世界职业 */
    @PutMapping("/profile/class")
    public ApiResponse<UserVO> chooseClass(@RequestBody Map<String, String> body,
                                            @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        String userClass = body.get("userClass");
        if (userClass == null || userClass.isBlank()) return ApiResponse.fail(400, "职业不能为空");
        return userService.chooseClass(userId, userClass);
    }

    // ───────── Google OAuth ─────────

    /** GET /user/oauth/google/url → { url } 或 url=null（未启用时） */
    @GetMapping("/oauth/google/url")
    public ApiResponse<Map<String, String>> googleUrl() {
        String url = googleOAuthService.buildAuthorizeUrl();
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        result.put("enabled", authProperties.getGoogle().isEnabled() ? "true" : "false");
        return ApiResponse.ok(result);
    }

    /** GET /user/oauth/google/callback?code=xxx&state=xxx → 302 重定向前端（带 access_token） */
    @GetMapping("/oauth/google/callback")
    public ModelAndView googleCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        try {
            String accessToken = googleOAuthService.exchangeCodeForToken(code, state);
            Map<String, String> googleUser = googleOAuthService.fetchUserInfo(accessToken);
            JwtVO jwt = userService.googleLogin(googleUser).getData();
            String redirectUrl = authProperties.getFrontendUrl() + "/login?access_token=" + jwt.getToken();
            return new ModelAndView("redirect:" + redirectUrl);
        } catch (Exception e) {
            String errorUrl = authProperties.getFrontendUrl() + "/login?auth_error=" + e.getMessage();
            return new ModelAndView("redirect:" + errorUrl);
        }
    }
}
