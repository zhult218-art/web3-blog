package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.media.entity.Music;
import com.web3.media.mapper.MusicMapper;
import com.web3.media.vo.MusicVO;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * MusicService —— 音乐业务服务
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：实现音乐资源的业务逻辑，包括分页查询（歌名/歌手关键字）、详情查询、
 * 创建/更新/删除（管理员操作），以及实体到视图对象（MusicVO）的转换。
 * <p>
 * 关键注解作用：
 * <ul>
 *   <li>@Service：声明为 Spring 业务组件</li>
 *   <li>继承 ServiceImpl&lt;MusicMapper, Music&gt;：获得 MyBatis-Plus 内置 CRUD 能力</li>
 * </ul>
 */
@Service
public class MusicService
extends ServiceImpl<MusicMapper, Music> {
    /**
     * 分页查询音乐列表，支持歌名/歌手关键字筛选，按创建时间倒序
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 歌名/歌手关键字，可为空
     * @return 音乐视图对象的分页结果
     */
    public PageResult<MusicVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<Music>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Music::getTitle, (Object)keyword).or().like(Music::getArtist, (Object)keyword);
        }
        wrapper.orderByDesc(Music::getCreatedAt);
        Page<Music> mpPage = new Page<Music>((long)page, (long)size);
        Page<Music> result = this.baseMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    /**
     * 查询音乐详情，不存在时抛异常
     *
     * @param id 音乐 ID
     * @return 音乐视图对象
     */
    public MusicVO getDetail(Long id) {
        Music music = (Music)this.baseMapper.selectById(id);
        if (music == null) {
            throw new RuntimeException("\u97f3\u4e50\u4e0d\u5b58\u5728");
        }
        return this.toVO(music);
    }

    /**
     * 创建音乐（管理员操作）
     *
     * @param music 音乐实体
     * @return 创建成功后的音乐视图对象
     */
    public MusicVO create(Music music) {
        this.baseMapper.insert(music);
        return this.toVO(music);
    }

    /**
     * 更新音乐（管理员操作），不存在时抛异常
     *
     * @param id    音乐 ID
     * @param music 音乐实体（含需更新的字段）
     * @return 更新后的音乐视图对象
     */
    public MusicVO update(Long id, Music music) {
        Music exist = (Music)this.baseMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("\u97f3\u4e50\u4e0d\u5b58\u5728");
        }
        music.setId(id);
        this.baseMapper.updateById(music);
        return this.toVO((Music)this.baseMapper.selectById(id));
    }

    /**
     * 删除音乐（管理员操作）
     *
     * @param id 音乐 ID
     */
    public void delete(Long id) {
        this.baseMapper.deleteById(id);
    }

    /**
     * 实体转视图对象（内部工具方法）
     *
     * @param music 音乐实体
     * @return 音乐视图对象
     */
    private MusicVO toVO(Music music) {
        MusicVO vo = new MusicVO();
        BeanUtils.copyProperties((Object)music, (Object)vo);
        return vo;
    }
}
