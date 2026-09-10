package com.web3.aiproxy.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3.aiproxy.entity.ProxyUserGroup;
import com.web3.aiproxy.mapper.ProxyUserGroupMapper;
import com.web3.aiproxy.service.UserGroupService;
import com.web3.common.core.ApiResponse;
import com.web3.common.security.AuthUtils;
import com.web3.common.security.JwtProperties;
import com.web3.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 类名：AdminGroupController
 * 所属模块：ai-proxy-service（中转站）
 * 职责：用户分组管理接口（需管理员 JWT，P2-T14）—— 分组倍率 CRUD；
 *       default 分组不可删除；变更后清空本地倍率缓存即时生效。
 */
@RestController
@RequestMapping("/admin/ai/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final ProxyUserGroupMapper groupMapper;
    private final UserGroupService userGroupService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @GetMapping
    public ApiResponse<List<ProxyUserGroup>> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        return ApiResponse.ok(groupMapper.selectList(
                Wrappers.<ProxyUserGroup>lambdaQuery().orderByAsc(ProxyUserGroup::getId)));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProxyUserGroup body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        if (!StringUtils.hasText(body.getGroupName())) return ApiResponse.fail(400, "groupName is required");
        if (groupMapper.selectCount(Wrappers.<ProxyUserGroup>lambdaQuery()
                .eq(ProxyUserGroup::getGroupName, body.getGroupName())) > 0) {
            return ApiResponse.fail(409, "group already exists");
        }
        if (body.getRate() == null || body.getRate().signum() <= 0) body.setRate(BigDecimal.ONE);
        if (body.getStatus() == null) body.setStatus(1);
        groupMapper.insert(body);
        userGroupService.evictLocal();
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyUserGroup exist = groupMapper.selectById(id);
        if (exist == null) return ApiResponse.fail(404, "group not found");
        if (body.containsKey("rate")) {
            try {
                BigDecimal rate = new BigDecimal(String.valueOf(body.get("rate")));
                if (rate.signum() > 0) exist.setRate(rate);
            } catch (NumberFormatException ignored) {
            }
        }
        if (body.containsKey("description")) exist.setDescription((String) body.get("description"));
        if (body.containsKey("status")) {
            exist.setStatus(Integer.parseInt(String.valueOf(body.get("status"))));
        }
        groupMapper.updateById(exist);
        userGroupService.evictLocal();
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        AuthUtils.requireAdmin(auth, jwtUtil, jwtProperties);
        ProxyUserGroup exist = groupMapper.selectById(id);
        if (exist == null) return ApiResponse.fail(404, "group not found");
        if ("default".equals(exist.getGroupName())) return ApiResponse.fail(400, "default group cannot be deleted");
        groupMapper.deleteById(id);
        userGroupService.evictLocal();
        return ApiResponse.ok();
    }
}
