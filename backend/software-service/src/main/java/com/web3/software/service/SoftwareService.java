package com.web3.software.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.software.entity.Software;
import com.web3.software.mapper.SoftwareMapper;
import com.web3.software.vo.SoftwareVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 类名：SoftwareService
 * 所属模块：software-service（软件下载服务）
 * 职责：软件下载业务服务，继承 MyBatis-Plus ServiceImpl，提供软件的分页查询（关键词/分类筛选）、
 *       详情、创建与下载计数累加逻辑。
 * 关键注解：@Service 注册为 Spring 业务 Bean。
 */
@Service
public class SoftwareService extends ServiceImpl<SoftwareMapper, Software> {
    /**
     * 分页查询软件列表：关键词模糊匹配名称/描述，分类精确匹配，按创建时间倒序。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @param keyword  关键词（可空）
     * @param category 分类（可空）
     * @return 软件分页结果
     */
    public PageResult<SoftwareVO> page(int pageNum, int pageSize, String keyword, String category) {
        LambdaQueryWrapper<Software> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Software::getName, keyword).or().like(Software::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Software::getCategory, category);
        }
        wrapper.orderByDesc(Software::getCreatedAt);
        Page<Software> p = new Page<>(pageNum, pageSize);
        IPage<Software> result = this.page(p, wrapper);
        List<SoftwareVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    /**
     * 按 ID 查询软件详情并转换为 VO。
     *
     * @param id 软件 ID
     * @return 软件 VO；不存在时返回 null
     */
    public SoftwareVO findById(Long id) {
        Software entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        return this.toVO(entity);
    }

    /**
     * 新增软件条目：保存实体并返回对应的 VO。
     *
     * @param software 待保存的软件实体（主键由 MyBatis-Plus 自动分配）
     * @return 保存成功后的软件 VO
     */
    public SoftwareVO create(Software software) {
        this.save(software);
        return this.toVO(software);
    }

    /**
     * 更新软件条目：按 ID 更新非空字段并返回更新后的 VO。
     *
     * @param software 待更新的软件实体（id 必填）
     * @return 更新后的软件 VO
     */
    public SoftwareVO update(Software software) {
        this.updateById(software);
        Software updated = this.getById(software.getId());
        return this.toVO(updated);
    }

    /**
     * 删除软件条目（逻辑删除）。
     *
     * @param id 软件 ID
     */
    public void delete(Long id) {
        this.removeById(id);
    }

    /**
     * 软件下载计数 +1：软件存在时将 downloadCount 累加并更新。
     *
     * @param id 软件 ID
     */
    public void incrementDownload(Long id) {
        UpdateWrapper<Software> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id).setSql("download_count = download_count + 1");
        this.update(wrapper);
    }

    /**
     * 软件实体转换为展示层 VO（字段同名拷贝）。
     *
     * @param s 软件实体
     * @return 软件 VO
     */
    private SoftwareVO toVO(Software s) {
        SoftwareVO vo = new SoftwareVO();
        vo.setId(s.getId());
        vo.setName(s.getName());
        vo.setDescription(s.getDescription());
        vo.setDownloadUrl(s.getDownloadUrl());
        vo.setCategory(s.getCategory());
        vo.setVersion(s.getVersion());
        vo.setSize(s.getSize());
        vo.setOs(s.getOs());
        vo.setIcon(s.getIcon());
        vo.setDownloadCount(s.getDownloadCount());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }
}
