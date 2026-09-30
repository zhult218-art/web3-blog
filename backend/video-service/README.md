# Video Service 部署文档

视频上传 / ffmpeg 转码（HLS 自适应码率）/ OSS 签名分发 服务。

## 一、环境要求

1. **Python 3.10+**
2. **ffmpeg + ffprobe**（转码核心，须加入 PATH，或用环境变量指定路径）
   - Windows: `winget install ffmpeg` 或从 https://www.gyan.dev/ffmpeg/builds/ 下载
   - Linux: `apt install ffmpeg` / CentOS: `yum install ffmpeg`
3. （可选）**阿里云 OSS** 账号及 Bucket —— 未配置时自动降级为本地磁盘存储

## 二、安装依赖

```bash
cd backend/video-service
pip install -r requirements.txt
```

## 三、配置（全部可用环境变量覆盖）

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `VIDEO_ADMIN_TOKEN` | 管理员 token（**生产必改**） | `change-me-admin-token` |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 AK | 空（未配置=本地存储模式） |
| `OSS_ENDPOINT` | 如 `https://oss-cn-hangzhou.aliyuncs.com` | 空 |
| `OSS_BUCKET` | Bucket 名 | 空 |
| `FFMPEG_PATH` / `FFPROBE_PATH` | 可执行文件路径 | `ffmpeg` / `ffprobe` |
| `VIDEO_HOST` / `VIDEO_PORT` | 监听地址/端口 | `0.0.0.0` / `8300` |

其他业务参数（分片时长、三档码率、短视频阈值 120s、并发数 2 等）在 [app/config.py](app/config.py) 顶部集中修改。

- **OSS 配置位置**：Bucket 需开启「公共读关闭 + 私有读写」，URL 全部走服务端 6 小时签名（防盗链）。
- **域名**：给 Bucket 绑定 CDN/自定义域名后，把播放域名配置到播放页 `API_BASE`（见下）。

## 四、启动

```bash
cd backend/video-service/app
uvicorn main:app --host 0.0.0.0 --port 8300
# 或
python main.py
```

- 后台管理页：`http://127.0.0.1:8300/static/admin.html`
- 播放页：`http://127.0.0.1:8300/static/player.html?vid=<video_id>`
- 接口文档（Swagger）：`http://127.0.0.1:8300/docs`

## 五、前端对接

两页均为原生 HTML，**后端地址在页面顶部全局常量**：

```js
// player.html / admin.html 顶部
const API_BASE = 'http://127.0.0.1:8300';   // 换成你的服务域名
```

## 六、接口列表

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/video/upload` | ✅ X-Admin-Token | 上传 mp4（≤2GB），返回 `video_id` |
| GET | `/api/video/task/{video_id}` | ✅ | 查询转码状态/进度百分比 |
| GET | `/api/video/list?page=1&size=10` | 公开 | 分页视频列表 |
| GET | `/api/video/{video_id}` | 公开 | 播放信息（m3u8/mp4/封面 URL 已带 6h 签名） |
| DELETE | `/api/video/{video_id}` | ✅ | 删除视频并清理 OSS 全部资源 |

## 七、转码规则（速览）

- HLS：4 秒分片，3 档清晰度（1080p@3500k / 720p@2000k / 480p@1000k），音频 aac 128k
- 短视频（≤120s）：额外产出 720p faststart mp4，播放页优先 mp4、弱网自动降级 m3u8
- 封面：自动截第 3 秒画面，≤720p
- 并发：同一时刻最多 2 个转码任务，其余内存队列排队；完成后删除服务器临时文件

## 八、安全重点（必读）

1. **绝对不要把本服务直接暴露公网承载实盘/私密内容**：管理接口虽有 token，但服务本身无 HTTPS/限流，生产环境请置于 Nginx 反代 + HTTPS + IP 白名单之后。
2. `VIDEO_ADMIN_TOKEN` 默认值仅为开发用，**上线前必须更换**。
3. 博客对外展示建议只嵌入「播放页链接」，管理页仅内网使用。
4. OSS 走 6 小时签名 URL，过期自动失效，防批量爬取；如需更强保护可在 OSS 控制台加 Referer 白名单。
