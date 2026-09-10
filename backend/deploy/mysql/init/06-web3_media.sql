USE web3_media;

CREATE TABLE IF NOT EXISTS `music` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `artist` VARCHAR(128) DEFAULT NULL,
  `url` VARCHAR(512) NOT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `duration` INT DEFAULT 0,
  `album` VARCHAR(128) DEFAULT NULL,
  `tags` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `video` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `url` VARCHAR(512) NOT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `duration` INT DEFAULT 0,
  `tags` VARCHAR(255) DEFAULT NULL,
  `episode` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `playlist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `cover` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 种子数据（公开外链，保证开箱可播，2026-08-09 已逐个实测 200）
-- 音乐：SoundHelix 免费演示曲目 + Kalimba + Sample
-- 视频：W3C / W3Schools / Test-Videos / MDN 开源演示视频
-- 封面：picsum.photos 占位图
-- ============================================================
INSERT INTO `music` (`title`, `artist`, `url`, `cover`, `duration`, `album`, `tags`) VALUES
('SoundHelix Song 1', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'https://picsum.photos/seed/music1/300/300', 444, 'SoundHelix Demo', '电子,纯音乐'),
('SoundHelix Song 2', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'https://picsum.photos/seed/music2/300/300', 370, 'SoundHelix Demo', '流行,纯音乐'),
('SoundHelix Song 3', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'https://picsum.photos/seed/music3/300/300', 309, 'SoundHelix Demo', '摇滚,纯音乐'),
('SoundHelix Song 4', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'https://picsum.photos/seed/music4/300/300', 406, 'SoundHelix Demo', '民谣,纯音乐'),
('SoundHelix Song 5', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'https://picsum.photos/seed/music5/300/300', 320, 'SoundHelix Demo', '古典,纯音乐'),
('SoundHelix Song 6', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', 'https://picsum.photos/seed/music6/300/300', 383, 'SoundHelix Demo', '电子,纯音乐'),
('SoundHelix Song 7', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', 'https://picsum.photos/seed/music7/300/300', 354, 'SoundHelix Demo', '爵士,纯音乐'),
('SoundHelix Song 8', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 'https://picsum.photos/seed/music8/300/300', 291, 'SoundHelix Demo', '轻音乐,纯音乐'),
('SoundHelix Song 9', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 'https://picsum.photos/seed/music9/300/300', 366, 'SoundHelix Demo', '氛围,纯音乐'),
('SoundHelix Song 10', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'https://picsum.photos/seed/music10/300/300', 338, 'SoundHelix Demo', '古典,纯音乐'),
('SoundHelix Song 11', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', 'https://picsum.photos/seed/music11/300/300', 401, 'SoundHelix Demo', '电子,纯音乐'),
('SoundHelix Song 12', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', 'https://picsum.photos/seed/music12/300/300', 427, 'SoundHelix Demo', '钢琴,纯音乐'),
('SoundHelix Song 13', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3', 'https://picsum.photos/seed/music13/300/300', 345, 'SoundHelix Demo', '爵士,纯音乐'),
('SoundHelix Song 14', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3', 'https://picsum.photos/seed/music14/300/300', 312, 'SoundHelix Demo', '民谣,纯音乐'),
('SoundHelix Song 15', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3', 'https://picsum.photos/seed/music15/300/300', 396, 'SoundHelix Demo', '摇滚,纯音乐'),
('SoundHelix Song 16', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3', 'https://picsum.photos/seed/music16/300/300', 378, 'SoundHelix Demo', '流行,纯音乐'),
('SoundHelix Song 17', 'SoundHelix', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-17.mp3', 'https://picsum.photos/seed/music17/300/300', 361, 'SoundHelix Demo', '电子,纯音乐'),
('Kalimba', 'LearningContainer', 'https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3', 'https://picsum.photos/seed/music18/300/300', 300, '免费演示音频', '轻音乐,纯音乐'),
('Sample MP3', 'FileSamples', 'https://filesamples.com/samples/audio/mp3/sample1.mp3', 'https://picsum.photos/seed/music19/300/300', 120, 'Sample 演示', '演示,纯音乐'),
('T-Rex Roar', 'MDN', 'https://interactive-examples.mdn.mozilla.net/media/cc0-audio/t-rex-roar.mp3', 'https://picsum.photos/seed/music20/300/300', 10, 'MDN 演示', '音效');

INSERT INTO `video` (`title`, `description`, `url`, `cover`, `duration`, `tags`, `episode`) VALUES
('Sintel Trailer', 'Blender 基金会开源动画短片《辛特尔》预告片', 'https://media.w3.org/2010/05/sintel/trailer.mp4', 'https://picsum.photos/seed/video1/640/360', 52, '动画,演示', 1),
('Bunny Trailer', '开源动画《大雄兔》预告片', 'https://media.w3.org/2010/05/bunny/trailer.mp4', 'https://picsum.photos/seed/video2/640/360', 33, '动画,演示', 1),
('Movie 300', 'W3C 标准媒体演示视频', 'https://media.w3.org/2010/05/video/movie_300.mp4', 'https://picsum.photos/seed/video3/640/360', 30, '演示,开源', 1),
('Movie 300 WebM', 'W3C 标准媒体演示视频（WebM 格式）', 'https://media.w3.org/2010/05/video/movie_300.webm', 'https://picsum.photos/seed/video4/640/360', 30, '演示,开源', 1),
('Sintel Trailer WebM', '《辛特尔》预告片 WebM 版本', 'https://media.w3.org/2010/05/sintel/trailer.webm', 'https://picsum.photos/seed/video5/640/360', 52, '动画,演示', 2),
('Big Buck Bunny', 'W3Schools HTML 演示视频《大雄兔》', 'https://www.w3schools.com/html/mov_bbb.mp4', 'https://picsum.photos/seed/video6/640/360', 60, '动画,演示', 1),
('Movie', 'W3Schools 演示视频', 'https://www.w3schools.com/html/movie.mp4', 'https://picsum.photos/seed/video7/640/360', 60, '演示,开源', 1),
('Big Buck Bunny HD', 'Test-Videos 开源高清演示（1080p 片段）', 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4', 'https://picsum.photos/seed/video8/640/360', 10, '动画,演示', 2),
('Jellyfish', 'Test-Videos 水母高清演示（1080p 片段）', 'https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4', 'https://picsum.photos/seed/video9/640/360', 10, '自然,演示', 1),
('Sintel HD', 'Test-Videos 开源动画《辛特尔》片段', 'https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4', 'https://picsum.photos/seed/video10/640/360', 10, '动画,演示', 3),
('Flower', 'MDN 开源演示视频《花朵》', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 'https://picsum.photos/seed/video11/640/360', 5, '演示,开源', 1),
('Flower WebM', 'MDN 开源演示视频《花朵》WebM 版本', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.webm', 'https://picsum.photos/seed/video12/640/360', 5, '演示,开源', 2);

INSERT INTO `playlist` (`name`, `description`, `user_id`, `cover`) VALUES
('演示歌单 - 纯音乐', 'SoundHelix 免费演示曲目合集', NULL, 'https://picsum.photos/seed/playlist1/400/400'),
('演示歌单 - 电子', '电子风格演示曲目', NULL, 'https://picsum.photos/seed/playlist2/400/400'),
('演示视频单', 'W3C/MDN 开源演示视频', NULL, 'https://picsum.photos/seed/playlist3/400/400');
