package com.web3.user.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.user.config.AuthProperties;
import com.web3.user.dto.LoginDTO;
import com.web3.user.dto.UserRegisterDTO;
import com.web3.user.entity.User;
import com.web3.user.mapper.UserMapper;
import com.web3.user.vo.JwtVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试（2026-10-20 补全）。
 *
 * <p>user-service 是认证核心（24 个类、此前 0 个测试），本测试优先锁住
 * 两条曾经出过高危问题的链路：</p>
 *
 * <ol>
 *   <li><b>注册即提权</b>：历史上只要注册用户名 {@code admin} 就会拿到 ADMIN 角色，
 *       而注册接口是公开的，等于任何人可拿管理员。修复后角色只由
 *       {@code ADMIN_USERNAMES} 环境变量白名单决定，未配置时永远为 USER。
 *       本测试在未设置该环境变量的前提下注册 "admin"，断言角色必须是 USER。</li>
 *   <li><b>登录静默成功</b>：后端业务失败曾返回 HTTP 200 + code=500，前端误判为登录成功。
 *       本测试断言失败路径必须返回 code=500 且 data 为空。</li>
 * </ol>
 *
 * <p>另覆盖账号形式自动识别（用户名 / 邮箱 / 手机号）与图形验证码校验。</p>
 */
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private OtpService otpService;
    @Mock
    private GoogleOAuthService googleOAuthService;
    @Mock
    private AuthProperties authProperties;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userMapper, redisTemplate, jwtUtil, jwtProperties,
                passwordEncoder, captchaService, otpService, googleOAuthService, authProperties);

        // redisTemplate.opsForValue() 默认返回 null，注册/登录成功路径会 NPE，必须打桩
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtProperties.getRefresh()).thenReturn(43200L);
        when(jwtProperties.getPrefix()).thenReturn("Bearer ");
        when(jwtUtil.generateToken(anyString(), any(), anyString(), any(), any(), any()))
                .thenReturn("mocked.jwt.token");
        when(jwtUtil.generateToken(anyString(), any(), anyString(), any(), any()))
                .thenReturn("mocked.jwt.token");
    }

    private UserRegisterDTO registerDto(String username) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("plain-password");
        dto.setNickname(username);
        dto.setEmail(username + "@example.com");
        return dto;
    }

    private User existingUser(Long id, String username, String rawPassword, String encoded) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setPassword(encoded);
        u.setRole("USER");
        u.setStatus(1);
        return u;
    }

    // ───────────── 注册 ─────────────

    @Test
    void register_duplicateUsername_shouldFail() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser(1L, "alice", "x", "enc"));

        ApiResponse<JwtVO> res = userService.register(registerDto("alice"));

        assertEquals(500, res.getCode());
        assertNull(res.getData());
        verify(userMapper, never()).insert(any(User.class));
    }

    /**
     * 核心回归：注册名为 "admin" 不得获得 ADMIN 角色。
     *
     * <p>测试进程未设置 ADMIN_USERNAMES，isAdminUsername 必然返回 false，
     * 因此入库的 role 必须是 USER。若有人把默认白名单改回 "admin"，本用例立刻失败。</p>
     */
    @Test
    void register_usernameAdmin_shouldNotBeGrantedAdminRole() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("plain-password")).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        ApiResponse<JwtVO> res = userService.register(registerDto("admin"));

        assertEquals(200, res.getCode());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("USER", captor.getValue().getRole(),
                "注册名 admin 不得自动获得 ADMIN 角色（ADMIN_USERNAMES 未配置时应为 USER）");
    }

    @Test
    void register_shouldStoreBcryptHashNotPlainText() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("plain-password")).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register(registerDto("bob"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("$2a$10$encoded", captor.getValue().getPassword());
        assertNotEquals("plain-password", captor.getValue().getPassword());
    }

    @Test
    void register_success_shouldIssueTokenAndCache() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        ApiResponse<JwtVO> res = userService.register(registerDto("carol"));

        assertEquals(200, res.getCode());
        assertNotNull(res.getData());
        assertEquals("mocked.jwt.token", res.getData().getToken());
        // 会话必须写入 Redis，否则刷新页面即掉登录
        verify(valueOperations).set(anyString(), eq("mocked.jwt.token"), anyLong(), any());
    }

    // ───────────── 登录 ─────────────

    @Test
    void login_wrongCaptcha_shouldFailBeforeQueryingUser() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("x");
        dto.setCaptchaId("cid");
        dto.setCaptchaCode("0000");
        when(captchaService.verify("cid", "0000")).thenReturn(false);

        ApiResponse<JwtVO> res = userService.login(dto);

        assertEquals(500, res.getCode());
        assertEquals("验证码错误", res.getMessage());
        verify(userMapper, never()).selectOne(any(Wrapper.class));
    }

    @Test
    void login_userNotExist_shouldFailWithGenericMessage() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("nobody");
        dto.setPassword("x");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        ApiResponse<JwtVO> res = userService.login(dto);

        assertEquals(500, res.getCode());
        // 不得泄露"用户不存在"，统一提示避免账号枚举
        assertEquals("用户名或密码错误", res.getMessage());
        assertNull(res.getData());
    }

    @Test
    void login_wrongPassword_shouldFailWithSameGenericMessage() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("bad");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser(1L, "alice", "bad", "$2a$10$enc"));
        when(passwordEncoder.matches("bad", "$2a$10$enc")).thenReturn(false);

        ApiResponse<JwtVO> res = userService.login(dto);

        assertEquals(500, res.getCode());
        assertEquals("用户名或密码错误", res.getMessage());
        assertNull(res.getData());
    }

    @Test
    void login_success_shouldReturnToken() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("right");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser(1L, "alice", "right", "$2a$10$enc"));
        when(passwordEncoder.matches("right", "$2a$10$enc")).thenReturn(true);

        ApiResponse<JwtVO> res = userService.login(dto);

        assertEquals(200, res.getCode());
        assertNotNull(res.getData());
        assertEquals("mocked.jwt.token", res.getData().getToken());
        assertEquals("alice", res.getData().getUser().getUsername());
    }

    /** 账号含 @ 时按 email 形式处理，仍能正常登录（分支覆盖） */
    @Test
    void login_byEmail_shouldSucceed() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice@example.com");
        dto.setPassword("right");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser(1L, "alice", "right", "$2a$10$enc"));
        when(passwordEncoder.matches("right", "$2a$10$enc")).thenReturn(true);

        ApiResponse<JwtVO> res = userService.login(dto);

        assertEquals(200, res.getCode());
        verify(userMapper).selectOne(any(Wrapper.class));
    }

    /** 纯数字账号按手机号形式处理，仍能正常登录（分支覆盖） */
    @Test
    void login_byPhone_shouldSucceed() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("13800138000");
        dto.setPassword("right");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser(1L, "alice", "right", "$2a$10$enc"));
        when(passwordEncoder.matches("right", "$2a$10$enc")).thenReturn(true);

        ApiResponse<JwtVO> res = userService.login(dto);

        assertEquals(200, res.getCode());
        verify(userMapper).selectOne(any(Wrapper.class));
    }
}
