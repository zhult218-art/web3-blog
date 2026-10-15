package com.web3.tool.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.tool.entity.Script;
import com.web3.tool.mapper.ScriptMapper;
import com.web3.tool.vo.ScriptVO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 脚本业务服务(tool-service 模块)。
 *
 * <p>继承 MyBatis-Plus 的 {@link com.baomidou.mybatisplus.extension.service.impl.ServiceImpl},
 * 提供脚本的分页查询(支持关键词/分类筛选)、详情、创建与下载计数累加逻辑。
 * {@code @Service} 注解使其被 Spring 容器托管,供 ScriptController 注入使用。</p>
 */
@Service
public class ScriptService extends ServiceImpl<ScriptMapper, Script> {
    /**
     * 分页查询脚本列表:关键词模糊匹配名称/描述/标签,分类精确匹配,
     * 按创建时间倒序。
     *
     * @param pageNum  页码,从 1 开始
     * @param pageSize 每页条数
     * @param keyword  关键词(可空)
     * @param category 分类(可空)
     * @return 脚本分页结果
     */
    public PageResult<ScriptVO> page(int pageNum, int pageSize, String keyword, String category) {
        LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Script::getName, keyword).or().like(Script::getDescription, keyword).or().like(Script::getTags, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Script::getCategory, category);
        }
        wrapper.orderByDesc(Script::getCreatedAt);
        Page<Script> p = new Page<>(pageNum, pageSize);
        IPage<Script> result = this.page(p, wrapper);
        List<ScriptVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    /**
     * 按 ID 查询脚本详情并转换为 VO。
     *
     * @param id 脚本 ID
     * @return 脚本 VO;不存在时返回 null
     */
    public ScriptVO findById(Long id) {
        Script entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        return this.toVO(entity);
    }

    /**
     * 新增脚本:保存实体并返回对应的 VO。
     *
     * @param script 待保存的脚本实体(主键由 MyBatis-Plus 自动分配)
     * @return 保存成功后的脚本 VO
     */
    public ScriptVO create(Script script) {
        this.save(script);
        return this.toVO(script);
    }

    /**
     * 更新脚本：按 ID 更新非空字段并返回更新后的 VO。
     *
     * @param script 待更新的脚本实体（id 必填）
     * @return 更新后的脚本 VO
     */
    public ScriptVO update(Script script) {
        this.updateById(script);
        Script updated = this.getById(script.getId());
        return this.toVO(updated);
    }

    /**
     * 删除脚本（逻辑删除）。
     *
     * @param id 脚本 ID
     */
    public void delete(Long id) {
        this.removeById(id);
    }

    /**
     * 脚本下载计数 +1:脚本存在时将 downloadCount 累加并更新。
     *
     * @param id 脚本 ID
     */
    public void incrementDownload(Long id) {
        UpdateWrapper<Script> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id).setSql("download_count = download_count + 1");
        this.update(wrapper);
    }

    /**
     * 脚本实体转换为展示层 VO(字段同名拷贝)。
     *
     * @param s 脚本实体
     * @return 脚本 VO
     */
    private ScriptVO toVO(Script s) {
        ScriptVO vo = new ScriptVO();
        vo.setId(s.getId());
        vo.setName(s.getName());
        vo.setDescription(s.getDescription());
        vo.setContent(s.getContent());
        vo.setCategory(s.getCategory());
        vo.setLanguage(s.getLanguage());
        vo.setVersion(s.getVersion());
        vo.setDownloadCount(s.getDownloadCount());
        vo.setTags(s.getTags());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }
}
