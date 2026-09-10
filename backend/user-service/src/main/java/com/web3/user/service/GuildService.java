package com.web3.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.common.core.ApiResponse;
import com.web3.common.core.BusinessException;
import com.web3.user.entity.Guild;
import com.web3.user.entity.GuildMember;
import com.web3.user.entity.User;
import com.web3.user.mapper.GuildMapper;
import com.web3.user.mapper.GuildMemberMapper;
import com.web3.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GuildService {
    private final GuildMapper guildMapper;
    private final GuildMemberMapper guildMemberMapper;
    private final UserMapper userMapper;

    public GuildService(GuildMapper guildMapper, GuildMemberMapper guildMemberMapper, UserMapper userMapper) {
        this.guildMapper = guildMapper;
        this.guildMemberMapper = guildMemberMapper;
        this.userMapper = userMapper;
    }

    /** 获取所有公会列表（简要信息） */
    public ApiResponse<List<Map<String, Object>>> listGuilds() {
        List<Guild> guilds = guildMapper.selectList(
            new LambdaQueryWrapper<Guild>().eq(Guild::getStatus, 1).orderByDesc(Guild::getCreatedAt)
        );
        List<Map<String, Object>> result = guilds.stream().map(g -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("description", g.getDescription());
            m.put("emblem", g.getEmblem());
            m.put("maxMembers", g.getMaxMembers());
            m.put("memberCount", guildMemberMapper.selectCount(
                new LambdaQueryWrapper<GuildMember>()
                    .eq(GuildMember::getGuildId, g.getId())
                    .eq(GuildMember::getStatus, "APPROVED")
            ));
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    /** 获取单个公会详情 */
    public ApiResponse<Map<String, Object>> getGuild(Long guildId) {
        Guild g = guildMapper.selectById(guildId);
        if (g == null) return ApiResponse.fail("公会不存在");
        Map<String, Object> m = new HashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName());
        m.put("description", g.getDescription());
        m.put("emblem", g.getEmblem());
        m.put("masterUserId", g.getMasterUserId());
        m.put("maxMembers", g.getMaxMembers());
        m.put("memberCount", guildMemberMapper.selectCount(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getGuildId, g.getId())
                .eq(GuildMember::getStatus, "APPROVED")
        ));
        // 查会长昵称
        User master = userMapper.selectById(g.getMasterUserId());
        if (master != null) m.put("masterNickname", master.getNickname());
        return ApiResponse.ok(m);
    }

    /** 申请加入公会（需要已选职业） */
    @Transactional
    public ApiResponse<Void> applyGuild(Long userId, Long guildId, String message) {
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        if (user.getUserClass() == null || user.getUserClass().isBlank()) {
            return ApiResponse.fail(400, "请先选择职业");
        }
        Guild guild = guildMapper.selectById(guildId);
        if (guild == null || guild.getStatus() != 1) return ApiResponse.fail("公会不存在或已关闭");
        // 检查是否已有申请
        GuildMember exist = guildMemberMapper.selectOne(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getUserId, userId)
        );
        if (exist != null) {
            if ("APPROVED".equals(exist.getStatus())) return ApiResponse.fail(400, "你已经是该公会成员");
            if ("PENDING".equals(exist.getStatus())) return ApiResponse.fail(400, "你已有待审核的申请");
            // REJECTED 状态允许重新申请
            exist.setStatus("PENDING");
            exist.setMessage(message);
            exist.setCardNumber(null);
            exist.setApprovedBy(null);
            exist.setApprovedAt(null);
            guildMemberMapper.updateById(exist);
        } else {
            // 检查人数上限
            long count = guildMemberMapper.selectCount(
                new LambdaQueryWrapper<GuildMember>()
                    .eq(GuildMember::getGuildId, guildId)
                    .eq(GuildMember::getStatus, "APPROVED")
            );
            if (count >= guild.getMaxMembers()) return ApiResponse.fail(400, "公会人数已满");
            GuildMember member = new GuildMember();
            member.setGuildId(guildId);
            member.setUserId(userId);
            member.setStatus("PENDING");
            member.setMessage(message);
            guildMemberMapper.insert(member);
        }
        return ApiResponse.ok();
    }

    /** 获取待审核申请列表（仅会长可操作） */
    public ApiResponse<List<Map<String, Object>>> getPendingMembers(Long userId, Long guildId) {
        Guild guild = guildMapper.selectById(guildId);
        if (guild == null) return ApiResponse.fail("公会不存在");
        if (!guild.getMasterUserId().equals(userId)) return ApiResponse.fail(403, "只有会长可以操作");
        List<GuildMember> members = guildMemberMapper.selectList(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getStatus, "PENDING")
                .orderByAsc(GuildMember::getCreatedAt)
        );
        List<Map<String, Object>> result = members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("userId", m.getUserId());
            map.put("message", m.getMessage());
            map.put("createdAt", m.getCreatedAt());
            // 附带申请者信息
            User applicant = userMapper.selectById(m.getUserId());
            if (applicant != null) {
                map.put("nickname", applicant.getNickname());
                map.put("username", applicant.getUsername());
                map.put("avatar", applicant.getAvatar());
                map.put("userClass", applicant.getUserClass());
            }
            return map;
        }).collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    /** 获取公会已通过成员列表 */
    public ApiResponse<List<Map<String, Object>>> getApprovedMembers(Long guildId) {
        List<GuildMember> members = guildMemberMapper.selectList(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getStatus, "APPROVED")
                .orderByAsc(GuildMember::getApprovedAt)
        );
        List<Map<String, Object>> result = members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("cardNumber", m.getCardNumber());
            map.put("approvedAt", m.getApprovedAt());
            User member = userMapper.selectById(m.getUserId());
            if (member != null) {
                map.put("userId", member.getId());
                map.put("nickname", member.getNickname());
                map.put("avatar", member.getAvatar());
                map.put("userClass", member.getUserClass());
            }
            return map;
        }).collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    /** 审批申请：通过或拒绝（仅会长可操作） */
    @Transactional
    public ApiResponse<Void> approveMember(Long userId, Long memberId, boolean approve) {
        GuildMember member = guildMemberMapper.selectById(memberId);
        if (member == null) return ApiResponse.fail("申请不存在");
        Guild guild = guildMapper.selectById(member.getGuildId());
        if (guild == null) return ApiResponse.fail("公会不存在");
        if (!guild.getMasterUserId().equals(userId)) return ApiResponse.fail(403, "只有会长可以审批");
        if (!"PENDING".equals(member.getStatus())) return ApiResponse.fail("该申请已处理");
        if (approve) {
            // 生成冒险者卡号：公会缩写 + 4位年月 + 4位随机
            String cardNo = generateCardNumber(guild.getName());
            member.setStatus("APPROVED");
            member.setCardNumber(cardNo);
            member.setApprovedBy(userId);
            member.setApprovedAt(LocalDateTime.now());
        } else {
            member.setStatus("REJECTED");
            member.setApprovedBy(userId);
            member.setApprovedAt(LocalDateTime.now());
        }
        guildMemberMapper.updateById(member);
        return ApiResponse.ok();
    }

    /** 获取当前用户在公会中的状态 */
    public ApiResponse<Map<String, Object>> getMyGuildStatus(Long userId) {
        // 查找用户所在的所有公会关系
        List<GuildMember> memberships = guildMemberMapper.selectList(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getUserId, userId)
                .orderByDesc(GuildMember::getCreatedAt)
        );
        if (memberships.isEmpty()) {
            return ApiResponse.ok(null);
        }
        // 返回最新的一条（一个用户只在一个公会）
        GuildMember latest = memberships.get(0);
        Guild guild = guildMapper.selectById(latest.getGuildId());
        Map<String, Object> result = new HashMap<>();
        result.put("guildId", latest.getGuildId());
        result.put("status", latest.getStatus());
        result.put("cardNumber", latest.getCardNumber());
        result.put("approvedAt", latest.getApprovedAt());
        if (guild != null) {
            result.put("guildName", guild.getName());
            result.put("guildEmblem", guild.getEmblem());
            result.put("guildDescription", guild.getDescription());
        }
        return ApiResponse.ok(result);
    }

    /** 创建新公会（任何人都可以创建，但需管理员审核后才生效） */
    @Transactional
    public ApiResponse<Map<String, Object>> createGuild(Long userId, String name, String description) {
        User user = userMapper.selectById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        // 检查是否已创建公会
        Guild existing = guildMapper.selectOne(
            new LambdaQueryWrapper<Guild>().eq(Guild::getMasterUserId, userId)
        );
        if (existing != null) return ApiResponse.fail(400, "你已经是公会会长，不能重复创建");
        // 检查是否已加入公会
        GuildMember membership = guildMemberMapper.selectOne(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getUserId, userId)
                .eq(GuildMember::getStatus, "APPROVED")
        );
        if (membership != null) return ApiResponse.fail(400, "你已经是公会成员，请先退出再创建");

        Guild guild = new Guild();
        guild.setName(name);
        guild.setDescription(description);
        guild.setMasterUserId(userId);
        guild.setMaxMembers(50);
        guild.setStatus(1); // 默认直接生效，后续可加审核流程
        guildMapper.insert(guild);

        // 创建者自动成为公会成员
        GuildMember selfMember = new GuildMember();
        selfMember.setGuildId(guild.getId());
        selfMember.setUserId(userId);
        selfMember.setStatus("APPROVED");
        selfMember.setCardNumber(generateCardNumber(name));
        selfMember.setApprovedBy(userId);
        selfMember.setApprovedAt(LocalDateTime.now());
        guildMemberMapper.insert(selfMember);

        Map<String, Object> result = new HashMap<>();
        result.put("id", guild.getId());
        result.put("name", guild.getName());
        return ApiResponse.ok(result);
    }

    /** 退出公会（普通成员可退出，会长不能直接退出需转让） */
    @Transactional
    public ApiResponse<Void> leaveGuild(Long userId) {
        GuildMember membership = guildMemberMapper.selectOne(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getUserId, userId)
                .eq(GuildMember::getStatus, "APPROVED")
        );
        if (membership == null) return ApiResponse.fail(400, "你不在任何公会中");
        Guild guild = guildMapper.selectById(membership.getGuildId());
        if (guild != null && guild.getMasterUserId().equals(userId)) {
            return ApiResponse.fail(400, "会长不能退出公会，请先转让会长职位");
        }
        membership.setStatus("LEFT");
        guildMemberMapper.updateById(membership);
        return ApiResponse.ok();
    }

    /** 获取用户当前所在公会（用于 Controller 返回） */
    public GuildMember getUserGuildMember(Long userId) {
        return guildMemberMapper.selectOne(
            new LambdaQueryWrapper<GuildMember>()
                .eq(GuildMember::getUserId, userId)
                .eq(GuildMember::getStatus, "APPROVED")
        );
    }

    /** 获取公会（用于 Controller 返回） */
    public Guild getGuildById(Long guildId) {
        return guildMapper.selectById(guildId);
    }

    private String generateCardNumber(String guildName) {
        String prefix = guildName.length() >= 2 ? guildName.substring(0, 2) : guildName;
        String time = String.format("%04d", System.currentTimeMillis() % 10000);
        String rand = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + time + "-" + rand;
    }
}
