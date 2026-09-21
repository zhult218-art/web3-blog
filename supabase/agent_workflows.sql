-- ============================================================
-- Agent 会话台：Workflow 定义库 + 作品存档（web3-blog）
-- 用法：Supabase SQL Editor 粘贴执行（可重复执行），或在命令行 supabase db push。
-- 说明：
--   * agent_workflows ：技能工作流。前端用 /agent/workflow/match 按关键词匹配，
--                       后端（ai-proxy-service）按 enabled 实时拉取，未命中时使用内置兜底。
--   * agent_artifacts ：每次会话生成的作表/作表图/Word/海报 JSON 存档，RLS 公开读与写。
-- 后端起服务后无需重启即可生效（带 60s 缓存）。
-- ============================================================

-- ------------------------------------------------------------
-- 1) 技能工作流 agent_workflows
-- ------------------------------------------------------------
create table if not exists public.agent_workflows (
  id bigint generated always as identity primary key,
  slug text unique not null,
  name text not null,
  description text not null,
  icon text default '🤖',
  color text default '#a8b593',
  sample text,
  triggers text[] default '{}',
  steps jsonb not null default '[]'::jsonb,
  system_prompt text,
  output_format text not null default 'text',
  sort_order integer default 0,
  enabled boolean default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on column public.agent_workflows.steps is
  '[{"id":"topic","question":"...","placeholder":"...","type":"text|textarea|select","options":["..."]}]';
comment on column public.agent_workflows.output_format is
  'chart | table | word | image | text';

alter table public.agent_workflows enable row level security;

create policy "agent_workflows_public_read" on public.agent_workflows
  for select using (enabled = true or auth.role() = 'service_role');
create policy "agent_workflows_admin_write" on public.agent_workflows
  for all using (auth.role() = 'service_role')
  with check (auth.role() = 'service_role');

-- ------------------------------------------------------------
-- 2) 作品存档 agent_artifacts
-- ------------------------------------------------------------
create table if not exists public.agent_artifacts (
  id uuid default gen_random_uuid() primary key,
  workflow_slug text,
  title text,
  format text not null,           -- chart | table | word | image | text
  payload jsonb not null default '{}'::jsonb,
  meta jsonb default '{}'::jsonb,
  created_at timestamptz not null default now()
);

alter table public.agent_artifacts enable row level security;

create policy "agent_artifacts_public_read" on public.agent_artifacts
  for select using (true);
create policy "agent_artifacts_public_insert" on public.agent_artifacts
  for insert with check (true);

-- ============================================================
-- 2) 种子数据：四个内置技能
-- ============================================================
insert into public.agent_workflows
  (slug, name, description, icon, color, sample, triggers, steps, system_prompt, output_format, sort_order)
values
(
  'chart',
  '数据图表',
  '拆解你的数据，一步一步问出要点，最后用 ECharts 生成可视化图表（可下载 PNG）。',
  '📊',
  '#6ee7b7',
  '帮我画一张 2026 年各季度销售趋势折线图',
  array['图','图表','柱状','折线','饼图','环形','echarts','chart','可视化','统计图','数据图','做图','画图','趋势图','占比图'],
  '[
    {"id":"chartType","question":"你想做哪种类型的图表？","type":"select","options":["柱状图","折线图","饼图","环形图"]},
    {"id":"title","question":"给这张图表起个标题吧","placeholder":"例如：2026 年各季度销售额","type":"text"},
    {"id":"data","question":"提供数据，格式：类别=数值，多个用逗号分隔","placeholder":"例如：一月=120, 二月=150, 三月=98","type":"textarea"},
    {"id":"theme","question":"希望用哪种配色风格？","type":"select","options":["科技冷色","暖色渐变","青绿清爽"]}
  ]'::jsonb,
  '你是资深数据可视化工程师。根据用户回答输出一张 ECharts 配置。严格只输出一个 JSON 对象（不要代码块、不要解释）：{"title":图标标题,"legend":[],"tooltip":{},"xAxis":{"data":[...类别...]},"series":[{"name":"数值","type":bar|line|pie,"data":[...]}]}。pie/环形使用 series[0].data=[{name,value}]，不要 xAxis。' ,
  'chart',
  1
),
(
  'table',
  '表格整理',
  '把杂乱的文本整理成表格，可下载 CSV（Excel 可直接打开）。',
  '📋',
  '#67e8f9',
  '帮我把这几个城市的人口数据整理成一张表',
  array['表','表格','excel','csv','数据表','做表','整理成表','报表','行列','清单','对比表'],
  '[
    {"id":"title","question":"表格的名称/标题是什么？","placeholder":"例如：主要城市人口对比","type":"text"},
    {"id":"columns","question":"表头有哪些列？用逗号分隔","placeholder":"例如：城市,人口(万),地区","type":"text"},
    {"id":"rows","question":"逐行给数据：每行各列用逗号分隔，行之间用分号分隔","placeholder":"例如：北京,2188,华北;上海,2487,华东;广州,1868,华南","type":"textarea"}
  ]'::jsonb,
  '你是数据分析助手。根据用户回答输出一张结构化表格。严格只输出一个 JSON 对象（不要代码块）：{"title":表格标题,"columns":["列名",...],"rows":[["值",...],...]}。确保列数与每行单元格一一对应。' ,
  'table',
  2
),
(
  'word',
  '文档写作',
  '按文体要求一步步收集信息，最终生成一份可下载的 Word 文档。',
  '📄',
  '#fcd34d',
  '写一篇 800 字的项目季度工作总结',
  array['word','文档','写文档','doc','docx','报告','周报','工作总结','述职','文书','信函','通知','方案书','计划书','邮件'],
  '[
    {"id":"title","question":"文档标题/主题是什么？","placeholder":"例如：2026 年 Q3 项目工作总结","type":"text"},
    {"id":"style","question":"文档属于哪类文体？","type":"select","options":["工作总结","工作周报","方案书","通知公告","商务邮件","学习计划"]},
    {"id":"length","question":"篇幅要多长？","type":"select","options":["精简（约300字）","标准（约800字）","详细（约1500字）"]},
    {"id":"content","question":"需要包含哪些内容要点？（可选，可留空）","placeholder":"例如：上线了新功能、用户量翻倍、下一步计划","type":"textarea"}
  ]'::jsonb,
  '你是专业的文字工作者。根据用户的文体与篇幅写出结构完整的文档。严格只输出一个 JSON 对象（不要代码块）：{"title":标题,"sections":[{"heading":小节标题,"paragraphs":["段落",...],"bullets":["要点"]}],"footer":"落款或备注可选"}。' ,
  'word',
  3
),
(
  'image',
  '海报绘画',
  '一步一步收集画面主题、风格与文字，为你绘制一张可下载的海报/贺卡图片。',
  '🎨',
  '#e0aAff',
  '画一张科技风的新年贺卡',
  array['海报','贺卡','邀请函','宣传图','插画','封面图','背景图','画一幅','画一张','设计图','图片','image','卡片'],
  '[
    {"id":"topic","question":"画面主题是什么？想表达什么主体内容？","placeholder":"例如：宇宙飞船穿过星云","type":"text"},
    {"id":"style","question":"选择一种绘画风格","type":"select","options":["极简色块","科技未来","中国风","赛博霓虹"]},
    {"id":"text","question":"画面上要放一段主文字（可留空）","placeholder":"例如：新春快乐","type":"text"},
    {"id":"palette","question":"整体色调？","type":"select","options":["青紫霓虹","暖橙渐变","玉石青绿","黑白高级"]}
  ]'::jsonb,
  '你是平面设计师。根据用户回答绘制一张 SVG 海报。严格只输出一个 JSON 对象（不要代码块、不要HTML包裹）：{"svg":"<svg 宽度720 高度540 的 SVG XML 字符串>"}。要求：精美渐变背景、分层几何图形装饰、主题元素、居中排版主文字、整体协调不刺眼；只用 <rect><circle><path><text><defs><linearGradient><radialGradient>。' ,
  'image',
  4
)
on conflict (slug) do nothing;

-- ============================================================
-- 针对种子 system_prompt 的 JSON 校验提示（仅文档用途）
-- 若需自定义：UPDATE public.agent_workflows SET steps=... WHERE slug='...';
-- ============================================================