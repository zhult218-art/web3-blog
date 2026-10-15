package com.web3.media.controller;

import com.web3.common.core.ApiResponse;
import com.web3.common.core.UnauthorizedException;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import com.web3.media.entity.NailBooking;
import com.web3.media.entity.NailProfile;
import com.web3.media.entity.NailWork;
import com.web3.media.service.NailService;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * NailController —— 美甲小铺接口控制器
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：管理美甲小铺的对外 REST 接口，包括店主资料查询/更新、作品查询/发布/点赞/删除、
 * 预约提交/列表/状态更新。资料更新、作品管理、预约管理需"店主"权限（管理员或用户名 nailqueen）。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@RestController：声明为 REST 控制器，返回值自动序列化为 JSON</li>
 *   <li>@RequestMapping("/nail")：对外接口统一前缀为 /nail</li>
 * </ul>
 */
@RestController
@RequestMapping(value={"/nail"})
public class NailController {
    /** 店主用户名：该用户与管理员均可管理美甲数据（可通过配置 nail.owner-username 覆盖） */
    @Value("${nail.owner-username:nailqueen}")
    private String ownerUsername;
    /** 美甲小铺业务服务 */
    private final NailService nailService;
    /** JWT 令牌校验工具 */
    private final JwtUtil jwtUtil;
    /** JWT 配置属性 */
    private final JwtProperties jwtProperties;

    public NailController(NailService nailService, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.nailService = nailService;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 店主资料查询（前端路由：GET /nail/profile），无需登录
     *
     * @return 店主资料实体
     */
    @GetMapping(value={"/profile"})
    public ApiResponse<NailProfile> profile() {
        return ApiResponse.ok(this.nailService.getProfile());
    }

    /**
     * 更新店主资料（前端路由：PUT /nail/profile，需店主）
     *
     * @param profile       店主资料实体（JSON 请求体，整体更新 id=1）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 更新后的店主资料实体
     */
    @PutMapping(value={"/profile"})
    public ApiResponse<NailProfile> updateProfile(@RequestBody NailProfile profile, @RequestHeader(value="Authorization") String authorization) {
        this.requireOwner(authorization);
        return ApiResponse.ok(this.nailService.updateProfile(profile));
    }

    /**
     * 作品列表（前端路由：GET /nail/works），无需登录，按创建时间倒序全量返回
     *
     * @return 作品实体列表
     */
    @GetMapping(value={"/works"})
    public ApiResponse<List<NailWork>> works() {
        return ApiResponse.ok(this.nailService.works());
    }

    /**
     * 发布作品（前端路由：POST /nail/work，需店主）
     *
     * @param work          作品实体（JSON 请求体：title/tags/price/img）
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 保存后的作品实体（owner 为当前登录用户名）
     */
    @PostMapping(value={"/work"})
    public ApiResponse<NailWork> createWork(@RequestBody NailWork work, @RequestHeader(value="Authorization") String authorization) {
        String owner = this.requireOwner(authorization);
        return ApiResponse.ok(this.nailService.createWork(work, owner));
    }

    /**
     * 作品点赞（前端路由：PUT /nail/work/{id}/like）
     *
     * @param id 作品 ID
     * @return 自增后的最新点赞数
     */
    @PutMapping(value={"/work/{id}/like"})
    public ApiResponse<Integer> likeWork(@PathVariable Long id) {
        return ApiResponse.ok(this.nailService.likeWork(id));
    }

    /**
     * 删除作品（前端路由：DELETE /nail/work/{id}，需店主，逻辑删除）
     *
     * @param id            作品 ID
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @DeleteMapping(value={"/work/{id}"})
    public ApiResponse<Object> deleteWork(@PathVariable Long id, @RequestHeader(value="Authorization") String authorization) {
        this.requireOwner(authorization);
        this.nailService.deleteWork(id);
        return ApiResponse.ok();
    }

    /**
     * 提交预约（前端路由：POST /nail/booking），无需登录，状态固定为 PENDING
     *
     * @param booking 预约实体（JSON 请求体：name/phone/service/date/timeSlot/note）
     * @return 保存后的预约实体
     */
    @PostMapping(value={"/booking"})
    public ApiResponse<NailBooking> booking(@RequestBody NailBooking booking) {
        try {
            return ApiResponse.ok(this.nailService.createBooking(booking));
        }
        catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    /**
     * 预约列表（前端路由：GET /nail/booking/list，需店主），按创建时间倒序
     *
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 预约实体列表
     */
    @GetMapping(value={"/booking/list"})
    public ApiResponse<List<NailBooking>> bookingList(@RequestHeader(value="Authorization") String authorization) {
        this.requireOwner(authorization);
        return ApiResponse.ok(this.nailService.bookings());
    }

    /**
     * 更新预约状态（前端路由：PUT /nail/booking/{id}/status?status=CONFIRMED，需店主）
     * 仅允许 PENDING/CONFIRMED/DONE/CANCELLED
     *
     * @param id            预约 ID
     * @param status        目标状态
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 成功提示
     */
    @PutMapping(value={"/booking/{id}/status"})
    public ApiResponse<Object> updateBookingStatus(@PathVariable Long id, @RequestParam(value="status") String status, @RequestHeader(value="Authorization") String authorization) {
        this.requireOwner(authorization);
        try {
            this.nailService.updateBookingStatus(id, status);
        }
        catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
        return ApiResponse.ok();
    }

    /**
     * 用户端查询我的预约（前端路由：GET /nail/booking/my?phone=xxx，无需登录）
     * 用户凭提交预约时的手机号查询历史与当前预约，用于「我的预约」入口
     *
     * @param phone 预约人手机号
     * @return 预约实体列表
     */
    @GetMapping(value={"/booking/my"})
    public ApiResponse<List<NailBooking>> myBookings(@RequestParam(value="phone") String phone) {
        return ApiResponse.ok(this.nailService.bookingsByPhone(phone));
    }

    /**
     * 用户端取消自己的预约（前端路由：PUT /nail/booking/{id}/cancel?phone=xxx，无需登录）
     * 仅允许 PENDING/CONFIRMED 状态取消，且手机号必须匹配
     *
     * @param id    预约 ID
     * @param phone 预约人手机号（鉴权用）
     * @return 成功提示
     */
    @PutMapping(value={"/booking/{id}/cancel"})
    public ApiResponse<Object> cancelMyBooking(@PathVariable Long id, @RequestParam(value="phone") String phone) {
        try {
            this.nailService.cancelBookingByUser(id, phone);
        }
        catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
        return ApiResponse.ok();
    }

    /**
     * 店主鉴权（内部工具方法）：管理员或用户名为 nailqueen 时放行
     * 令牌无效/未登录时抛 401；已登录但非店主时抛 403
     *
     * @param authorization 请求头 Authorization 的 JWT 令牌
     * @return 当前登录用户名
     */
    private String requireOwner(String authorization) {
        try {
            return AuthUtils.requireAdmin(authorization, this.jwtUtil, this.jwtProperties);
        }
        catch (UnauthorizedException e) {
            if (e.getCode() != 403) {
                throw e;
            }
            String token = AuthUtils.resolveToken(authorization, this.jwtProperties);
            Claims claims = this.jwtUtil.parseClaims(token);
            String username = claims.getSubject();
            if (ownerUsername.equals(username)) {
                return username;
            }
            throw e;
        }
    }
}
