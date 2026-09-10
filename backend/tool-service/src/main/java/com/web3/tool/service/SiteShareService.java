package com.web3.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.tool.entity.SiteShare;
import com.web3.tool.mapper.SiteShareMapper;
import com.web3.tool.vo.SiteShareVO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * SiteShareService —— 分享网站业务服务
 * <p>
 * 所属模块：tool-service（工具模块）。
 * <p>
 * 继承 MyBatis-Plus 的 ServiceImpl，提供分享网站的列表查询（支持分类/关键词筛选，
 * 可选仅显示状态）、分类统计、新增、更新与逻辑删除能力。
 */
@Service
public class SiteShareService extends ServiceImpl<SiteShareMapper, SiteShare> {

    /**
     * 查询分享网站列表：支持分类精确匹配与名称/描述/链接关键词模糊匹配，
     * 按排序值升序、序号升序返回。
     *
     * @param category 分类（可空）
     * @param keyword  关键词（可空）
     * @param onlyVisible 仅返回状态为显示(status=1)的站点；false 返回全部
     * @return 站点视图对象列表
     */
    public List<SiteShareVO> listSites(String category, String keyword, boolean onlyVisible) {
        LambdaQueryWrapper<SiteShare> wrapper = new LambdaQueryWrapper<SiteShare>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(SiteShare::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SiteShare::getName, keyword)
                    .or().like(SiteShare::getDescription, keyword)
                    .or().like(SiteShare::getUrl, keyword));
        }
        if (onlyVisible) {
            wrapper.eq(SiteShare::getStatus, 1);
        }
        wrapper.orderByAsc(SiteShare::getSort).orderByAsc(SiteShare::getId);
        return this.list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 查询全部分类（去重、非空、按排序值升序）。
     *
     * @return 分类名称列表
     */
    public List<String> listCategories() {
        return this.list(new LambdaQueryWrapper<SiteShare>()
                        .isNotNull(SiteShare::getCategory)
                        .ne(SiteShare::getCategory, "")
                        .orderByAsc(SiteShare::getSort))
                .stream().map(SiteShare::getCategory).distinct().collect(Collectors.toList());
    }

    /**
     * 新增分享网站：缺省排序 0、状态显示；返回新增后的视图对象。
     *
     * @param site 站点实体
     * @return 站点视图对象
     */
    public SiteShareVO create(SiteShare site) {
        if (site.getSort() == null) {
            site.setSort(0);
        }
        if (site.getStatus() == null) {
            site.setStatus(1);
        }
        this.save(site);
        return this.toVO(site);
    }

    /**
     * 更新分享网站：不存在时抛异常；返回更新后的视图对象。
     *
     * @param id   站点 ID
     * @param site 站点实体（含需更新的字段）
     * @return 站点视图对象
     */
    public SiteShareVO update(Long id, SiteShare site) {
        SiteShare exist = this.getById(id);
        if (exist == null) {
            throw new RuntimeException("分享网站不存在");
        }
        site.setId(id);
        this.updateById(site);
        return this.toVO(this.getById(id));
    }

    /**
     * 删除分享网站（逻辑删除）：不存在时抛异常。
     *
     * @param id 站点 ID
     */
    public void delete(Long id) {
        SiteShare exist = this.getById(id);
        if (exist == null) {
            throw new RuntimeException("分享网站不存在");
        }
        this.removeById(id);
    }

    /**
     * 站点实体转视图对象（内部工具方法）。
     */
    private SiteShareVO toVO(SiteShare site) {
        SiteShareVO vo = new SiteShareVO();
        BeanUtils.copyProperties(site, vo);
        return vo;
    }
}