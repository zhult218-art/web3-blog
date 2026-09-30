# 前端巨型组件拆分计划（2026-09-17）

## 背景

体检报告 §6 指出前端有 **13 个巨型组件**，最大者
[quant/index.vue](src/views/quant/index.vue) **2164 行**。这些文件同时承载：
- 模板（template）
- 业务状态（refs / reactive）
- 多个独立功能块（AI 配置、行情、策略、回测、聊天、Markdown 导出等）
- HTTP 调用、流式 SSE 处理、本地存储

单文件超大导致：首屏解析慢、HMR 重编译耗时、AI 工具修改易越界、维护成本陡增。

## 拆分原则

1. **先行为后结构**：每个子组件先用 `v-model` + `props/emit` 隔离状态，
   不引入 Pinia 全局 store（避免一次性重构面太大）。
2. **保持视觉一致**：UI 不变，只做代码切分，不改样式与交互。
3. **样板先行**：本计划只示范拆 1 个子组件 `AiConfigPanel.vue`，
   其余 12 个按相同流程在后续会话进行。

## 13 个巨型组件清单（按行数排序，体检报告 §6）

| 行数 | 文件 | 拆分建议 |
| --- | --- | --- |
| 2164 | `views/quant/index.vue` | 拆 6 块：AiConfigPanel / MarketQuote / StrategyTable / BacktestChart / ChatPanel / ExportPanel |
| ~1600 | `views/jarvis/index.vue` | 拆 4 块：VoiceInput / ChatHistory / CommandPalette / ToolUse |
| ~1400 | `views/admin/Dashboard.vue` | 拆 4 块：StatCards / ServiceHealth / OperationLogTable / VisitLogChart |
| ... | （其余 10 个） | 见体检报告 §6 |

## 样板：AiConfigPanel.vue（建议下一步会话执行）

从 `quant/index.vue` line 881-1010 的 "AI Config Panel" 区块抽出为子组件。

### 抽取目标
- 模板：约 80 行（input 表单 + 按钮）
- 状态：`aiConfig` ref + `showAiConfig` ref + `saveAiConfig` / `loadAiConfig` 函数
- props：`modelValue`（控制显示）+ `config`（v-model 双向绑定）
- emit：`update:modelValue`、`update:config`、`save`

### 拆分步骤
1. 新建 `frontend/src/views/quant/components/AiConfigPanel.vue`
2. 复制模板 + 抽出函数到子组件
3. 父组件 `quant/index.vue` 用 `<AiConfigPanel v-model="showAiConfig" v-model:config="aiConfig" @save="onSaveAiConfig" />` 替换原区块
4. 移除父组件中已搬走的 `aiConfig` ref 和 `saveAiConfig` / `loadAiConfig` 函数
5. 验证：UI 视觉一致、配置保存路径仍是 sessionStorage → localStorage（详见 quant/index.vue line 1860-1886 的修复）

### 验收清单
- [ ] 配置保存到 sessionStorage（apiKey）+ localStorage（baseURL/model），不再回到 localStorage
- [ ] 关闭浏览器后 apiKey 失效
- [ ] AiConfig 内的 baseURL 仍能被 ChatPanel 读取（通过父组件的 aiConfig ref 传递）

## 其余 12 个组件拆分顺序建议

1. `admin/Dashboard.vue` （~1400 行）— 业务最独立，拆分风险最低
2. `jarvis/index.vue` （~1600 行）— 含 SSE 流式逻辑，拆分难度高
3. `quant/index.vue` 其余 5 块（按样板流程）
4. ...其余 9 个

## 不在本次范围

- 不引入 Pinia 全局 store（避免一次性重构面太大）
- 不改样式（tailwind class 不动）
- 不改路由（每个 .vue 路由保持不变）
- 不改后端 API 契约
