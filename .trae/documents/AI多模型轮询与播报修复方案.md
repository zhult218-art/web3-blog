# AI 多模型轮询兜底 + 星途播报缺失修复方案

## Context（为什么要做）

用户反馈两个问题：

1. **AI 经常被限流/断掉** —— 希望「多给我加几个免费模型，互相用，不要断我的 ai」。
2. **星途助手「有的输出没有播报」** —— 屏幕上出现了文字但没读出来。

### 排查结论

**问题 1 的根因：全站 AI 只挂了「一个免费网关」**

- [.env](file:///z:/project/myblog/web3/.env#L37-L39) 与 [secrets.json](file:///z:/project/myblog/web3/py-service/data/cockpit/user_data/secrets.json) 都指向 **Pollinations**：`https://text.pollinations.ai/openai` + `sk-pollinations-free`。
- 前端 AI 经 Vite 代理 `/pyai` → [router.py](file:///z:/project/myblog/web3/py-service/modules/ai/router.py) 的 `/api/ai/chat`、`/api/ai/chat/stream`；上游配置由 [service.py](file:///z:/project/myblog/web3/py-service/modules/ai/service.py) 的 `resolve_config()` 解析出**单一** `{base_url, api_key, model}`，失败时只对**同一个** provider 重试 3 次。→ 上游一限流/宕机，AI 直接断。
- 重要发现：项目**已经有**一个完整的多渠道路由实现（Java [ChannelRouterService.java](file:///z:/project/myblog/web3/backend/ai-proxy-service/src/main/java/com/web3/aiproxy/service/ChannelRouterService.java)、`proxy_channel` 表、按权重候选排序、失败自动禁用），**但星途助手与博客 AI 走的是 py-service 直连路径，完全没经过它**。
- [application.yml](file:///z:/project/myblog/web3/backend/ai-proxy-service/src/main/resources/application.yml) 里已列出 10 个 provider（NightRouter/DeepSeek/SiliconFlow/LM Studio/Groq/Doubao/Together/Fireworks/OpenRouter/Ollama），但其 Agent LLM 也**只用第一个 key 非空的**，没有轮换。

**问题 2 的根因：播报与展示不同步**

- 语音链路：`text` 事件只上屏、`sentence` 事件才进 TTS 队列（[router.py](file:///z:/project/myblog/web3/py-service/modules/voice/router.py#L47) 的 `SENT_DELIM` 按句切分）。
- **被说出口的垃圾**：`[[GUIDE]]{...}` 引导块是**跟着 delta 一起流下来**的，而 `SENT_DELIM` 含 `,` —— JSON 里的逗号会被当成句子分隔符，于是**把 JSON 片段朗读出来**（`extract_llm_guide` 只在最后整段剥离，来不及阻止）。
- **上屏却不播报**（已定位到行）：
  - [XingTuAssistant.vue](file:///z:/project/myblog/web3/frontend/src/components/common/XingTuAssistant.vue#L768)：错误分支 `addMsg('assistant', '抱歉，' + …)` 无 `speak()`。
  - [同文件 L756](file:///z:/project/myblog/web3/frontend/src/components/common/XingTuAssistant.vue#L756)：导航引导 `addMsg('assistant', msg.tip)` 无 `speak()`。
  - [同文件 L1736](file:///z:/project/myblog/web3/frontend/src/components/common/XingTuAssistant.vue#L1736)：首次访问欢迎语 `addMsg(...)` 无 `speak()`。

**目标**：AI 上游多 provider 自动轮询 + 无感切换，任一免费模型被限流/宕机都不中断；同时让每个应播报的输出都真正发声、且不再朗读 JSON。

---

## 方案 A：py-service AI 多 provider 轮询兜底

### A1. 新增 provider 解析与轮询（核心）

新增 `py-service/modules/ai/providers.py`，集中实现「候选列表 + 轮询 + 冷却」，供 **AI 通道**与**语音通道**共用（避免两边各写一套）：

- `resolve_providers() -> list[Provider]`，按优先级合并（可复用 [service.py](file:///z:/project/myblog/web3/py-service/modules/ai/service.py#L45-L51) 现有的三级 fallback 思路）：
  1. `secrets.json` / env `AI_PROVIDERS`（JSON 数组，可配多个，含 `{name, base_url, api_key, model}`）；
  2. 现有单条 `AI_*`（保持向后兼容，作为其中一个 provider）→ 保证**不改配置也不回归**；
  3. **内置免费预设目录**（配了对应 KEY 才启用，未配自动跳过）。
- 模块级 `round-robin` 游标：避免每次都先打第一个，把请求摊到多个免费额度上。
- 失败推进策略：
  - 单 provider 内对 `402/408/409/425/429/5xx` 重试 1 次；
  - 仍失败 → **切下一个 provider**（这是「不要断我的 ai」的核心）；
  - 命中 `429/402` → 记 `cooldown_until`，冷却期内优先跳过，但仍作最后兜底；全部冷却/失败时才真正报错。
- 流式特殊规则：**只有在「尚未向用户下发任何内容」时才允许切换 provider**（切换后用户完全无感）；已下发内容后失败则静默收尾（不重复、不串台）。

### A2. 内置免费模型目录（研究所得，按需自动启用）

| provider | base_url | 是否需要 key | 备注 |
| --- | --- | --- | --- |
| **Pollinations**（当前在用） | `https://text.pollinations.ai/openai` | 否（keyless） | 可多模型轮换：`openai`、`openai-fast`、`mistral` 等 —— **零注册即可获得冗余** |
| OpenRouter | `https://openrouter.ai/api/v1` | 是（免费注册，无需信用卡） | 30+ 免费模型，如 `deepseek/deepseek-chat-v3:free`、`meta-llama/llama-3.3-70b-instruct:free`、`qwen/qwen3-coder:free`、`openai/gpt-oss-120b:free`；免费档约 20 RPM / 50 RPD |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/openai/` | 是（免费档） | OpenAI 兼容端点，`gemini-2.5-flash` 等 |
| Groq | `https://api.groq.com/openai/v1` | 是（免费档） | 注意免费档已下架 Llama 3.3 70B，改用 `llama-3.1-8b-instant` / `openai/gpt-oss-20b` |
| Mistral | `https://api.mistral.ai/v1` | 是（免费档） | `open-mistral-nemo` 等 |
| SiliconFlow | `https://api.siliconflow.cn/v1` | 是 | 复用 [application.yml](file:///z:/project/myblog/web3/backend/ai-proxy-service/src/main/resources/application.yml) 已有地址，部分小模型免费 |

> 说明：除 Pollinations 外，**免费档都需要你注册领一个 key**（免费、多数不需要信用卡）。方案会自动识别：**配了就启用，没配就跳过**，不会因为缺 key 报错。我不会硬编码任何来路不明的第三方"免费网关"（安全与稳定性风险）。

### A3. 改造调用点（统一走轮询）

- [modules/ai/service.py](file:///z:/project/myblog/web3/py-service/modules/ai/service.py)：`chat()`、`stream_chat()` 改为遍历 `resolve_providers()` 的候选列表；`health()` 返回 provider 数量/名称/就绪状态（**不泄露 key**）。
- `modules/voice/llm.py`：改为复用 `providers.py`，让**语音助手**同样享受轮询兜底（这是最需要"不断线"的路径）。
- [.env.example](file:///z:/project/myblog/web3/.env) 补充：`AI_PROVIDERS`(JSON) 与各免费 provider 的 KEY 变量 + 获取说明注释。
- **不改** `secrets.json`（运行时现场配置），也不动 Java `ai-proxy-service`。

---

## 方案 B：星途播报修复

### B1. 服务端：引导块绝不朗读（[voice/router.py](file:///z:/project/myblog/web3/py-service/modules/voice/router.py)）

- 流式下发前**先剥离 `[[GUIDE]]`**：维护一个"待定尾巴"（保留 `len(mark)-1` 个字符防止半个标记被提前下发）；一旦发现 `[[GUIDE]]` 起点，**立即停止**把后续内容作为 `text`/`sentence` 下发，改为暂存为 guide 文本，最后交给 `extract_llm_guide`。
- 收尾 flush 同样先剥离，确保 `buf` 里不会残留 JSON 被当句子读出。
- 效果：**不再朗读 JSON**，同时不丢正文。

### B2. 前端：补齐"上屏即播报"（[XingTuAssistant.vue](file:///z:/project/myblog/web3/frontend/src/components/common/XingTuAssistant.vue)）

- L768 错误分支：补 `speak(...)`（配合已有 `friendlyServiceError`，确保**不泄露端口/内网地址**）。
- L756 引导 tip：补 `speak()`，并加"本次回复是否已播报过"的守卫，避免与流式句子重复朗读。
- L1736 首访欢迎：接入 `speak()`；若音频尚未解锁（首次访问无用户手势），沿用现有的"点击麦克风开始"提示逻辑，不报错。
- `endStreamMsg('（没有获取到内容）')` 的占位文案**保持静默**（纯 UI 占位，不是真正回复）。

---

## 涉及文件

- 新增 `py-service/modules/ai/providers.py`（候选解析 + 轮询 + 冷却）
- `py-service/modules/ai/service.py`（chat / stream_chat / health 改走轮询）
- `py-service/modules/voice/llm.py`（复用轮询）
- `py-service/modules/voice/router.py`（剥离 `[[GUIDE]]`，杜绝朗读 JSON）
- `frontend/src/components/common/XingTuAssistant.vue`（L756 / L768 / L1736 补播报）
- `.env.example`（多 provider 配置说明与示例）

## 验证方式

1. **轮询单测（不需要重启服务）**：用 `py-service/.venv/Scripts/python.exe` 跑一段脚本，构造 3 个假 provider（第 1、2 个返回 429/500，第 3 个正常），断言 `chat()` 最终由第 3 个 provider 返回，且冷却记录正确；断言全部失败时才抛错。
2. **流式切换**：构造"首个 provider 在**未下发任何内容**前失败"，断言自动切到下一个且用户侧流连续。
3. **真实上游**：重启 py-service（`start-all-services.ps1` 中 `python -m uvicorn main:app --port 9006`），`curl http://127.0.0.1:9006/api/ai/health` 应列出多个就绪 provider；`curl -X POST .../api/ai/chat` 返回正常。
5. **播报回归**：打开站点唤醒星途 → 触发一次导航引导、一次错误回复、一次首访欢迎，确认三者都有语音；且**不再听到 JSON 片段**。用 browser_use 打开 `http://localhost:5173` 检查控制台无新增报错。
6. **前端构建**：`node node_modules/vite/bin/vite.js build` 必须通过。

## 需要你知悉的前提

- 除 Pollinations（keyless）外，其余免费模型**需要你注册领 key**（免费）。我会把「多 provider 轮询 + 自动兜底」的骨架与预设目录全部写好：**你现在什么都不填也能立即获得 Pollinations 多模型轮询**；之后你只要把某个 key 填进 `.env`，对应 provider 就自动加入轮询。
- 本方案**不改你的 `secrets.json` 现场配置、不改 Java 服务**，可随时回退。
