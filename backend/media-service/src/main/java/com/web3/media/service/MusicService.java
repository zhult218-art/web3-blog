package com.web3.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web3.common.core.PageResult;
import com.web3.media.entity.Music;
import com.web3.media.entity.UserMusicHistory;
import com.web3.media.entity.UserMusicLike;
import com.web3.media.mapper.MusicMapper;
import com.web3.media.mapper.UserMusicHistoryMapper;
import com.web3.media.mapper.UserMusicLikeMapper;
import com.web3.media.vo.MusicVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private UserMusicLikeMapper likeMapper;
    @Autowired
    private UserMusicHistoryMapper historyMapper;
    /**
     * 分页查询音乐列表，支持歌名/歌手关键字筛选，按创建时间倒序
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 歌名/歌手关键字，可为空
     * @return 音乐视图对象的分页结果
     */
    public PageResult<MusicVO> page(int page, int size, String keyword) {
        // 分页 size 钳制：防止恶意超大 size 拖垮 DB（1..100）
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
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

    // ============================================================
    // 新增：收藏 / 播放统计 / 历史 / 热榜 / 搜索
    // ============================================================

    /**
     * 切换收藏状态：未收藏则插入收藏关系，已收藏则移除（toggle）。
     *
     * @param userId  当前用户 ID
     * @param musicId 音乐 ID
     * @return 操作后的收藏状态：true=已收藏，false=已取消
     */
    public boolean toggleLike(Long userId, Long musicId) {
        if (this.baseMapper.selectById(musicId) == null) {
            throw new RuntimeException("音乐不存在");
        }
        UserMusicLike exist = this.likeMapper.selectOne(
            new LambdaQueryWrapper<UserMusicLike>()
                .eq(UserMusicLike::getUserId, userId)
                .eq(UserMusicLike::getMusicId, musicId));
        if (exist != null) {
            this.likeMapper.delete(
                new LambdaQueryWrapper<UserMusicLike>()
                    .eq(UserMusicLike::getUserId, userId)
                    .eq(UserMusicLike::getMusicId, musicId));
            return false;
        }
        UserMusicLike like = new UserMusicLike();
        like.setUserId(userId);
        like.setMusicId(musicId);
        like.setCreatedAt(LocalDateTime.now());
        try {
            this.likeMapper.insert(like);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发双击：主键(user_id,music_id)保证仅一条记录，后到请求视为已收藏
            return true;
        }
        return true;
    }

    /**
     * 查询当前用户对某首歌的收藏态
     */
    public boolean isLiked(Long userId, Long musicId) {
        if (userId == null) return false;
        return this.likeMapper.selectCount(
            new LambdaQueryWrapper<UserMusicLike>()
                .eq(UserMusicLike::getUserId, userId)
                .eq(UserMusicLike::getMusicId, musicId)) > 0;
    }

    /**
     * 当前用户收藏列表（分页，按收藏时间倒序）
     */
    public PageResult<MusicVO> pageLiked(Long userId, int page, int size) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<UserMusicLike> mpPage = new Page<>(page, size);
        mpPage.setSearchCount(true);
        Page<UserMusicLike> result = this.likeMapper.selectPage(mpPage,
            new LambdaQueryWrapper<UserMusicLike>()
                .eq(UserMusicLike::getUserId, userId)
                .orderByDesc(UserMusicLike::getCreatedAt));
        List<Long> musicIds = result.getRecords().stream()
            .map(UserMusicLike::getMusicId).collect(Collectors.toList());
        List<MusicVO> items = this.batchFetchVOs(musicIds, userId);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    /**
     * 播放次数 +1（原子自增，避免并发覆盖）
     */
    public void incrementPlay(Long id) {
        if (this.baseMapper.selectById(id) == null) return;
        this.baseMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Music>()
                .eq(Music::getId, id)
                .setSql("play_count = play_count + 1"));
    }

    /**
     * 热榜：按 play_count 倒序，限制 limit（默认 20，钳制 1..100）
     */
    public List<MusicVO> hot(int limit) {
        limit = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<Music>()
            .orderByDesc(Music::getPlayCount)
            .last("LIMIT " + limit);
        List<Music> list = this.baseMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 本地搜索：歌名 / 歌手 LIKE 关键字（聚合搜索的本地部分，网易云/iTunes 由前端独立调用并合并）
     */
    public PageResult<MusicVO> search(String keyword, int page, int size) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Music> wrapper = new LambdaQueryWrapper<Music>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Music::getTitle, keyword).or().like(Music::getArtist, keyword);
        }
        wrapper.orderByDesc(Music::getPlayCount);
        Page<Music> mpPage = new Page<>((long) page, (long) size);
        Page<Music> result = this.baseMapper.selectPage(mpPage, wrapper);
        List<MusicVO> items = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    /**
     * 记录播放历史（单用户单首歌保留最新一条：先删后插，避免重复堆积）
     */
    @Transactional
    public void addHistory(Long userId, Long musicId) {
        if (this.baseMapper.selectById(musicId) == null) return;
        this.historyMapper.delete(
            new LambdaQueryWrapper<UserMusicHistory>()
                .eq(UserMusicHistory::getUserId, userId)
                .eq(UserMusicHistory::getMusicId, musicId));
        UserMusicHistory h = new UserMusicHistory();
        h.setUserId(userId);
        h.setMusicId(musicId);
        h.setPlayedAt(LocalDateTime.now());
        this.historyMapper.insert(h);
    }

    /**
     * 用户播放历史（按时间倒序分页，JOIN music 表批量回填，避免 N+1）
     */
    public PageResult<MusicVO> pageHistory(Long userId, int page, int size) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<UserMusicHistory> mpPage = new Page<>(page, size);
        mpPage.setSearchCount(true);
        Page<UserMusicHistory> result = this.historyMapper.selectPage(mpPage,
            new LambdaQueryWrapper<UserMusicHistory>()
                .eq(UserMusicHistory::getUserId, userId)
                .orderByDesc(UserMusicHistory::getPlayedAt));
        List<Long> musicIds = result.getRecords().stream()
            .map(UserMusicHistory::getMusicId).collect(Collectors.toList());
        List<MusicVO> items = this.batchFetchVOs(musicIds, userId);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    /**
     * 清空当前用户播放历史
     */
    public void clearHistory(Long userId) {
        this.historyMapper.delete(
            new LambdaQueryWrapper<UserMusicHistory>()
                .eq(UserMusicHistory::getUserId, userId));
    }

    /**
     * 取详情并附带当前用户收藏态（userId 可空）
     */
    public MusicVO getDetailForUser(Long id, Long userId) {
        Music music = this.baseMapper.selectById(id);
        if (music == null) {
            throw new RuntimeException("音乐不存在");
        }
        MusicVO vo = this.toVO(music);
        vo.setLiked(this.isLiked(userId, id));
        return vo;
    }

    /**
     * 按 ID 批量拉取 MusicVO，保持入参顺序，附带 liked 状态
     */
    private List<MusicVO> batchFetchVOs(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Music> musics = this.baseMapper.selectBatchIds(ids);
        Map<Long, Music> map = new HashMap<>();
        for (Music m : musics) map.put(m.getId(), m);
        // 批量查 liked 状态：单次查询该用户所有 liked 记录，然后 set 比对
        Set<Long> likedIds = userId == null ? Collections.emptySet() :
            this.likeMapper.selectList(
                new LambdaQueryWrapper<UserMusicLike>()
                    .eq(UserMusicLike::getUserId, userId)
                    .in(UserMusicLike::getMusicId, ids))
                .stream().map(UserMusicLike::getMusicId).collect(Collectors.toSet());
        List<MusicVO> result = new ArrayList<>();
        for (Long id : ids) {
            Music m = map.get(id);
            if (m == null) continue;
            MusicVO vo = this.toVO(m);
            vo.setLiked(likedIds.contains(id));
            result.add(vo);
        }
        return result;
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
