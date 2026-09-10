# 第三方 API 工具中心接入文档

> 本系统内置「API 工具中心」页面（`/tools/api`），聚合多个免费 API 平台的能力，统一通过后端网关代理调用，密钥保存在服务端，前端不直接暴露。

---

## 1. 总体架构

```
浏览器 (前端 /tools/api)
   │  GET /api/{platform}/...   (vite dev 代理 /api → 网关 8080)
   ▼
Spring Cloud Gateway (:8080)
   ├─ /j8y/**    → https://api.j8y.cn       (服务端注入 app_key)
   ├─ /showapi/**→ https://route.showapi.com (服务端注入 appKey)
   └─ /shanhe/** → https://api.shanhe.kim   (公开接口免密钥)
   ▼
第三方 API 平台
```

- 前端统一走 `/api/j8y/...`、`/api/showapi/...`、`/api/shanhe/...` 前缀。
- 网关路由定义：`backend/gateway/src/main/resources/application.yml`。
- 密钥通过网关 `AddRequestParameter` 过滤器在服务端注入，前端代码中无任何密钥。

---

## 2. 密钥清单

| 平台 | 密钥项 | 网关环境变量 | 状态 |
| --- | --- | --- | --- |
| FreeAPI (j8y.cn) | AppKey | 直接写在 yaml（已配置） | ✅ 已配置 |
| 万维易源 ShowAPI | AppKey | `SHOWAPI_APP_KEY` | ⚠️ 需自行注册填写 |
| 山河云 (shanhe.kim) | 私有接口需 Token | 前端可选透传 `apikey` 参数 | ✅ 公开接口免密钥 |

### 2.1 FreeAPI (https://api.j8y.cn)
- 已配置密钥：`ak_6086d3c8d256b083dff13942e3a15efb`（用户自有账号）
- 网关路由：
```yaml
- id: api-j8y
  uri: https://api.j8y.cn
  predicates:
    - Path=/j8y/**
  filters:
    - StripPrefix=1
    - AddRequestParameter=app_key, ak_6086d3c8d256b083dff13942e3a15efb
```
- 前端调用示例：`GET /api/j8y/api/gateway.php?api_path=ip-lookup&ip=8.8.8.8`
- 限额：1000 次/日（大部分接口），10 QPS。

### 2.2 万维易源 ShowAPI (https://www.showapi.com)
- 需在 showapi 官网注册并创建应用获取 AppKey，然后填入网关：
```yaml
- id: api-showapi
  uri: https://route.showapi.com
  predicates:
    - Path=/showapi/**
  filters:
    - StripPrefix=1
    - AddRequestParameter=appKey, ${SHOWAPI_APP_KEY:}
```
- 前端调用示例：`GET /api/showapi/6-1?num=18908711234`

### 2.3 山河云 (https://api.shanhe.kim)
- 公开接口无需密钥；文档标注「需 Token」的接口（如摸鱼日历、快递查询等）需注册获取 Token，前端调用时传 `apikey=xxx` 参数（透传）。

---

## 3. 已接入工具清单

### 3.1 FreeAPI (j8y.cn) — 网关 `/j8y/`

| 工具 | api_path | 参数 | 返回要点 | 实测状态 |
| --- | --- | --- | --- | --- |
| IP 归属地查询 | `ip-lookup` | `ip` | country / region / city / isp / org / lat / lon | ✅ 正常 |
| QQ 信息查询 | `cxqq` | `qq` | 昵称 / 头像 / 邮箱 / VIP 等级 | ✅ 正常 |
| 随机猫咪图片 | `cat` | 无 | 猫图 url / 宽高 | ✅ 正常 |
| 历史上的今天 | `history` | 无 | data.data.list[]: year / title / type / link | ✅ 正常 |
| 三角洲每日密码 | `sjzmm` | 无 | 每日密码 + 更新日期 | ✅ 正常 |
| 抖音去水印 | `dyqsy` | `url` | 无水印视频/图集链接 | ✅ 正常(需真实链接) |
| 天气查询 | `weather` | `city` | — | ⚠️ 上游 500，待平台修复 |
| 网易云无损解析 | `wy_music` | 待确认 | — | ⚠️ 参数格式未公开 |
| 汽水音乐解析 | `qsyy` | `url` | — | ⚠️ 上游 500 |
| 图片安全检查 | `txtp` | `url` | — | ⚠️ 上游报错 |

> ⚠️ 状态异常的接口在页面中提供「通用调用」面板（任意参数透传 + 原始 JSON 展示），便于排查与后续参数调整。

### 3.2 万维易源 ShowAPI — 网关 `/showapi/`

| 工具 | 接入点 | 参数 | 返回要点 |
| --- | --- | --- | --- |
| 手机号归属地查询 | `6-1` | `num` | prov / city / areaCode / postCode / cityCode / name(运营商) / type |
| 外汇币种列表 | `105-35` | 无 | exchange_list[]: flag / name / code |
| 银行汇率查询 | `105-30` | `code`(如 USD) | list[]: name / code / hui_in / hui_out / zhesuan / chao_in / chao_out / time / day |
| 汇率转换 | `105-31` | 见官方文档 | — |
| 历史汇率查询 | `105-34` | 见官方文档 | — |
| 星座运势查询 | `872-1` | `star`(小写拼音) / `date`(MMDD) / `needTomorrow` / `needWeek` / `needMonth` / `needYear` | day: general_txt / love_txt / work_txt / money_txt / 各星指数 / lucky_num / lucky_direction / grxz |

星座 star 参数对照：
`baiyang` 白羊、`jinniu` 金牛、`shuangzi` 双子、`juxie` 巨蟹、`shizi` 狮子、`chunv` 处女、
`tiancheng` 天秤、`tianxie` 天蝎、`sheshou` 射手、`mojie` 摩羯、`shuiping` 水瓶、`shuangyu` 双鱼。

### 3.3 山河云 (shanhe.kim) — 网关 `/shanhe/`（公开接口）

| 工具 | 接口路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| 星座运势 | `/API/星座.php` | `name`(如 白羊) / `type=json` | overall / career / fortune / love 等 |
| 今日天气 | `/API/天气.php` | `city`(必填) / `type=json` | 详细天气 |
| 农历查询 | `/API/农历.php` | `type=json` | 今日公历/农历/节气 |
| 历史上的今天 | `/API/历史上的今天.php` | `type=json` / `num` | 事件列表 |
| 随机一言 | `/API/一言.php` | `type=json` | 随机句子 |
| 微博热榜 | `/API/微博热榜.php` | `type=json` | 热搜列表 |
| 知乎热榜 | `/API/知乎热榜.php` | `type=json` | 热榜列表 |
| 摸鱼日历 | `/API/摸鱼日历.php` | `style=1/2/3` / `apikey`(需 Token) | 日历图 |
| 随机壁纸 | `/API/随机壁纸.php` | `apikey`(需 Token) | 壁纸图 |
| 必应壁纸 | `/API/必应壁纸.php` | `type=json` | 每日壁纸 |

> 山海云更多接口见 https://api.shanhe.kim/ （接口路径 = `/API/{接口名}.php`，接口名为中文）。

---

## 4. 前端页面

- 路由：`/tools/api`（`frontend/src/views/tools/ApiTools.vue`）
- 入口：`frontend/src/views/tools/index.vue` 的「在线工具」网格新增「API 工具中心」卡片
- API 封装：`frontend/src/api/third-party.js`（j8y / showapi / shanhe 三组方法）
- 页面能力：
  - 平台分组卡片网格，点击进入工具面板
  - 结构化结果渲染 + 「原始 JSON」切换查看
  - 复制结果
  - 通用调用面板：任意参数透传，返回原始 JSON

---

## 5. 可扩展平台（本次仅文档化）

### 5.1 RapidAPI (https://rapidapi.com)
- 全球最大 API 市场，搜索 `Data` 分类可获得海量接口；需注册并绑定信用卡（有免费额度层级）。
- 调用需请求头 `x-rapidapi-key` / `x-rapidapi-host`。
- 接入建议：网关增加
```yaml
- id: api-rapidapi
  uri: https://${RAPIDAPI_HOST}   # 例如 community-open-weather-map.p.rapidapi.com
  predicates:
    - Path=/rapidapi/**
  filters:
    - StripPrefix=1
    - AddRequestHeader=X-RapidAPI-Key, ${RAPIDAPI_KEY:}
    - AddRequestHeader=X-RapidAPI-Host, ${RAPIDAPI_HOST:}
```

### 5.2 万维易源免费市场更多接口 (https://www.showapi.com/market?type=member)
免费可用：新闻 (109)、油价 (138)、身份证解析 (25)、坐标转换 (1252)、IP 定位 (20)、黄历 (856)、节假日 (894)、ISBN (1626)、图片处理 (1)、行政区划 (1149)、菜谱 (1164)、验证码 (26)、笑话 (341)、藏头诗 (950)、二维码 (887)、历史上的今天 (119) 等。
接入方式：按「接入点」路径新增网关透传（均为 `/showapi/{产品}-{接入点}`），前端在 ApiTools.vue 工具配置数组中追加即可。

---

## 6. 部署与验证

```bash
# 1. 网关配置修改后重新打包并重启
cd backend/gateway && mvn package -DskipTests
# 重启 gateway（先停止占用 8080 的旧进程）

# 2. 验证代理链路
curl "http://localhost:8080/j8y/api/gateway.php?api_path=ip-lookup&ip=8.8.8.8"
curl "http://localhost:8080/shanhe/API/%E6%98%9F%E5%BA%A7.php?name=%E7%99%BD%E7%BE%8A&type=json"
curl "http://localhost:8080/showapi/105-35"        # 配置 SHOWAPI_APP_KEY 后可用

# 3. 前端
cd frontend && npm run build
```

---

## 7. 注意事项

1. **密钥安全**：除 showapi/shanhe 需用户自行注册的密钥外，j8y 密钥已固化在网关 yaml；切勿将密钥写进前端代码。
2. **限流配额**：各平台均有每日配额（j8y 1000 次/日、10 QPS），页面无缓存，高频使用前建议评估。
3. **数据合规**：外汇报价、星座运势等数据仅供娱乐/参考，页面已标注免责声明。
4. **接口异常**：j8y 部分接口（weather/wy_music 等）平台侧不稳定，已做降级处理（提示 + 原始 JSON），待平台修复后自动恢复。
