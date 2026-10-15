package com.web3.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.BusinessException;
import com.web3.common.core.PageResult;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.user.config.AuthProperties;
import com.web3.user.dto.LoginDTO;
import com.web3.user.dto.UserRegisterDTO;
import com.web3.user.entity.User;
import com.web3.user.mapper.UserMapper;
import com.web3.user.vo.JwtVO;
import com.web3.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 类名：UserService
 * 所属模块：user-service（用户服务）
 * 职责：用户业务逻辑服务，负责注册、登录、资料查询/更新、管理员建户改户删户、用户分页列表、状态管理、权限查询/设置等业务实现。
 * 关键注解：@Service 注册为 Spring 业务 Bean。
 * 说明：登录/注册成功后签发 JWT 并写入 Redis（键 token:{userId}，有效期取 jwt.expire 分钟），供后续会话校验。
 *       注册成功后异步调用 forum-service 内部接口，自动把用户加入所有公开群。
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final OtpService otpService;
    private final GoogleOAuthService googleOAuthService;
    private final AuthProperties authProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public UserService(UserMapper userMapper, StringRedisTemplate redisTemplate, JwtUtil jwtUtil, JwtProperties jwtProperties, PasswordEncoder passwordEncoder,
                       CaptchaService captchaService, OtpService otpService, GoogleOAuthService googleOAuthService,
                       AuthProperties authProperties) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.otpService = otpService;
        this.googleOAuthService = googleOAuthService;
        this.authProperties = authProperties;
    }

    private void autoJoinChatRooms(Long userId, String username, String nickname) {
        autoJoinChatRooms(userId, username, nickname, null);
    }

    /**
     * 注册后自动加入所有公开群（调用 forum-service 内部接口）。
     * 失败不影响注册主流程，仅记录日志。
     */
    private void autoJoinChatRooms(Long userId, String username, String nickname, String avatar) {
        try {
            String forumUrl = System.getenv().getOrDefault("FORUM_SERVICE_URL", "http://127.0.0.1:8083");
            String internalKey = System.getenv().getOrDefault("INTERNAL_API_KEY", "");
            if (internalKey.isBlank()) return; // 未配置内部密钥则跳过
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Key", internalKey);
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("userId", userId);
            body.put("username", username);
            body.put("nickname", nickname == null ? username : nickname);
            if (avatar != null) body.put("avatar", avatar);
            restTemplate.postForEntity(forumUrl + "/chat/internal/auto-join",
                    new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            // forum-service 不可用或内部密钥不匹配时静默失败，不影响注册
            log.debug("[user] auto-join chat rooms failed (forum-service unavailable): {}", e.getMessage());
        }
    }

    /**
     * 判断用户名是否属于管理员名单（环境变量 ADMIN_USERNAMES，逗号分隔）。
     *
     * <p>安全说明：历史上此处默认值为 "admin"，意味着任何人只要注册一个叫 admin 的账号就能拿到 ADMIN
     * 角色。该默认值已于 2026-09-17 移除，现在 ADMIN 角色只能通过以下途径获得：</p>
     * <ol>
     *   <li>管理员在后台通过 createByAdmin/updateByAdmin 显式授权；</li>
     *   <li>启动时通过环境变量 ADMIN_USERNAMES=xxx,yyy 显式声明允许提权的用户名（仅推荐用于首次种子账号）。</li>
     * </ol>
     *
     * @param username 用户名
     * @return true=该用户名在白名单中（白名单为空时永远返回 false）
     */
    private boolean isAdminUsername(String username) {
        String admins = System.getenv("ADMIN_USERNAMES");
        if (admins == null || admins.isBlank()) {
            return false;
        }
        return Arrays.stream(admins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).anyMatch(name -> name.equals(username));
    }

    /**
     * 用户注册：校验用户名唯一 -> BCrypt 加密密码 -> 判定角色 -> 入库 -> 签发 JWT 并缓存至 Redis。
     *
     * @param dto 注册参数（username/password/nickname/email）
     * @return 成功：code=200，data 为 JwtVO（token + 用户信息）；用户名已存在：code=500
     */
    public ApiResponse<JwtVO> register(UserRegisterDTO dto) {
        User exist = this.userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exist != null) {
            return ApiResponse.fail("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole(isAdminUsername(dto.getUsername()) ? "ADMIN" : "USER");
        this.userMapper.insert(user);
        String token = this.jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole(), user.getNickname(), user.getPermissions());
        this.redisTemplate.opsForValue().set("token:" + user.getId(), token, this.jwtProperties.getRefresh() * 60L, TimeUnit.SECONDS);
        autoJoinChatRooms(user.getId(), user.getUsername(), user.getNickname());
        JwtVO vo = new JwtVO();
        vo.setToken(token);
        vo.setUser(this.toUserVO(user));
        return ApiResponse.ok(vo);
    }

    /**
     * 用户登录：支持用户名/邮箱/手机号三种形式（自动识别），含图形验证码校验。
     *
     * @param dto 登录参数（username/account + password + captchaId + captchaCode）
     * @return 成功：code=200，data 为 JwtVO；用户名或密码错误：code=500
     */
    public ApiResponse<JwtVO> login(LoginDTO dto) {
        if (dto.getCaptchaId() != null && dto.getCaptchaCode() != null) {
            if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
                return ApiResponse.fail("验证码错误");
            }
        }
        String account = dto.getUsername().trim();
        User user = findUserByAccount(account);
        if (user == null || !this.passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return ApiResponse.fail("用户名或密码错误");
        }
        return issueToken(user);
    }

    /**
     * 邮箱验证码登录（验证码已由 Controller 层校验通过）。
     *
     * <p>2026-10-10 安全修订：未知邮箱是否自动注册受 {@link AuthProperties#isAutoRegister()} 控制，
     * 默认 false（关闭）——未注册邮箱返回「邮箱未注册，请先注册」，防止任意邮箱被自动创建账户。
     * 显式设为 true 时恢复自动注册（需配合真实 SMTP，避免 devCode 泄露链路）。</p>
     */
    public ApiResponse<JwtVO> emailLogin(String email) {
        return emailLogin(email, false);
    }

    /**
     * 邮箱登录（验证码链路与可信第三方签发链路共用）。
     *
     * @param email          邮箱
     * @param trustedIssuer  true 表示邮箱所有权已由可信第三方（如 Supabase 服务端校验 access_token）
     *                       验证，无视 auto-register 开关强制允许自动注册；
     *                       false 表示走验证码登录链路，按 auto-register 开关决定是否自动注册
     */
    public ApiResponse<JwtVO> emailLogin(String email, boolean trustedIssuer) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            if (!trustedIssuer && !authProperties.isAutoRegister()) {
                return ApiResponse.fail("邮箱未注册，请先注册");
            }
            // 自动注册（开关已开启，或由可信第三方签发）
            user = new User();
            String localPart = email.split("@")[0];
            String username = "e_" + localPart.replaceAll("[^a-zA-Z0-9_]", "_");
            String base = username;
            int suffix = 0;
            while (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
                suffix++;
                username = base + suffix;
            }
            user.setUsername(username);
            user.setNickname(localPart);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString().replace("-", "")));
            user.setRole("USER");
            userMapper.insert(user);
            autoJoinChatRooms(user.getId(), user.getUsername(), user.getNickname());
        }
        return issueToken(user);
    }

    /**
     * 手机验证码登录（验证码已由 Controller 层校验通过）。
     *
     * <p>2026-10-10 安全修订：未知手机号是否自动注册受 {@link AuthProperties#isAutoRegister()} 控制，
     * 默认 false（关闭）——未注册手机号返回「手机号未注册，请先注册」。</p>
     */
    public ApiResponse<JwtVO> phoneLogin(String phone) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            if (!authProperties.isAutoRegister()) {
                return ApiResponse.fail("手机号未注册，请先注册");
            }
            user = new User();
            String username = "p_" + phone.substring(Math.max(0, phone.length() - 6));
            String base = username;
            int suffix = 0;
            while (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
                suffix++;
                username = base + suffix;
            }
            user.setUsername(username);
            user.setNickname("用户" + phone.substring(Math.max(0, phone.length() - 4)));
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString().replace("-", "")));
            user.setRole("USER");
            userMapper.insert(user);
            autoJoinChatRooms(user.getId(), user.getUsername(), user.getNickname());
        }
        return issueToken(user);
    }

    /** Google OAuth 登录（信息已由 GoogleOAuthService 处理） */
    public ApiResponse<JwtVO> googleLogin(Map<String, String> googleUser) {
        User user = googleOAuthService.findOrCreateGoogleUser(googleUser);
        return issueToken(user);
    }

    /** 绑定邮箱到已有用户 */
    public ApiResponse<UserVO> bindEmail(Long userId, String email) {
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        user.setEmail(email);
        userMapper.updateById(user);
        return ApiResponse.ok(toUserVO(user));
    }

    /** 绑定手机号到已有用户 */
    public ApiResponse<UserVO> bindPhone(Long userId, String phone) {
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        user.setPhone(phone);
        userMapper.updateById(user);
        return ApiResponse.ok(toUserVO(user));
    }

    /** 更新用户头像 */
    public ApiResponse<UserVO> updateAvatar(Long userId, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        user.setAvatar(avatar);
        userMapper.updateById(user);
        return ApiResponse.ok(toUserVO(user));
    }

    /** 选择异世界职业 */
    public ApiResponse<UserVO> chooseClass(Long userId, String userClass) {
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        java.util.List<String> valid = java.util.List.of("WARRIOR","MAGE","HEALER","ASSASSIN","RANGER","PALADIN","NECROMANCER","BERSERKER");
        if (!valid.contains(userClass.toUpperCase())) return ApiResponse.fail(400, "无效的职业");
        user.setUserClass(userClass.toUpperCase());
        userMapper.updateById(user);
        return ApiResponse.ok(toUserVO(user));
    }

    /** 根据输入自动识别账号类型（用户名/邮箱/手机号）并查找用户 */
    private User findUserByAccount(String account) {
        if (account.contains("@")) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, account));
        }
        if (account.matches("^\\d{7,15}$")) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, account));
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, account));
    }

    /** 签发 JWT 并写入 Redis，返回 JwtVO（perms 声明随签发写入，管理员改权限后重新登录/续期生效） */
    private ApiResponse<JwtVO> issueToken(User user) {
        Integer verified = user.getVerified() != null ? user.getVerified() : 0;
        String token = this.jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole(), user.getNickname(), user.getPermissions(), verified);
        // Redis 会话 TTL 与 JWT/Cookie 一致取 jwt.refresh（7 天），保证刷新页面后 Cookie 仍能命中 Redis 续期
        this.redisTemplate.opsForValue().set("token:" + user.getId(), token, this.jwtProperties.getRefresh() * 60L, TimeUnit.SECONDS);
        JwtVO vo = new JwtVO();
        vo.setToken(token);
        vo.setUser(this.toUserVO(user));
        return ApiResponse.ok(vo);
    }

    /**
     * 查询当前登录用户信息：解析令牌 -> 按 userId 查库。
     *
     * @param auth Authorization 请求头（Bearer 令牌）
     * @return 成功：code=200，data 为 UserVO；令牌失效：code=401；用户不存在：code=404
     */
    public ApiResponse<UserVO> profile(String auth) {
        String token = auth.replace(this.jwtProperties.getPrefix(), "").trim();
        if (!this.jwtUtil.validate(token)) {
            return ApiResponse.fail(401, "登录已过期");
        }
        Claims claims = this.jwtUtil.parseClaims(token);
        Long userId = claims.get("userId", Long.class);
        User user = this.userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        return ApiResponse.ok(this.toUserVO(user));
    }

    /**
     * 会话续期：校验令牌并重新签发 JWT（HttpOnly Cookie 会话刷新用），
     * 令牌失效/用户不存在/被禁用时返回 401。
     *
     * @param token JWT 原文（非 Bearer 前缀）
     * @return 成功：code=200，data 为 JwtVO；失败：code=401
     */
    public ApiResponse<JwtVO> refresh(String token) {
        if (token == null || token.isBlank()) {
            return ApiResponse.fail(401, "未登录");
        }
        if (!this.jwtUtil.validate(token)) {
            return ApiResponse.fail(401, "登录已过期");
        }
        Claims claims = this.jwtUtil.parseClaims(token);
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            return ApiResponse.fail(401, "登录已过期");
        }
        String redisKey = "token:" + userId;
        String stored = this.redisTemplate.opsForValue().get(redisKey);
        if (stored == null || !stored.equals(token)) {
            // 会话已退出或被新登录覆盖，禁止通过旧令牌续期
            return ApiResponse.fail(401, "登录已过期");
        }
        User user = this.userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail(401, "登录已过期");
        }
        Integer status = user.getStatus();
        if (status == null || status.intValue() != 1) {
            return ApiResponse.fail(401, "账号已被禁用");
        }
        return this.issueToken(user);
    }

    /**
     * 退出登录：作废 Redis 中的会话记录（若令牌有效）。
     *
     * @param token JWT 原文（可为空）
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            if (this.jwtUtil.validate(token)) {
                Claims claims = this.jwtUtil.parseClaims(token);
                Long userId = claims.get("userId", Long.class);
                if (userId != null) {
                    this.redisTemplate.delete("token:" + userId);
                }
            }
        } catch (Exception e) {
            // 令牌已失效时无需作废
            log.debug("[user] logout token invalidate skipped: {}", e.getMessage());
        }
    }

    /**
     * 更新当前用户资料（昵称/邮箱）。
     *
     * @param userId 当前登录用户 ID
     * @param dto    待更新的字段（nickname/email，为 null 时不更新）
     * @return 成功：code=200，data 为更新后的 UserVO；用户不存在：code=404
     */
    public ApiResponse<UserVO> updateProfile(Long userId, UserRegisterDTO dto) {
        User user = this.userMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        this.userMapper.updateById(user);
        return ApiResponse.ok(this.toUserVO(user));
    }

    /**
     * 更新用户状态（启用/禁用）。
     *
     * @param id     目标用户 ID
     * @param status 目标状态（1=启用，0=禁用）
     * @throws RuntimeException 用户不存在时抛出
     */
    public void updateStatus(Long id, Integer status) {
        User user = this.userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        this.userMapper.updateById(user);
    }

    /**
     * 管理员创建用户：默认启用状态（status=1），可按角色参数指定 ADMIN/USER。
     *
     * @param dto  新用户信息（username/password/nickname/email）
     * @param role 指定角色，非 ADMIN 一律为 USER
     * @return 成功：code=200，data 为新建用户 UserVO；用户名已存在或密码不足 6 位：code=400/500
     */
    public ApiResponse<UserVO> createByAdmin(UserRegisterDTO dto, String role) {
        User exist = this.userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exist != null) {
            return ApiResponse.fail("用户名已存在");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            return ApiResponse.fail(400, "密码至少6位");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole("ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
        user.setStatus(1);
        this.userMapper.insert(user);
        return ApiResponse.ok(this.toUserVO(user));
    }

    /**
     * 管理员更新用户：可更新昵称、邮箱、密码（重置时加密）、角色。
     *
     * @param id   目标用户 ID
     * @param dto  待更新信息
     * @param role 指定角色（可选）
     * @return 成功：code=200，data 为更新后的 UserVO；用户不存在：code=404；密码不足 6 位：code=400
     */
    public ApiResponse<UserVO> updateByAdmin(Long id, UserRegisterDTO dto, String role) {
        User user = this.userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 6) {
                return ApiResponse.fail(400, "密码至少6位");
            }
            user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
        }
        if (role != null && !role.isBlank()) {
            user.setRole("ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
        }
        this.userMapper.updateById(user);
        return ApiResponse.ok(this.toUserVO(user));
    }

    /**
     * 删除用户（MyBatis-Plus 逻辑删除）。
     *
     * @param id 目标用户 ID
     */
    public void deleteById(Long id) {
        this.userMapper.deleteById(id);
    }

    /**
     * 分页查询用户列表，按创建时间倒序，支持用户名/昵称模糊搜索。
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 搜索关键字（可为 null）
     * @return 分页结果 PageResult<UserVO>
     */
    public PageResult<UserVO> list(int page, int size, String keyword) {
        // 分页 size 钳制：防止恶意超大 size 拖垮 DB（1..100）
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        Page<User> p = new Page<>(page, size);
        Page<User> result = this.userMapper.selectPage(p, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toUserVO).toList());
    }

    /**
     * 统计正常状态（status=1）用户总数。
     *
     * @return 启用用户数
     */
    public long countUsers() {
        return this.userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
    }

    /**
     * 查询用户权限列表（按逗号拆分 permissions 字段）。
     *
     * @param id 目标用户 ID
     * @return 成功：code=200，data 为权限字符串列表；用户不存在：code=404
     */
    public ApiResponse<Object> permissions(Long id) {
        User user = this.userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        if (user.getPermissions() == null || user.getPermissions().isBlank()) {
            return ApiResponse.ok(java.util.List.of());
        }
        return ApiResponse.ok(java.util.Arrays.stream(user.getPermissions().split(",")).map(String::trim).filter(p -> !p.isBlank()).toList());
    }

    /**
     * 覆盖式设置用户权限列表（去空去重后逗号拼接存储；列表为空时清空权限）。
     *
     * @param id          目标用户 ID
     * @param permissions 新的权限列表
     * @return code=200 表示设置成功；用户不存在：code=404
     */
    public ApiResponse<Object> setPermissions(Long id, java.util.List<String> permissions) {
        User user = this.userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.fail(404, "用户不存在");
        }
        if (permissions == null || permissions.isEmpty()) {
            user.setPermissions(null);
        } else {
            user.setPermissions(String.join(",", permissions.stream().filter(p -> p != null && !p.isBlank()).distinct().toList()));
        }
        this.userMapper.updateById(user);
        return ApiResponse.ok();
    }

    /**
     * 提交实名认证申请：保存真实姓名+身份证号，状态置为审核中(1)。
     * 已认证(2)的用户不允许重复提交。
     */
    public ApiResponse<UserVO> submitVerification(Long userId, String realName, String idCard) {
        User user = this.userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        if (user.getVerified() != null && user.getVerified() == 2) {
            return ApiResponse.fail("您已通过实名认证，无需重复提交");
        }
        if (realName == null || realName.trim().length() < 2) {
            return ApiResponse.fail(400, "请输入真实姓名");
        }
        if (idCard == null || !idCard.trim().matches("^\\d{17}[\\dXx]$")) {
            return ApiResponse.fail(400, "请输入有效的 18 位身份证号");
        }
        user.setRealName(realName.trim());
        user.setIdCard(idCard.trim().toUpperCase());
        user.setVerified(1);
        this.userMapper.updateById(user);
        return ApiResponse.ok(this.toUserVO(user));
    }

    /**
     * 管理员审核实名认证：通过(verified=2) 或 拒绝(verified=3)。
     */
    public ApiResponse<Object> auditVerification(Long targetUserId, Integer verified) {
        User user = this.userMapper.selectById(targetUserId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        if (verified != 2 && verified != 3) return ApiResponse.fail(400, "无效的审核操作");
        user.setVerified(verified);
        this.userMapper.updateById(user);
        return ApiResponse.ok();
    }

    /**
     * 内部接口：返回全量用户的轻量目录（群聊成员列表用）。
     * 仅含 id/username/nickname/avatar，按创建时间升序。
     */
    public java.util.List<Map<String, Object>> listAllForChat() {
        return this.userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername, User::getNickname, User::getAvatar)
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getCreatedAt))
                .stream().map(u -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("nickname", u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname());
                    m.put("avatar", u.getAvatar());
                    return m;
                }).toList();
    }

    /**
     * 实体转视图对象：User -> UserVO（权限字符串拆分为列表，过滤空项）。
     *
     * @param user 用户实体
     * @return UserVO 视图对象
     */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setUserClass(user.getUserClass());
        vo.setVerified(user.getVerified() != null ? user.getVerified() : 0);
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        if (user.getPermissions() != null && !user.getPermissions().isBlank()) {
            vo.setPermissions(java.util.Arrays.stream(user.getPermissions().split(",")).map(String::trim).filter(p -> !p.isBlank()).toList());
        }
        if (user.getAvatar() != null) {
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}
