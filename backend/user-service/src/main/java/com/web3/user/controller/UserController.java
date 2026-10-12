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

    /**
     * 写入 auth Cookie。
     *
     * <p>2026-09-17 修订：增加 Secure 标注，由环境变量 {@code COOKIE_SECURE} 控制
     * （默认 true，适合 HTTPS 部署；本地 dev HTTP 调试时设 {@code COOKIE_SECURE=false}）。
     * 同时根据 {@code COOKIE_SAMESITE} 控制 SameSite 策略，默认 Lax，
     * 跨域前后端分离部署可设 {@code COOKIE_SAMESITE=None}（此时强制 Secure=true）。</p>
     */
    private void setAuthCookie(HttpServletResponse response, ApiResponse<JwtVO> result) {
        if (response == null || result == null || result.getData() == null || result.getData().getToken() == null) {
            return;
        }
        String token = result.getData().getToken();
        ResponseCookie cookie = this.buildAuthCookie(token, jwtProperties.getExpire());
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = this.buildAuthCookie("", 0L);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private ResponseCookie buildAuthCookie(String token, long maxAgeMinutes) {
        boolean secure = !"false".equalsIgnoreCase(System.getenv("COOKIE_SECURE"));
        String sameSite = System.getenv().getOrDefault("COOKIE_SAMESITE", "Lax");
        // SameSite=None 必须 Secure，否则浏览器会拒绝
        if ("None".equalsIgnoreCase(sameSite)) {
            secure = true;
        }
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ofMinutes(maxAgeMinutes))
                .build();
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

    /**
     * POST /user/email/code { email, captchaId, captchaCode } → 发送验证码
     *
     * <p>2026-10-10 安全修订：不再回传 devCode 字段。dev 模式（mailEnabled=false）验证码
     * 由 OtpService 写入服务端日志（明文），请从控制台读取；生产模式走真实 SMTP。
     * 移除响应体 devCode 可阻断「发码→取码→登录接管」链路。</p>
     */
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
        otpService.sendEmailCode(email);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
        return ApiResponse.ok(result);
    }

    /**
     * POST /user/email/login { email, code } → 邮箱验证码登录
     *
     * <p>2026-10-10 安全修订：是否自动注册未知邮箱受 {@code app.auth.auto-register} 开关控制，
     * 默认 false（关闭）——未注册邮箱登录返回「邮箱未注册，请先注册」，
     * 防止任意邮箱被自动创建账户接管。设为 true 时恢复自动注册（需配合真实 SMTP）。</p>
     */
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

    /**
     * POST /user/phone/code { phone, captchaId, captchaCode } → 发送短信验证码
     *
     * <p>2026-10-10 安全修订：不再回传 devCode 字段。dev 模式（smsEnabled=false）与网关未接入
     * 的 fallback 路径均由 OtpService 写入服务端日志（明文），请从控制台读取；
     * 生产模式走真实 SMS 网关（见 {@link com.web3.user.service.OtpService#sendSmsByGateway}），
     * 需配置 SMS_PROVIDER/SMS_KEY/SMS_SECRET/SMS_SIGN_NAME/SMS_TEMPLATE_CODE。</p>
     */
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
        otpService.sendPhoneCode(phone);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
        return ApiResponse.ok(result);
    }

    /**
     * POST /user/phone/login { phone, code } → 手机验证码登录
     *
     * <p>2026-10-10 安全修订：是否自动注册未知手机号受 {@code app.auth.auto-register} 开关控制，
     * 默认 false（关闭）——未注册手机号登录返回「手机号未注册，请先注册」。</p>
     */
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
            // supabase 已通过其服务端校验 access_token 确认邮箱所有权，走可信签发路径（允许自动注册）
            ApiResponse<JwtVO> result = userService.emailLogin(email, true);
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
        otpService.sendEmailCode(email);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
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
        otpService.sendPhoneCode(phone);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送");
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

    /** POST /user/profile/verify { realName, idCard } → 提交实名认证申请 */
    @PostMapping("/profile/verify")
    public ApiResponse<UserVO> submitVerification(@RequestBody Map<String, String> body,
                                                   @RequestHeader(value = "Authorization") String auth) {
        Long userId = AuthUtils.requireUserId(auth, jwtUtil, jwtProperties);
        return userService.submitVerification(userId, body.get("realName"), body.get("idCard"));
    }

    /** PUT /user/{id}/verify?verified=2 → 管理员审核实名认证（2=通过, 3=拒绝） */
    @PutMapping("/{id}/verify")
    public ApiResponse<Object> auditVerification(@PathVariable Long id, @RequestParam Integer verified,
                                                  @RequestHeader(value = "Authorization") String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return userService.auditVerification(id, verified);
    }

    // ───────── 服务间内部接口 ─────────

    /**
     * GET /user/internal/all-users → 全量用户轻量目录 [{id,username,nickname,avatar}]
     * 仅供 forum-service 等内部服务拉取（群聊成员列表），X-Internal-Key 鉴权。
     */
    @GetMapping("/internal/all-users")
    public ApiResponse<java.util.List<Map<String, Object>>> allUsersForChat(
            @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        String expected = System.getenv().getOrDefault("INTERNAL_API_KEY", "");
        if (expected.isEmpty() || !expected.equals(key)) {
            return ApiResponse.fail(403, "forbidden");
        }
        return ApiResponse.ok(this.userService.listAllForChat());
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

    /**
     * GET /user/oauth/google/callback?code=xxx&state=xxx → 302 重定向前端。
     *
     * <p>2026-09-17 修订：不再把 JWT 放 URL query string（避免 token 进入浏览器历史、
     * Referer 头、代理日志导致泄露）。改为：</p>
     * <ol>
     *   <li>用 {@link HttpServletResponse#addHeader(String, String)} 写入 HttpOnly+Secure+SameSite
     *       Cookie（复用 {@link #buildAuthCookie}），与密码登录路径一致；</li>
     *   <li>URL 只带一个 <b>一次性</b> state（OAuth 标准的 state 参数透传），用于前端关闭弹窗 /
     *       显示"登录成功"提示，不再暴露任何凭证；</li>
     *   <li>异常路径同样不带任何敏感信息，只透传 errCode。</li>
     * </ol>
     */
    @GetMapping("/oauth/google/callback")
    public ModelAndView googleCallback(@RequestParam String code,
                                       @RequestParam(required = false) String state,
                                       HttpServletResponse response) {
        try {
            String accessToken = googleOAuthService.exchangeCodeForToken(code, state);
            Map<String, String> googleUser = googleOAuthService.fetchUserInfo(accessToken);
            JwtVO jwt = userService.googleLogin(googleUser).getData();
            // token 走 HttpOnly Cookie，不暴露在 URL
            response.addHeader("Set-Cookie", this.buildAuthCookie(jwt.getToken(), jwtProperties.getExpire()).toString());
            String redirectUrl = authProperties.getFrontendUrl() + "/login?oauth=success&state=" + safeState(state);
            return new ModelAndView("redirect:" + redirectUrl);
        } catch (Exception e) {
            String errorUrl = authProperties.getFrontendUrl() + "/login?oauth=error&reason=callback_failed";
            return new ModelAndView("redirect:" + errorUrl);
        }
    }

    /** 把 state 限制为 [A-Za-z0-9_-] 防注入，原值不合法时返回空串。 */
    private String safeState(String state) {
        if (state == null || state.isEmpty()) {
            return "";
        }
        return state.replaceAll("[^A-Za-z0-9_-]", "").substring(0, Math.min(state.length(), 64));
    }

    /**
     * POST /user/oauth/google/dev-login { email, name?, picture? } → Google dev mock 登录
     *
     * <p>2026-09-20 新增：当 {@code GOOGLE_OAUTH_ENABLED=false}（即未配置真实 Google client_id/secret）
     * 时，允许前端传入 mock 的 Google 用户信息（email/name/picture）直接走 {@link UserService#googleLogin}
     * 自动注册/登录链路，方便本地开发测试 Google 登录按钮的整条链路。</p>
     *
     * <p><b>安全约束</b>：仅在 {@code google.enabled=false} 时开放；
     * 一旦生产部署设置 {@code GOOGLE_OAUTH_ENABLED=true} 并配置真实 client_id/secret，
     * 此接口立即返回 403 拒绝服务，强制走真实 OAuth 流程。</p>
     *
     * <p>申请真实 Google client_id/secret 步骤（你本人操作）：
     * 1. 打开 https://console.cloud.google.com/ → 新建/选择项目
     * 2. API &amp; Services → OAuth consent screen → External → 填 App name/email
     * 3. API &amp; Services → Credentials → Create Credentials → OAuth client ID
     *    Application type: Web application
     *    Authorized redirect URIs: http://localhost:8081/user/oauth/google/callback （本地）
     *                              https://你的域名/user/oauth/google/callback （生产）
     * 4. 拿到 Client ID / Client Secret 填入 .env：
     *    GOOGLE_OAUTH_ENABLED=true
     *    GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
     *    GOOGLE_CLIENT_SECRET=xxx
     *    GOOGLE_REDIRECT_URI=http://localhost:8081/user/oauth/google/callback
     * 5. 重启 user-service 后，前端 Google 按钮会跳转真实 Google 授权页</p>
     */
    @PostMapping("/oauth/google/dev-login")
    public ApiResponse<JwtVO> googleDevLogin(@RequestBody Map<String, String> body, HttpServletResponse response) {
        if (authProperties.getGoogle().isEnabled()) {
            // 生产已启用真实 OAuth，禁止 dev mock 防账户接管
            return ApiResponse.fail(403, "Google OAuth 已启用，请使用真实 Google 登录按钮");
        }
        String email = body.get("email");
        if (email == null || email.isBlank() || !email.contains("@")) {
            return ApiResponse.fail(400, "请输入合法邮箱");
        }
        // 构造 mock Google 用户信息，复用 findOrCreateGoogleUser 自动注册链路
        Map<String, String> mockGoogleUser = new HashMap<>();
        mockGoogleUser.put("sub", "dev_" + email.toLowerCase());
        mockGoogleUser.put("email", email);
        mockGoogleUser.put("name", body.getOrDefault("name", email.split("@")[0]));
        mockGoogleUser.put("picture", body.getOrDefault("picture", ""));
        ApiResponse<JwtVO> result = userService.googleLogin(mockGoogleUser);
        this.setAuthCookie(response, result);
        return result;
    }
}
