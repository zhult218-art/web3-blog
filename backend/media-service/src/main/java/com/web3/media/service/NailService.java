package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.media.entity.NailBooking;
import com.web3.media.entity.NailProfile;
import com.web3.media.entity.NailWork;
import com.web3.media.mapper.NailBookingMapper;
import com.web3.media.mapper.NailProfileMapper;
import com.web3.media.mapper.NailWorkMapper;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * NailService —— 美甲小铺业务服务
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：实现美甲小铺的核心业务逻辑，包括店主资料查询/更新（单行记录 id=1，不存在时自动初始化）、
 * 作品查询/发布/点赞/删除、预约提交/列表/状态更新。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;NailWorkMapper, NailWork&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class NailService
extends ServiceImpl<NailWorkMapper, NailWork> {
    /** 允许的预约状态集合 */
    private static final Set<String> BOOKING_STATUSES = Set.of("PENDING", "CONFIRMED", "DONE", "CANCELLED");
    /** 店主资料表数据访问接口 */
    private final NailProfileMapper nailProfileMapper;
    /** 预约表数据访问接口 */
    private final NailBookingMapper nailBookingMapper;

    public NailService(NailProfileMapper nailProfileMapper, NailBookingMapper nailBookingMapper) {
        this.nailProfileMapper = nailProfileMapper;
        this.nailBookingMapper = nailBookingMapper;
    }

    /**
     * 查询店主资料（固定 id=1），不存在时插入默认行再返回
     *
     * @return 店主资料实体
     */
    public NailProfile getProfile() {
        NailProfile profile = this.nailProfileMapper.selectById(1L);
        if (profile == null) {
            NailProfile created = new NailProfile();
            created.setId(1L);
            this.nailProfileMapper.insert(created);
            profile = this.nailProfileMapper.selectById(1L);
        }
        return profile;
    }

    /**
     * 更新店主资料（整体更新 id=1）
     *
     * @param profile 店主资料实体（含需更新的字段）
     * @return 更新后的店主资料实体
     */
    public NailProfile updateProfile(NailProfile profile) {
        profile.setId(1L);
        this.nailProfileMapper.updateById(profile);
        return this.nailProfileMapper.selectById(1L);
    }

    /**
     * 查询全部美甲作品，按创建时间倒序
     *
     * @return 作品实体列表
     */
    public List<NailWork> works() {
        return this.list(new LambdaQueryWrapper<NailWork>().orderByDesc(NailWork::getCreatedAt));
    }

    /**
     * 发布美甲作品，owner 记录为当前登录用户名
     *
     * @param work  作品实体
     * @param owner 当前登录用户名
     * @return 保存后的作品实体
     */
    public NailWork createWork(NailWork work, String owner) {
        work.setOwner(owner);
        if (work.getLikes() == null) {
            work.setLikes(0);
        }
        this.save(work);
        return work;
    }

    /**
     * 作品点赞：likes 自增 1
     *
     * @param id 作品 ID
     * @return 自增后的最新点赞数
     */
    public Integer likeWork(Long id) {
        NailWork work = this.getById(id);
        if (work == null) {
            throw new RuntimeException("作品不存在");
        }
        LambdaUpdateWrapper<NailWork> wrapper = new LambdaUpdateWrapper<NailWork>().eq(NailWork::getId, id).setSql("likes = likes + 1");
        this.baseMapper.update(null, wrapper);
        return this.getById(id).getLikes();
    }

    /**
     * 删除美甲作品（owner 操作，逻辑删除）
     *
     * @param id 作品 ID
     */
    public void deleteWork(Long id) {
        this.removeById(id);
    }

    /**
     * 提交美甲预约（公开），状态固定为 PENDING
     *
     * @param booking 预约实体（name/phone 必填）
     * @return 保存后的预约实体
     * @throws IllegalArgumentException 姓名或电话为空
     */
    public NailBooking createBooking(NailBooking booking) {
        if (booking.getName() == null || booking.getName().isBlank() || booking.getPhone() == null || booking.getPhone().isBlank()) {
            throw new IllegalArgumentException("姓名和电话不能为空");
        }
        booking.setId(null);
        booking.setStatus("PENDING");
        this.nailBookingMapper.insert(booking);
        return booking;
    }

    /**
     * 查询全部预约列表，按创建时间倒序
     *
     * @return 预约实体列表
     */
    public List<NailBooking> bookings() {
        return this.nailBookingMapper.selectList(new LambdaQueryWrapper<NailBooking>().orderByDesc(NailBooking::getCreatedAt));
    }

    /**
     * 用户端：按手机号查询自己的预约（用于「我的预约」入口）
     * 只返回未取消的预约或最近 30 天内全部预约，避免历史数据过多
     *
     * @param phone 预约人手机号
     * @return 预约实体列表
     */
    public List<NailBooking> bookingsByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return this.nailBookingMapper.selectList(
            new LambdaQueryWrapper<NailBooking>()
                .eq(NailBooking::getPhone, phone.trim())
                .orderByDesc(NailBooking::getCreatedAt)
        );
    }

    /**
     * 用户端取消自己的预约：仅允许 PENDING/CONFIRMED 状态取消，
     * 且必须手机号匹配（避免他人恶意取消）
     *
     * @param id    预约 ID
     * @param phone 预约人手机号（鉴权用）
     * @throws IllegalArgumentException 预约不存在 / 手机号不匹配 / 状态不允许取消
     */
    public void cancelBookingByUser(Long id, String phone) {
        NailBooking booking = this.nailBookingMapper.selectById(id);
        if (booking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (phone == null || !phone.trim().equals(booking.getPhone())) {
            throw new IllegalArgumentException("手机号不匹配，无法取消此预约");
        }
        if (!"PENDING".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
            throw new IllegalArgumentException("当前状态无法取消（已完成或已取消）");
        }
        booking.setStatus("CANCELLED");
        this.nailBookingMapper.updateById(booking);
    }

    /**
     * 更新预约状态（仅允许 PENDING/CONFIRMED/DONE/CANCELLED）
     *
     * @param id     预约 ID
     * @param status 目标状态
     * @throws IllegalArgumentException 状态非法或预约不存在
     */
    public void updateBookingStatus(Long id, String status) {
        if (status == null || !BOOKING_STATUSES.contains(status)) {
            throw new IllegalArgumentException("非法的预约状态：" + status);
        }
        NailBooking booking = this.nailBookingMapper.selectById(id);
        if (booking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        booking.setStatus(status);
        this.nailBookingMapper.updateById(booking);
    }
}
