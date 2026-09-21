-- ============================================================
-- Web3 Blog · 个人知识库 + 博客体系种子
-- 在 Supabase Dashboard → SQL Editor 中整段执行即可（可重复执行）
--
-- 内容：
--   1) kb_entries 知识库表（成长阶段 seedling/growing/mastered）+ RLS
--      - 匿名只读 published=true
--      - 站主用 Supabase Auth 登录后可全部增删改
--   2) 知识库种子：覆盖 硬件底层/嵌入式/软件工程/网络工程/网络安全/运维与效率/前沿技术
--   3) 博客 7 篇详细文章：
--      - 免费远程完全指南：RustDesk + Tailscale
--      - Jev：Vercel AI Gateway 的结构化决策模型
--      - 硬件底层 / 嵌入式 / 软件工程 / 网络工程 / 网络安全 五大体系全景
-- ============================================================

-- ---------- 1. 知识库表 ----------
create table if not exists public.kb_entries (
  id            bigint generated always as identity primary key,
  title         text not null,
  slug          text not null,
  summary       text default '',
  content       text default '',
  category      text default '软件工程',
  subcategory   text default '',
  tags          text[] default '{}',
  stage         text default 'seedling' check (stage in ('seedling','growing','mastered')),
  source        text default '原创',
  source_url    text default '',
  difficulty    int  default 1 check (difficulty between 1 and 5),
  published     boolean default true,
  view_count    int default 0,
  -- 艾宾浩斯 / SM-2 简化复习调度
  review_count  int default 0,            -- 已复习次数
  review_interval int default 1,          -- 当前间隔（天）
  ease_factor   numeric default 2.5,      -- 难度系数 EF，范围 1.3~3.0
  next_review   timestamptz default now(),-- 下次应复习时间
  last_review   timestamptz,
  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);

create unique index if not exists kb_entries_slug_key on public.kb_entries(slug);
create index if not exists kb_entries_category_idx on public.kb_entries(category);
create index if not exists kb_entries_stage_idx on public.kb_entries(stage);
create index if not exists kb_entries_next_review_idx on public.kb_entries(next_review) where published = true;

-- 若表已存在（首次建表时上面 create table if not exists 不会改结构），补加复习字段
do $$
begin
  if not exists (select 1 from information_schema.columns where table_schema='public' and table_name='kb_entries' and column_name='review_count') then
    alter table public.kb_entries add column review_count int default 0;
    alter table public.kb_entries add column review_interval int default 1;
    alter table public.kb_entries add column ease_factor numeric default 2.5;
    alter table public.kb_entries add column next_review timestamptz default now();
    alter table public.kb_entries add column last_review timestamptz;
    create index if not exists kb_entries_next_review_idx on public.kb_entries(next_review) where published = true;
  end if;
end $$;

-- updated_at 自动刷新
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;

drop trigger if exists trg_kb_touch on public.kb_entries;
create trigger trg_kb_touch before update on public.kb_entries
for each row execute function public.touch_updated_at();

-- ---------- RLS ----------
alter table public.kb_entries enable row level security;

drop policy if exists "kb_public_read" on public.kb_entries;
create policy "kb_public_read" on public.kb_entries
  for select to anon using (published = true);

drop policy if exists "kb_owner_all" on public.kb_entries;
create policy "kb_owner_all" on public.kb_entries
  for all to authenticated
  using (true) with check (true);

-- ============================================================
-- 2. 知识库种子（幂等：按 slug upsert）
-- ============================================================
insert into public.kb_entries (title, slug, summary, content, category, subcategory, tags, stage, source, difficulty)
values

-- ============ 硬件底层 ============
('计算机的层次：从晶体管到一条指令', 'hw-layers-transistor-to-instruction',
 '理解二进制世界的物理根基：晶体管→逻辑门→组合/时序电路→CPU 取指执行。',
$md$# 计算机的层次：从晶体管到一条指令

## 一、抽象层次
1. **器件层**：MOSFET 晶体管，工作在截止/饱和区，本质是电控开关。
2. **逻辑门层**：与/或/非/异或，CMOS 实现；NAND 是万能门。
3. **组合逻辑**：加法器（半加/全加/行波进位/超前进位）、多路选择器 MUX、译码器。
4. **时序逻辑**：锁存器/触发器（D 触发器）、寄存器、时钟与建立保持时间。
5. **数据通路 + 控制器 = CPU**：ALU + 寄存器堆 + PC + 控制单元。

## 二、一条指令的生命周期（以 add x1,x2,x3 为例）
1. **取指 IF**：PC → 指令存储器，取出机器码，PC += 4。
2. **译码 ID**：解析 opcode/funct，读寄存器堆。
3. **执行 EXE**：ALU 完成加法（RISC 流水线中这是第三级）。
4. **访存 MEM**：本指令无内存访问，直接流过。
5. **写回 WB**：结果写回 x1。

> 经典五级流水线：IF → ID → EX → MEM → WB；冒险（结构/数据/控制）靠转发(forwarding)、停顿(stall)、分支预测解决。

## 三、关键量化定律
- **摩尔定律**：晶体管密度约 2 年翻倍（已放缓，转向多核/3D 封装）。
- **Amdahl 定律**：加速比 = 1 / ((1-p) + p/s)，优化瓶颈部件才有效。
- **功耗墙**：动态功耗 P ≈ α·C·V²·f，所以提频越来越难。

## 四、动手验证
- Logisim / Digital 画一个全加器和 4 位计数器。
- 用 `cat /proc/cpuinfo`、CPU-Z 看本机微架构（如 Intel Golden Cove / ARM Cortex-A78）。
$md$,
'硬件底层', '计算机组成原理', array['组成原理','晶体管','流水线'], 'mastered', '书籍笔记', 2),

('Cache 与存储层级：为什么程序会"不够快"', 'hw-cache-memory-hierarchy',
 '寄存器→L1/L2/L3 Cache→内存→SSD→HDD 的速度差可达数百倍，局部性决定性能。',
$md$# Cache 与存储层级

## 层级与量级延迟（参考）
| 层级 | 延迟量级 |
|---|---|
| 寄存器 | <1ns |
| L1 Cache | ~1ns（4 cycles） |
| L2 | ~3-5ns |
| L3 | ~15ns |
| DDR 内存 | ~80-100ns |
| NVMe SSD | ~10μs |
| SATA SSD | ~100μs |
| HDD | ~10ms |

## 核心概念
- **时间局部性**：刚访问的数据很快再访问（循环变量）。
- **空间局部性**：相邻地址会被访问（数组顺序遍历）→ 按 cache line（通常 64B）取。
- **映射方式**：直接映射 / 全相联 / n 路组相联。
- **写策略**：write-through vs write-back；write-allocate vs no-write-allocate。
- **伪共享 false sharing**：多核线程变量落在同一 cache line 导致频繁失效，填充字节对齐解决。

## 性能分析命令
```bash
# Linux 查看 cache
lscpu | grep -i cache
# 性能剖析
perf stat -e cache-misses,cache-references ./your_program
```

## 工程启示
- 遍历多维数组按内存布局顺序（C 语言行优先）。
- 热点数据结构紧凑、对齐；避免链表式随机跳转。
- Redis 本质就是用 DRAM 的速度对抗磁盘延迟。
$md$,
'硬件底层', '存储体系（Cache/内存/磁盘）', array['Cache','性能','局部性'], 'growing', '实战总结', 3),

('总线与接口速查：PCIe / USB / SATA / DDR', 'hw-bus-interfaces-cheatsheet',
 '主流计算机总线与外设接口的速率、形态与适用场景速查表。',
$md$# 总线与接口速查

## PCIe（高速串行，点对点，全双工）
- 通道 x1/x4/x8/x16；每通道速率：3.0≈8GT/s、4.0≈16GT/s、5.0≈32GT/s，编码 128b/130b。
- PCIe 4.0 x16 单向理论 ≈ 31.5 GB/s；显卡、NVMe（常 x4）用它。
- 热插拔、SR-IOV 虚拟化直通。

## USB
| 版本 | 标称速率 | 常见接口 |
|---|---|---|
| USB 2.0 | 480 Mbps | Micro-B / Type-C |
| USB 3.2 Gen1(5Gbps) | 5 Gbps | A / C（蓝色） |
| USB 3.2 Gen2 | 10 Gbps | C |
| USB4 / Thunderbolt 3/4 | 40 Gbps | Type-C（DP Alt、PD 供电） |

## 存储接口
- SATA III：6 Gbps（实际 ~550 MB/s），AHCI 协议。
- NVMe over PCIe：队列深（64K 队列 × 64K 命令），7GB/s+（PCIe4）。

## 内存
- DDR4：2133-3200 MT/s；DDR5：4800-8000 MT/s，双通道带宽翻倍。
- ECC 内存用于服务器，纠正单比特错误。

## 显示与其它
- DP 1.4：32.4Gbps（DSC 可上 4K240/8K60）；HDMI 2.1：48Gbps。
- I2C/SPI/UART 详见嵌入式分类。
$md$,
'硬件底层', '总线与接口（PCIe/USB）', array['PCIe','USB','NVMe','接口'], 'seedling', '原创', 2),

-- ============ 嵌入式 ============
('裸机开发第一站：寄存器、时钟与启动流程', 'emb-bare-metal-register-boot',
 'STM32 从上电到 main() 发生了什么：向量表、时钟树、外设寄存器映射。',
$md$# 裸机开发第一站

## 上电启动流程（Cortex-M）
1. 从向量表偏移 0x00 取**初始 MSP**；偏移 0x04 取 **Reset_Handler** 地址。
2. Reset_Handler（汇编）：拷贝 .data 到 SRAM、.bss 清零、调用 SystemInit 配时钟。
3. 跳转 `__libc_init_array` → `main()`。

## 操作寄存器的标准写法
```c
// 地址 → 指针 → 解引用；volatile 防止编译器优化掉硬件访问
#define GPIOA_MODER (*(volatile uint32_t *)0x48000000U)
GPIOA_MODER = (GPIOA_MODER & ~(3U << 10)) | (1U << 10); // PA5 通用输出
```
HAL 库本质：`GPIOA->MODER` 结构体成员就是这些寄存器宏的封装。

## 时钟树（STM32F1/F4 典型）
- HSE 外部晶振（8MHz）→ PLL 倍频 → SYSCLK（72/168MHz）
- 总线分频：AHB / APB1（低速）/ APB2（高速）
- 外设使用前必须在 RCC 中使能时钟，这是新手最常见"寄存器写了没反应"的原因。

## 调试
- SWD 两线（SWDIO/SWCLK）+ OpenOCD / ST-Link / J-Link。
- 排查三板斧：时钟开了吗？引脚复用对吗？NVIC 中断使能了吗？
$md$,
'嵌入式', 'STM32/ARM Cortex-M', array['STM32','裸机','寄存器','Cortex-M'], 'growing', '实战总结', 3),

('嵌入式通信协议对比：UART / I2C / SPI / CAN', 'emb-comm-protocols-uart-i2c-spi-can',
 '四种最常用板级/总线协议的线序、速率、拓扑与典型使用场景。',
$md$# 通信协议对比

| 协议 | 线数 | 同步 | 双工 | 速率 | 拓扑 | 典型场景 |
|---|---|---|---|---|---|---|
| UART | TX/RX/GND | 异步 | 全双工 | 9600~数 Mbps | 点对点 | 调试串口、GPS 模块 |
| I2C | SDA/SCL | 同步 | 半双工 | 100k/400k/1M/5M | 一主多从(7/10位地址) | 传感器、EEPROM、OLED |
| SPI | MOSI/MISO/SCK/CS | 同步 | 全双工 | 数十 Mbps | 一主多从(片选) | Flash、LCD、高速 ADC |
| CAN | CANH/CANL | 异步 | 半双工 | 125k~5M(距离反比) | 多主仲裁总线 | 汽车、工业总线 |

## 要点
- **UART**：波特率双方约定一致，起始位+数据位+校验+停止位；RS232/RS485 是物理层变种（485 差分可长距离、多机）。
- **I2C**：开漏 + 上拉电阻，靠地址寻址；注意时钟拉伸 stretch、上拉阻值（常见 4.7kΩ）。
- **SPI**：4 种 CPOL/CPHA 模式，接屏前先对数据手册确认模式 0/3。
- **CAN**：非破坏性仲裁（ID 小优先）、CRC、ACK 帧、错误自动重传；CAN FD 速率与帧长升级。

## 工具
- 逻辑分析仪（Kingst/DSLogic）+ 免费软件 sigrok/PulseView 抓 I2C/SPI/UART 解码。
- CAN 用 CANable + candleLight 固件 + Wireshark/savvyCAN。
- 串口报文可用站内 [Base64 编解码](/tools/base64)、[时间戳转换](/tools/timestamp) 辅助解析。
$md$,
'嵌入式', '通信协议（UART/I2C/SPI/CAN）', array['I2C','SPI','UART','CAN'], 'growing', '原创', 3),

('FreeRTOS 核心：任务/调度/IPC', 'emb-freertos-task-scheduler-ipc',
 'FreeRTOS 任务状态机、抢占式调度、信号量/队列/事件组与优先级反转。',
$md$# FreeRTOS 核心

## 任务与调度
- 任务 = 独立栈 + TCB；四种状态：Running / Ready / Blocked / Suspended。
- 抢占式：高优先级就绪立即抢占；同优先级时间片轮转（configUSE_TIME_SLICING）。
- Tick 心跳驱动延时与超时，SysTick 中 xPortSysTickHandler → PendSV 完成切换。

## 常用 API
```c
xTaskCreate(vTask, "name", 256, NULL, 2, &handle);
vTaskDelay(pdMS_TO_TICKS(100));          // 相对延时
vTaskDelayUntil(&last, pdMS_TO_TICKS(10)); // 精确周期
```

## IPC
- **队列 Queue**：任务间传数据（值拷贝），天然线程安全。
- **二值信号量**：中断通知任务（`xSemaphoreGiveFromISR`）。
- **互斥量 Mutex**：带优先级继承，缓解**优先级反转**。
- **计数信号量**：资源池；**事件组**：多条件等待（bit AND/OR）。
- **流缓冲/消息缓冲**：一对一高效字节流。

## 避坑
- 中断里只能用 `FromISR` 系列 API。
- 栈溢出用 `uxTaskGetStackHighWaterMark` 检测。
- 优先级别乱开，优先分析"谁等谁"再决定。
$md$,
'嵌入式', 'RTOS（FreeRTOS/RT-Thread）', array['FreeRTOS','RTOS','实时系统'], 'seedling', '原创', 4),

-- ============ 软件工程 ============
('Linux 命令行肌肉记忆清单', 'sw-linux-cli-muscle-memory',
 '文件/文本/进程/网络/排查五类高频命令，配合管道组合解决 90% 日常问题。',
$md$# Linux 命令行肌肉记忆清单

## 文件与查找
```bash
ls -lah ; du -sh * | sort -h ; df -h
find . -name "*.log" -mtime +7 -delete
lsof -i:8080                 # 谁占用了端口
```

## 文本三剑客
```bash
grep -rn "ERROR" /var/log --include=*.log | awk '{print $4}' | sort | uniq -c | sort -rn
sed -i 's/old/new/g' file
jq '.data[] | select(.id>3)' resp.json    # JSON 处理
```

## 进程与性能
```bash
ps auxf ; top/htop ; pidstat 1
vmstat 1 ; iostat -xz 1 ; free -h
strace -p <pid> -f -e trace=network   # 系统调用追踪
```

## 网络
```bash
ss -tunlp ; ip a ; ip route
curl -v -w '%{time_total}\n' https://example.com
tcpdump -i any -nn 'port 53' ; dig +trace example.com
```

## 急救场景
- 磁盘满：`du -sh /* | sort -h` 逐层定位；journal 日志 `journalctl --vacuum-size=200M`。
- 服务挂：`systemctl status xxx` + `journalctl -u xxx -f`。
- 负载高但 CPU 闲：多半在等 IO（iostat %util、await）。

> 生成的 UUID/时间戳/Base64 可用站内工具：[UUID 生成](/tools/uuid)、[时间戳转换](/tools/timestamp)、[Base64](/tools/base64)。
$md$,
'软件工程', '操作系统原理（Linux）', array['Linux','命令行','排障'], 'mastered', '实战总结', 2),

('数据结构与算法：按面试/实战权重梳理', 'sw-dsa-by-priority',
 '数组/哈希/树/堆/图/DP 的核心复杂度与真实工程用途，不只刷题为用而学。',
$md$# 数据结构与算法：按实战权重

## 必拿（工程每天都在接触）
- 动态数组/链表：ArrayList vs LinkedList 缓存友好性差异。
- 哈希表：O(1) 平均；冲突（链地址/开放寻址）；扩容 rehash；Redis dict 渐进式 rehash。
- 排序：快排平均 O(nlogn) 最坏 O(n²)；TimSort（Python/Java 内置）利用已有序段。

## 高频
- 双指针/滑动窗口：子串子数组问题。
- 栈/单调栈：括号匹配、下一个更大元素；队列/BFS：层序、最短步数。
- 二叉树 → 二叉搜索树 → 平衡树（红黑树，Java TreeMap/ Linux CFS 调度器）。
- 堆：TopK、定时器、优先队列（Go container/heap）。
- Trie：前缀匹配、敏感词、命令补全。

## 进阶
- 图：BFS/DFS、Dijkstra（单源最短路）、并查集（连通性、Kruskal）。
- DP：一维/背包/区间；记忆化 → 递推；识别"最优子结构+重叠子问题"。
- LRU（哈希+双向链表）、LFU；布隆过滤器（缓存穿透防护）。

## 复杂度直觉
- O(n²) 在 n=10⁵ 已不可接受；O(nlogn) 是排序底线；对数复杂度意味着"翻倍数据只多一步"。
$md$,
'软件工程', '数据结构与算法', array['算法','数据结构','面试'], 'growing', '原创', 3),

('微服务架构图鉴：本博客的技术底座', 'sw-microservice-architecture-of-site',
 'Spring Cloud Gateway + Nginx + 多服务 + JWT 的一次完整请求链路梳理。',
$md$# 本站微服务架构

## 组件
- **网关 gateway-service**：统一入口、路由转发、跨域、（鉴权过滤器）。
- **业务服务**：user / blog / forum / shop / media / tool / quant / ai-proxy。
- **common-security**：JwtUtil、AuthUtils 统一鉴权工具。
- 前端 Vue3 SPA → Nginx 静态托管 + /api 反代到网关。

## 一次登录请求链路
1. POST /api/auth/login → 网关路由 user-service。
2. 校验密码（BCrypt）→ JwtUtil 生成 token（claims: userId/username/authorities/nickname）。
3. 前端存 localStorage，axios 拦截器自动加 `Authorization: Bearer`。
4. 后续请求网关/服务用 AuthUtils.requireUserId 解析。

## 服务间要点
- 无状态化：session 不存服务端，JWT 自包含。
- 配置与发现：Nacos（如已启用）管理配置与注册。
- 容错：超时、重试要防雪崩；Resilience4j 熔断隔离。
- 可观测：traceId 贯穿日志（MDC），Prometheus + Grafana 监控。

## 对应学习
微服务不是银弹：先单体、按边界（DDD 限界上下文）拆分；详见 [架构图鉴](/architecture)。
$md$,
'软件工程', '分布式与微服务', array['微服务','SpringCloud','JWT','架构'], 'growing', '实战总结', 3),

('Git 工作流与救命命令', 'sw-git-workflow-survival',
 '分支模型、rebase 取舍、误操作恢复 reflog，以及提交信息规范。',
$md$# Git 工作流与救命命令

## 日常流
```bash
git switch -c feat/login      # 新分支
git add -p                    # 分块暂存，保持提交原子性
git commit -m "feat(auth): 添加登录"
git fetch origin && git rebase origin/main   # 同步主干，保持线性
```

## rebase vs merge
- 合并公共分支用 merge（不改写历史）。
- 同步自己未推送的特性分支用 rebase（历史线性清爽）。
- 铁律：**已推送给别人的提交不要 rebase/force push**。

## 救命
```bash
git reflog                   # HEAD 移动历史，找回"丢失"提交
git reset --hard <hash>      # 工作区回到指定提交
git restore --staged f       # 取消暂存
git cherry-pick <hash>       # 拣某个提交到当前分支
git revert <hash>            # 用新提交撤销（安全，用于公共分支）
```

## Conventional Commits
feat / fix / docs / style / refactor / perf / test / chore。

## 小技巧
- `git stash -u` 连未跟踪文件一起暂存。
- `git log --oneline --graph --all` 看分支图。
- diff 结果可以贴进站内 [文本对比](/tools/diff) 做可视化对照。
$md$,
'软件工程', '后端工程与 API 设计', array['Git','工作流','rebase'], 'mastered', '原创', 2),

('Docker 到 docker compose：把环境装进集装箱', 'sw-docker-compose-in-practice',
 '镜像/容器/卷/网络核心概念 + 生产可用的 compose 模板（含 RustDesk 服务器）。',
$md$# Docker 实战

## 心智模型
- **镜像 Image**：只读模板（分层 + UnionFS）；**容器 Container**：镜像的可写运行实例。
- **卷 Volume**：绕过联合文件系统持久化数据；**网络**：bridge/host/自定义网络，服务名即 DNS。

## 常用命令
```bash
docker ps -a ; docker logs -f <c> ; docker exec -it <c> sh
docker system prune -af --volumes   # 大扫除（谨慎）
```

## compose 模板（RustDesk 自建服务器示例）
```yaml
services:
  hbbs:
    image: rustdesk/rustdesk-server:latest
    container_name: hbbs
    network_mode: host
    volumes: ["./hbbs-data:/root"]
    command: hbbs -k _
    restart: unless-stopped
  hbbr:
    image: rustdesk/rustdesk-server:latest
    container_name: hbbr
    network_mode: host
    volumes: ["./hbbr-data:/root"]
    command: hbbr -k _
    restart: unless-stopped
```
更完整的免费远程组网方案见博客《免费远程完全指南：RustDesk + Tailscale》（/blog?category=运维与效率）。

## 优化
- 多阶段构建减小镜像；`.dockerignore` 排除 node_modules。
- 固定镜像版本号而非 latest（生产）。
$md$,
'软件工程', 'Docker/K8s 与云原生', array['Docker','compose','部署'], 'growing', '实战总结', 2),

-- ============ 网络工程 ============
('TCP 三次握手/四次挥手与状态机', 'net-tcp-handshake-state-machine',
 '为什么是三次不是两次、TIME_WAIT 的意义、抓包验证每一个标志位。',
$md$# TCP 连接管理

## 三次握手
1. C → S：SYN, seq=x，C 进入 SYN_SENT。
2. S → C：SYN+ACK, seq=y, ack=x+1，S 进入 SYN_RCVD。
3. C → S：ACK, ack=y+1，双方 ESTABLISHED。

**为什么三次**：防止历史失效的 SYN 突然到达让服务端白开连接，并让双方确认"我的发/收和对方的发/收"通道都通。

## 四次挥手
FIN_WAIT_1 → FIN_WAIT_2 / CLOSE_WAIT → LAST_ACK → **TIME_WAIT(2MSL)** → CLOSED。
- **TIME_WAIT 为什么存在**：①保证最后 ACK 能到达（丢了可重发 FIN）；②让旧连接的迷途报文在网络中消亡，避免污染新复用连接。
- 大量 TIME_WAIT 压测时可调 `net.ipv4.tcp_tw_reuse=1`。

## 关键机制
- 可靠传输：序列号 + 累积确认 + 超时重传。
- 流量控制：滑动窗口 rwnd；拥塞控制：慢启动/拥塞避免/快重传/快恢复 cwnd。
- MSS/MTU、Nagle vs delayed ACK 互动、keepalive。

## 抓包验证
```bash
sudo tcpdump -i any -nn 'tcp port 8080 and tcp[tcpflags] & (tcp-syn|tcp-fin) != 0'
# 或 Wireshark 过滤器：tcp.flags.syn==1 || tcp.flags.fin==1
```
配合站内 [60秒API广场](/tools/api-plaza) 造请求观察。
$md$,
'网络工程', 'OSI/TCP-IP 模型', array['TCP','握手','抓包','Wireshark'], 'mastered', '原创', 3),

('HTTPS/TLS 握手全流程与证书体系', 'net-https-tls-handshake-certificates',
 '对称/非对称/哈希分工、TLS1.3 一轮握手、CA 信任链与常见证书报错排查。',
$md$# HTTPS / TLS

## 三类密码学原语分工
- **非对称**（RSA/ECDH）：协商密钥、身份认证（慢）。
- **对称**（AES-GCM/ChaCha20）：加密大量数据（快）。
- **哈希/MAC**（SHA256/HMAC）：完整性。

## TLS 1.2 握手（简化）
ClientHello(支持套件/Random) → ServerHello(选套件/Random/证书) →
客户端验证证书链 → 生成 PreMaster（RSA）或 ECDHE 公钥互换 →
双方推导会话密钥 → Finished 互验 → 应用数据。

**TLS 1.3**：合并往返，1-RTT（会话恢复 0-RTT），废除 RSA 密钥交换，只保留前向保密（ECDHE）套件。

## 证书信任链
根 CA（系统/浏览器信任库）→ 中间 CA → 站点证书；每一级用上一级公钥验签。
- 单域名 / 泛域名 *.a.com / SAN 多域名。
- Let's Encrypt 免费 90 天，certbot/acme.sh 自动续期。

## 常见报错排查
- NET::ERR_CERT_DATE_INVALID：过期/本机时间错。
- hostname mismatch：证书域名不含当前域名。
- self-signed：自签未被信任（内网加根证书或 mkcert）。
- 混合内容：HTTPS 页引用 HTTP 资源被拦。

```bash
openssl s_client -connect example.com:443 -servername example.com | openssl x509 -noout -dates -subject
```
$md$,
'网络工程', 'HTTP/HTTPS 与 DNS/TLS', array['TLS','HTTPS','证书','OpenSSL'], 'growing', '官方文档', 3),

('HTTP 缓存、跨域与状态码实战', 'net-http-cache-cors-status',
 '强缓存/协商缓存头、CORS 预检机制、常见状态码与排障路径。',
$md$# HTTP 实战要点

## 缓存
- **强缓存**：`Cache-Control: max-age=31536000, immutable`（命中不请求，200 from disk cache）。
- **协商缓存**：`ETag/If-None-Match`（内容指纹优先）、`Last-Modified/If-Modified-Since`（秒级精度）→ 304。
- SPA 策略：HTML 不缓存（no-cache），带 hash 的 js/css 永久强缓存。

## CORS
- 简单请求直接发，带 `Origin`，服务端回 `Access-Control-Allow-Origin`。
- 非简单（PUT/DELETE、自定义头、application/json）先 **OPTIONS 预检**。
- 带 Cookie 必须 `Allow-Credentials: true` 且 ACAO 不能是 *，要回具体源。

## 状态码
- 301 永久 / 302 临时 / 307/308 保持方法重定向。
- 400 参数 / 401 未认证 / 403 无权限 / 404 / 409 冲突 / 429 限流。
- 502 网关后端挂 / 503 不可用 / 504 网关超时。

## 排障
```bash
curl -I https://x.com/a.js          # 只看响应头
curl -X OPTIONS ... -H "Origin: http://localhost:5173" -v
```
URL 参数乱码用站内 [URL 编解码](/tools/url) 验证。
$md$,
'网络工程', 'HTTP/HTTPS 与 DNS/TLS', array['HTTP','CORS','缓存'], 'mastered', '实战总结', 2),

-- ============ 网络安全 ============
('OWASP Top 10 与最小验证 PoC', 'sec-owasp-top10-poc-notes',
 '注入/失效访问控制/XSS/SSRF 等十大风险的原理、验证方式与修复要点（本地靶场环境）。',
$md$# OWASP Top 10（2021 版核心）

> 仅在**本地靶场**（DVWA、Pikachu、sqli-labs）或获书面授权目标上验证。

1. **失效的访问控制**：越权（IDOR，改 ?id=1001 看他人订单）→ 服务端鉴权，不靠隐藏菜单。
2. **加密失败**：明文 HTTP、弱哈希（MD5 存密码）→ TLS + BCrypt/Argon2。
3. **注入**：SQL 注入（' OR 1=1 -- ）、命令注入；**参数化查询/预编译**是根治。
4. **不安全设计**：业务逻辑层滥用（优惠券重复领）。
5. **安全配置错误**：默认口令、目录列举、报错堆栈外显。
6. **易受攻击的组件**：依赖 CVE（Log4Shell）→ SCA 扫描 + 及时升级。
7. **身份认证失败**：弱密码、无锁定、JWT 不过期 → MFA、限流。
8. **数据完整性失败**：CI/CD 管道、反序列化漏洞。
9. **日志与监控失败**：攻击无告警。
10. **SSRF**：服务端按用户 URL 发起请求，打 `http://127.0.0.1`、云元数据 `169.254.169.254` → URL 白名单、禁内网段、禁重定向跟随。

## XSS 三种
- 反射型（参数直接回显）、存储型（留言入库）、DOM 型（前端 innerHTML 注入）。
- 防御：输出编码（上下文相关）、CSP、HttpOnly Cookie、富文本用 DOMPurify。

## CSRF
利用已登录 Cookie 发跨站请求；SameSite=Lax/Strict、Anti-CSRF Token、校验 Origin。

> 测试 payload 编码可用 [Base64 工具](/tools/base64)，判断文件指纹可用 [Hash 生成](/tools/hash)。
$md$,
'网络安全', 'Web 安全（OWASP Top 10/SQL注入/XSS/SSRF）', array['OWASP','XSS','SQL注入','Web安全'], 'growing', '实战总结', 4),

('渗透测试标准流程 Checklist', 'sec-pentest-process-checklist',
 'PTES 思路：授权→信息收集→漏洞探测→利用→后渗透→报告，附常用工具链。',
$md$# 渗透测试流程（授权前提下）

## 0. 授权与边界
书面授权书（scope、时间窗、禁止项：DoS/社工/拖库），全程留痕。

## 1. 信息收集（PTES）
- 域名：子域名爆破（OneForAll/subfinder）、ICP/备案、证书透明日志 crt.sh。
- 资产：nmap 端口服务指纹、fofa/hunter 空间测绘、Wappalyzer 技术栈。
- 人员：企业邮箱命名规则 → 钓鱼面评估（不做实际钓鱼除非授权）。

## 2. 漏洞探测
- Web：Burp Suite 抓包、目录扫描（ffuf）、SQL 注入 sqlmap（授权！）、XSS。
- 服务：弱口令（hydra，谨慎）、已知 CVC-E 版本比对。
- 逻辑：越权、支付篡改、验证码复用。

## 3. 利用与后渗透
- getshell → 提权（内核 exploit/sudo 错配/SUID/计划任务）。
- 横向：网段探测、凭证收集（mimikatz 仅限授权内网演练）。

## 4. 报告
漏洞名称/等级（CVSS）/复现步骤/影响/修复建议，复测闭环。

## 工具链（均为合法安全测试用途）
Kali、Burp Suite Community、nmap、metasploit（框架学习）、Wireshark、ffuf、nuclei（模板化漏扫）。
$md$,
'网络安全', '渗透测试流程（信息收集/漏洞利用/提权）', array['渗透测试','BurpSuite','nmap','信息收集'], 'seedling', '原创', 4),

('密码学工程应用：哈希/对称/签名怎么选', 'sec-crypto-engineering-selection',
 '不推导数学公式，讲工程上何时用 AES、何时用 RSA/ECDSA、盐与 HMAC 的区别。',
$md$# 密码学工程选择

| 需求 | 选择 | 备注 |
|---|---|---|
| 密码存储 | Argon2id / BCrypt | 慢哈希+随机盐；绝不能 MD5/SHA1 |
| 文件校验 | SHA-256 | 站内设 [Hash 生成](/tools/hash) 可算 MD5/SHA（非安全用途） |
| 传输加密 | TLS（AES-GCM/ChaCha20-Poly1305） | 不要自己发明协议 |
| 接口防篡改 | HMAC-SHA256 | 共享密钥+时间戳+nonce 防重放 |
| 数字签名/身份 | ECDSA P-256 / Ed25519 | RSA-2048 起步，推荐椭圆曲线 |
| 密钥交换 | X25519/ECDHE | 前向保密 |

## 高频概念
- **编码 ≠ 加密**：Base64 只是编码，任何人可还原（[Base64 工具](/tools/base64)）。
- **对称加密**一把密钥；**非对称**公私钥对，公钥加密只有私钥解。
- **盐**防彩虹表：每用户独立随机盐，与哈希一起存。
- **JWT**：Header.Payload.Signature，Payload 仅 Base64URL 不保密，敏感信息别放；密钥要够长、定期轮换。
- 随机数：安全场景必须 CSPRNG（`crypto.getRandomValues` / `SecureRandom`），`Math.random()` 不安全。

## 验证证书/密钥
```bash
echo -n data | openssl dgst -sha256
openssl rsa -in key.pem -check ; openssl ec -in key.pem -text -noout
```
$md$,
'网络安全', '安全基础（CIA/加密与证书）', array['密码学','哈希','JWT','AES'], 'mastered', '实战总结', 3),

-- ============ 运维与效率 ============
('Tailscale 组网笔记：设备互联与子网路由', 'ops-tailscale-subnet-exit-node',
 'MagicDNS/ACL/子网路由/exit node 配置，把家里内网安全暴露给自己。',
$md$# Tailscale 笔记

## 核心
- 基于 WireGuard（内核级 UDP 加密隧道），协调服务器只做密钥分发与 NAT 打洞，不经过中转流量（打洞失败走 DERP 中继）。
- 每台设备分配 100.x.x.x CGNAT 地址 + MagicDNS 名（`pc.tailxxxx.ts.net`）。

## 常用操作
```bash
tailscale up                       # 登录加入 tailnet
tailscale status                   # 设备与连接方式（直接/DERP）
tailscale ip -4
tailscale ping <peer>              # 验证是否 P2P 直连
```

## 子网路由（一台设备代理整个物理内网）
```bash
# Linux 网关开启 IP 转发
echo 'net.ipv4.ip_forward=1' | sudo tee -a /etc/sysctl.conf && sudo sysctl -p
sudo tailscale up --advertise-routes=192.168.1.0/24
# 管理后台 Machines → 编辑路由设置 → Approve
# 其他设备：tailscale up --accept-routes
```

## Exit Node（全局流量走家里/云服务器出口）
```bash
sudo tailscale up --advertise-exit-node   # 后台批准
tailscale up --exit-node=<gateway-ip>
```

## 安全
ACL（管理后台 JSON）限制哪些用户能访问哪些端口，最小权限；
开启设备授权（Device approval）、MFA。

## 组合
配合 RustDesk 远程桌面：被控端装 Tailscale，RustDesk ID 服务器可不用公网 IP，见博客《免费远程完全指南》。
$md$,
'运维与效率', 'VPN 与零信任组网（Tailscale/WireGuard）', array['Tailscale','WireGuard','内网穿透'], 'mastered', '实战总结', 3),

('Nginx 反向代理与 HTTPS 配置模板', 'ops-nginx-reverse-proxy-template',
 'SPA 托管 + /api 反代 + WebSocket + 证书续期的生产模板。',
$md$# Nginx 配置模板

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    root /var/www/web3-blog/dist;
    # SPA history 路由回退
    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache";   # HTML 不缓存
    }
    # 带 hash 的静态资源长缓存
    location /assets/ {
        expires 1y; add_header Cache-Control "public, immutable";
    }

    # API 反代到网关
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket（如群聊 /chat/ws）
    location /chat/ {
        proxy_pass http://127.0.0.1:8083;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

    # gzip
    gzip on; gzip_types text/css application/javascript application/json image/svg+xml;
}
server { listen 80; server_name example.com; return 301 https://$host$request_uri; }
```

## 证书自动续期
```bash
sudo certbot --nginx -d example.com
sudo systemctl list-timers | grep certbot   # 确认续期 timer
```
$md$,
'运维与效率', 'Nginx 与反向代理', array['Nginx','HTTPS','WebSocket','部署'], 'mastered', '实战总结', 3),

-- ============ 前沿技术 ============
('Transformer 与大模型应用核心概念', 'ai-transformer-llm-concepts',
 '自注意力直觉、Token/上下文、温度、RAG 与 Agent 工具调用，应用开发者视角。',
$md$# 大模型应用核心概念

## Transformer 直觉
- Self-Attention：序列中每个 token 与所有 token 算相关性权重（Q·Kᵀ/√dk → softmax → 加权 V）。
- 多头：不同子空间并行关注不同关系；位置编码补充顺序信息。
- 训练：next-token 预测；对齐：SFT + RLHF/DPO。

## 应用开发关键参数
- **Token**：分词后的最小单位（中文常 1-2 token/字）；上下文窗口 = 输入+输出预算（32K/128K/1M）。
- **temperature**：低=确定（代码/分类），高=发散（创作）。
- **top_p / frequency_penalty**：核采样与重复惩罚。
- 结构化输出：JSON mode / function/tool calling / 专用评估模型（如 TypeSafe AI 的 Jev，见博客介绍）。

## RAG（检索增强）
文档切分（chunk）→ embedding → 向量库 → 检索 TopK → 拼进 prompt → 带引用回答。
解决知识时效与幻觉；关键在切分策略与重排（rerank）。

## Agent
LLM 做"大脑"做规划+工具调用（搜索、代码执行、数据库），ReAct 循环：Thought→Action→Observation。
本站 [Agent 会话台](/agent) 即此类实践。

## 成本直觉
按 token 计价；prompt 缓存、小模型分流、批处理是降本三板斧。
$md$,
'前沿技术', '大模型原理（Transformer/RAG/Agent）', array['大模型','Transformer','RAG','Agent'], 'growing', '官方文档', 3),

('Vercel AI SDK 与 AI Gateway 上手', 'ai-vercel-ai-sdk-gateway',
 '统一多家模型的调用方式：generateText/streamText/evaluate，一个 key 走网关。',
$md$# Vercel AI SDK / AI Gateway

## 解决什么问题
模型厂商 API 各不相同；AI Gateway 提供统一端点、密钥治理、用量统计、故障自动路由；AI SDK（npm `ai`）把调用抽象为同一套函数。

## 快速示例
```js
import { generateText } from 'ai';
// 环境变量配置网关 baseURL 与 key
const result = await generateText({
  model: gateway('openai/gpt-4o-mini'),
  prompt: '用一句话解释 TCP 三次握手',
});
console.log(result.text);
```
流式：`streamText` 返回 ReadableStream，适合打字机效果（本站星途助手同款体验）。

## 结构化评估：Jev
```js
import { experimental_evaluate as evaluate } from 'ai';
const result = await evaluate({
  model: 'typesafe-ai/jev',
  state: '客服已为客户办理全额退款',
  questions: { refunded: { type: 'boolean', instructions: '是否发生了退款?' } },
});
```
Jev 是 TypeSafe AI 的 System One 评估模型，返回选项/评分/布尔概率，适合分类、路由、评分卡与自动校验，网关内免费、32K 上下文。详细介绍见博客《Jev 模型体验分享》。

## 工程建议
- 网关侧做 provider 故障转移（主用一家、备用一家）。
- 结构化任务优先用支持 schema 的模型/评估模型，比让通用模型"尽量输出 JSON"稳得多。
$md$,
'前沿技术', 'Vercel AI SDK / AI Gateway', array['Vercel','AI Gateway','Jev','AI SDK'], 'growing', '官方文档', 2)

on conflict (slug) do update set
  title = excluded.title, summary = excluded.summary, content = excluded.content,
  category = excluded.category, subcategory = excluded.subcategory, tags = excluded.tags,
  stage = excluded.stage, source = excluded.source, difficulty = excluded.difficulty,
  updated_at = now();

-- ============================================================
-- 3. 博客文章种子（7 篇详细文章；幂等：slug 唯一冲突则跳过）
--    tags 为 text[]；is_top/published 为 boolean
-- ============================================================
-- 若 blog_articles 尚无 slug 唯一索引则补一个（已有重复 slug 时请先清理）
create unique index if not exists blog_articles_slug_key
  on public.blog_articles(slug) where slug is not null;

-- ---------- 文章 1：RustDesk + Tailscale 免费远程完全指南 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '免费远程完全指南：RustDesk + Tailscale 零成本组网实战（2026）',
  'free-remote-rustdesk-tailscale-guide',
  '不花一分钱、不被商业软件限速、数据全程加密：用 Tailscale 把所有设备组成私有内网，再用 RustDesk 远程桌面。本文覆盖原理、自建服务器、Windows/Linux/macOS/安卓全平台、排障与安全加固。',
$md$> 关键词：**免费、不限速、开源、端到端加密、内网穿透、自建中继**
>
> 一句话方案：**Tailscale 负责"把设备安全地放进同一个虚拟局域网"，RustDesk 负责"流畅的远程桌面体验"**。两者叠加 = 免费版"TeamViewer + 商业 SD-WAN"。

## 一、为什么要折腾免费远程方案

商业远程软件（向日葵、ToDesk、TeamViewer、AnyDesk）用起来方便，但常见痛点：

| 痛点 | 具体表现 |
|---|---|
| 限速 | 免费用户高峰时段画面模糊、延迟抖动 |
| 隐私 | 屏幕流经第三方服务器，运维/办公场景有合规风险 |
| 商业判定 | TeamViewer 很容易把个人使用判成商业用途 |
| 设备数/流量限制 | 免费档设备数量、月流量有天花板 |
| 广告与捆绑 | 国产软件安装时全家桶防不胜防 |

我们的目标：

1. **零成本**（除了一台最便宜的云服务器，甚至可以完全不要）；
2. **不限设备数量、不限流量**；
3. **密钥掌握在自己手里**，信不过任何中间厂商；
4. **全平台**：Windows、Linux、macOS、iOS/安卓都能连。

## 二、先建立心智模型：两个软件各干什么

```
  你的笔记本（外网/咖啡厅）
        │  WireGuard 加密隧道（Tailscale 自动打洞，P2P 直连）
        ▼
  家里的台式机 / 公司的 Linux 服务器（Tailscale 虚拟 IP：100.x.x.x）
        │
        └── RustDesk 客户端（监听在 Tailscale 虚拟网卡上）
            远程桌面、文件传输、剪贴板、语音
```

- **Tailscale**：基于 WireGuard 的零配置组网工具。它在你的每台设备上创建一张虚拟网卡，设备之间像在同一个局域网；NAT 穿透自动完成，打通失败时走免费的 DERP 中继。开源的协调服务器替代方案是 Headscale。
- **RustDesk**：开源远程桌面（TeamViewer 的替代品），Rust + Flutter 写的，自带 ID 服务器（hbbs）和中继服务器（hbbr），可以完全自建，也支持**直接填 IP 直连**。

> 💡 为什么不直接用 Tailscale + 系统自带远程桌面？可以：Windows 用 RDP、Linux 用 VNC/SSH、macOS 用屏幕共享。RustDesk 的价值是**跨平台体验统一 + 文件传输 + 不用单独开 RDP 端口**。

## 三、Tailscale 安装与组网（10 分钟）

### 3.1 注册与安装

用 GitHub/Google/微软账号在 tailscale.com 注册（免费档 100 台设备，个人绰绰有余）。

- Windows/macOS：官网下载安装包，登录即可。
- Linux：一行命令
```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
# 输出一个登录 URL，浏览器打开授权
tailscale status      # 查看设备列表与虚拟 IP
```
- 安卓/iOS：应用商店搜 Tailscale，登录同一账号。
- 群晖/威联通 NAS：套件中心或 Tailscale 官方 spk。

### 3.2 用 MagicDNS 记不住 IP 也没关系

管理后台开启 **MagicDNS** 后，每台设备有固定域名：`主机名.xxx.ts.net`。之后无论它在哪个网络，`ssh user@nas.xxx.ts.net` 永远可达。

### 3.3 验证 P2P 直连

```bash
tailscale ping home-pc
# 显示 "via DERP" 是中继兜底（能连但慢）
# 显示 "direct ... <真实IP:端口>" 就是 P2P 直连，延迟最低
```

大部分家宽 NAT 都能自动打洞成功；对称型 NAT 打不通时才走 DERP（免费但带宽共享）。

## 四、RustDesk：两种用法

### 用法 A：纯客户端直连（配合 Tailscale，最推荐，零服务器成本）

1. 家里被控端和外出的主控端都装好 RustDesk + Tailscale 并登录同一 Tailnet。
2. 被控端打开 RustDesk，设置 → 网络 → ID/中继服务器**不用填**，直接在主控端"连接 ID"框里填：
   - 被控端的 Tailscale IP（`100.x.x.x`）或 MagicDNS 域名；
   - RustDesk 默认直连端口 **21118**，确保被控端防火墙放行该端口（仅 Tailscale 网卡可达，不暴露公网）。
3. 被控端设置**固定的永久密码**（安全 → 启用密码）。

```
主控端 RustDesk 连接地址：100.88.12.34:21118
                       （即被控端 Tailscale IP + 默认端口）
```

✅ 优点：不依赖任何 RustDesk 公网服务器，流量经 Tailscale/WireGuard 端到端加密，**免费、无限速**。

### 用法 B：完全自建 ID + 中继服务器（给家人/团队用）

适合不想依赖 Tailscale 账号、或者要给客户提供远程协助的场景。需要一台有公网 IP 的云服务器（1C1G 即可，只在打洞失败时才消耗中继带宽）。

**Docker Compose 一键部署**（已安装 Docker）：

```yaml
# docker-compose.yml
services:
  hbbs:
    image: rustdesk/rustdesk-server:latest
    container_name: hbbs
    network_mode: host
    volumes:
      - ./hbbs-data:/root
    command: hbbs -k _
    restart: unless-stopped
  hbbr:
    image: rustdesk/rustdesk-server:latest
    container_name: hbbr
    network_mode: host
    volumes:
      - ./hbbr-data:/root
    command: hbbr -k _
    restart: unless-stopped
```

```bash
docker compose up -d
docker logs hbbs   # 首次启动会生成公钥 id_ed25519.pub，记下来
```

**云服务器安全组放行端口**：

| 端口 | 用途 |
|---|---|
| 21115-21119 TCP | 21115 NAT 测试、21116 ID(TCP)、21117 中继、21118 WebSocket 直连、21119 WebSocket |
| 21116 UDP | ID 注册/心跳 |

**客户端配置**：所有 RustDesk 客户端 → 设置 → 网络 → ID/中继服务器填 `你的域名或IP`，Key 填服务器生成的公钥（**Key 不为空才是端到端加密**，`-k _` 表示强制加密）。

### 4.x 平台注意事项

- **Windows 被控**：安装时勾选"以服务方式安装"，开机自启、锁屏也能连；电源选项关掉"睡眠"。
- **Linux 被控**：Wayland 下支持不完整，登录界面选 **X11 (Xorg)** 会话；或安装 headless：
```bash
sudo apt install ./rustdesk-*.deb
sudo systemctl enable --now rustdesk
```
- **macOS**：被控需要系统设置里授予"屏幕录制"和"辅助功能"权限。
- **手机主控**：安卓/iOS 客户端适合应急操作，触屏有手势板模式；手机被控仅安卓 Root/部分厂商支持。

## 五、进阶玩法

### 5.1 子网路由：一台设备代理全家内网

不想给每个设备都装 Tailscale？在家里常开的一台 Linux/软路由上：

```bash
echo 'net.ipv4.ip_forward=1' | sudo tee -a /etc/sysctl.conf && sudo sysctl -p
sudo tailscale up --advertise-routes=192.168.1.0/24
```
到 Tailscale 后台 Approve 该路由，你在外面就能用 `192.168.1.x` 直接访问家里的路由器管理页、NAS、打印机——**配合内网版 [二维码工具](/tools/qrcode)、路由器后台都像在本地一样**。

### 5.2 Exit Node：把家里宽带变成自己的全球出口

```bash
# 家里机器广播 exit node，后台 Approve
sudo tailscale up --advertise-exit-node
# 外出设备一键把全部流量走家里 IP
tailscale up --exit-node=100.x.x.x
```
在外面连公共 Wi-Fi 时，流量全程加密回家再出网，天然 VPN。

### 5.3 SSH 运维场景

Tailscale + SSH 比 RustDesk 更轻量：`ssh user@pc.xxx.ts.net` 直接维护 Linux 服务器。再配合 Tailscale SSH（自动签发短期证书），连密码和公钥分发都省了。

## 六、安全加固清单

1. Tailscale 后台开启**设备审批**和**MFA**；用 **ACL** 限制"只有我的笔记本能访问 3389/21118 端口"。
2. RustDesk 被控端设置**强永久密码**；自建服务器务必填 **Key（强制加密）**。
3. RustDesk 直连只绑 Tailscale 网卡（Windows 防火墙入站规则作用域改为 100.64.0.0/10）。
4. 云服务器只开放必要端口，`fail2ban` 防爆破，定期 `docker pull` 更新镜像。
5. 不要把 ID+密码截图发到公开群；RustDesk 支持一次性临时密码。

## 七、排障速查

| 现象 | 排查 |
|---|---|
| Tailscale 在线但 ping 不通 | 系统防火墙拦了虚拟网卡；`tailscale netcheck` 看 NAT 类型 |
| 一直 DERP 中继、卡顿 | 路由器开 UPnP/给出口 UDP 权限；公司网络禁 UDP 时尝试 41641 端口或 443 |
| RustDesk 连接超时 | 被控 21118 端口、是否在 Tailscale IP 上监听（`ss -tunlp`） |
| Linux 黑屏 | Wayland → X11；无显示器的主机插 HDMI 诱骗器或用虚拟显示器 |
| 自建服务器不在线 | 安全组 21116/UDP 没开；客户端 Key 没填 |

---

**总结**：个人用 → **Tailscale 组网 + RustDesk 填 Tailscale IP 直连**，0 元、不限速、端到端加密；给家人或团队用 → 再加一台 1C1G 云服务器自建 hbbs/hbbr。这套组合我自己日常维护家里 Windows 主机、NAS 和云服务器全靠它，延迟和流畅度明显优于免费版商业软件。

> 相关阅读：部署过程中常用的 [Nginx 反代](/blog?category=运维与效率)、[Base64](/tools/base64) 等工具都在站内工具站，配置文件贴来 [文本对比](/tools/diff) 也很方便。
$md$,
'运维与效率', array['RustDesk', 'Tailscale', '远程桌面', '内网穿透', 'WireGuard', '免费软件'],
'极光星途', 0, 0, true, true, now()
on conflict do nothing;

-- ---------- 文章 2：Jev 模型分享 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select 'Jev：Vercel AI Gateway 上免费的结构化决策评估模型体验',
  'jev-typesafe-ai-vercel-ai-gateway',
  'Jev 是 TypeSafe AI 推出的 System One 评估模型，专为软件中的快速结构化决策设计：给它一段状态和一组类型化问题，返回布尔概率、选项与评分，适合分类、路由、评分卡与自动化校验。Vercel AI Gateway 内免费，32K 上下文。',
$md$> 模型主页：[vercel.com/ai-gateway/models/jev](https://vercel.com/ai-gateway/models/jev)
> 提供方：TypeSafe AI　｜　价格：**免费**（32K 上下文，输入输出均免费）｜　发布：2026-09-15

## 一、Jev 是什么，和普通聊天模型有什么不同

普通大模型擅长"生成文本"，但工程上我们经常需要的是**确定性的判断**：

- 这条客服对话最终**有没有**完成退款？（布尔）
- 用户的工单应该路由到**哪个部门**：账单 / 技术 / 投诉？（分类选择）
- 这篇回答按评分卡能打几分、是否通过验收？（评分 + 布尔）
- 批量 100 条用户反馈，自动判断情感极性与是否含投诉（并行评估）

让通用聊天模型"请严格输出 JSON"总会遇到格式漂移、多余解释、漏字段。**Jev 是 TypeSafe AI 的 System One 评估模型（evaluation model）**，输入"共享状态（shared state）+ 类型化问题（typed questions）"，直接返回 **choices（选项）、scores（评分）和 boolean probabilities（布尔概率）**，并支持**单次请求并行评估多个问题**。

## 二、最小上手代码

使用 Vercel AI SDK 的 `experimental_evaluate` 接口：

```js
import { experimental_evaluate as evaluate } from 'ai';

const result = await evaluate({
  model: 'typesafe-ai/jev',          // AI Gateway 中的模型 slug
  state: '客服为客户办理了全额退款，并发送了退款确认邮件。',
  questions: {
    refunded: {
      type: 'boolean',
      instructions: '对话中是否实际发生了退款？',
    },
  },
});

console.log(result.questions.refunded);
// { value: true, probability: 0.98, ... }
```

多问题并行的例子——一次调用同时做意图分类 + 风险评分 + 结论布尔：

```js
const r = await evaluate({
  model: 'typesafe-ai/jev',
  state: userMessage,
  questions: {
    intent: {
      type: 'choice',
      instructions: '判断用户意图',
      choices: ['账单问题', '技术故障', '投诉', '咨询'],
    },
    urgency: {
      type: 'score',
      instructions: '紧急程度，1 最低 5 最高',
      min: 1, max: 5,
    },
    needsHuman: {
      type: 'boolean',
      instructions: '是否必须立即转人工？',
    },
  },
});
```

## 三、怎么开通

1. 登录 Vercel → AI Gateway → Quick Start 创建 API Key；
2. 网关地址 + Key 配到 AI SDK 的 provider 配置（环境变量，不要写进前端代码）；
3. 模型名固定填 **`typesafe-ai/jev`**。

可以在 [Vercel AI Gateway 文档](https://vercel.com/docs/ai-gateway) 查看各运行时（Node/Next.js 等）的 provider 接入方式。

## 四、典型应用场景

| 场景 | 用法 |
|---|---|
| 工单/消息路由 | choice 问题输出部门，替代关键词规则 |
| 内容审核 | boolean 判断违规 + score 置信度，低置信度转人工 |
| RAG 答案验收 | 对 RAG 生成结果打分：是否引用了上下文、有无幻觉，不通过自动重试 |
| 客服质检 | 批量并行 boolean：是否道歉、是否解决、是否承诺时效 |
| Agent 终止条件 | Agent 循环中判断"任务是否完成"，作为停机闸门 |
| A/B 文案评分 | rubric 多维度打分做自动化偏好评估 |

## 五、我的实战感受

1. **概率值比硬布尔更有用**：`probability: 0.62` 这种灰度结果正好进"人工复核队列"，比通用模型自信地胡说安心很多。
2. **并行问题一次出结果**：省掉多次往返，延迟和 token 成本都更友好。
3. **免费 + 32K 上下文**：做对话质检这种需要长状态的任务很舒服（注意页面标注的促销价时间窗口为 2026-09-25 前，之后以官方定价页为准）。
4. 它不是聊天模型，**不要拿它做开放式生成**；定位是"流水线里的结构化判断器"，和通用模型配合使用：通用模型负责写，Jev 负责判。

> 在本站 [AI 中转站](/ai-station) 可以管理自己的模型令牌；做这类网关应用时生成的调试数据可以丢进 [JSON 格式化](/tools/json) 里看结构。

## 六、参考链接

- 模型页：[vercel.com/ai-gateway/models/jev](https://vercel.com/ai-gateway/models/jev)
- AI Gateway 文档：[vercel.com/docs/ai-gateway](https://vercel.com/docs/ai-gateway)
- TypeSafe AI 条款：[typesafe.ai/legal/terms](https://typesafe.ai/legal/terms)
$md$,
'前沿AI', array['Jev', 'Vercel', 'AI Gateway', 'TypeSafe AI', '大模型', '结构化输出'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;


-- ---------- 文章 3：计算机底层硬件全景 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '计算机底层硬件知识体系全景图：从晶体管到整机（学习路线+书单+实操）',
  'computer-hardware-knowledge-map',
  '硬件不是装机单，而是程序员理解性能与程序行为的根。本文按数字电路→组成原理→CPU 微架构→存储→总线接口→GPU→固件的顺序给出完整知识地图、必懂概念、经典教材和动手实验。',
$md$> 配套实践：本文每个模块都在我的[知识库](/knowledge)里有持续更新的笔记条目。

## 学习路线总览

```
电学与数电基础
   └─ 逻辑门 / 组合时序电路
        └─ 计算机组成原理（数据通路、指令、流水线）
             ├─ CPU 微架构（Cache/分支预测/乱序执行/SIMD）
             ├─ 存储体系（寄存器→Cache→内存→SSD/HDD）
             ├─ 总线与接口（PCIe/USB/DDR/NVMe）
             ├─ GPU 与并行计算
             └─ 固件（BIOS/UEFI/启动流程）
```

## 一、电学与数字电路（打底）

- 电压/电流/电阻、欧姆定律、上拉/下拉、MOSFET 开关特性。
- 布尔代数、卡诺图化简；组合逻辑：加法器、译码器、多路选择器。
- 时序逻辑：锁存器/触发器、建立时间与保持时间（metastability 亚稳态）、同步设计、跨时钟域握手。
- **动手**：Logisim Evolution 搭一个 8 位加法器 + 状态机；进阶用 Verilog 在 FPGA 开发板点亮流水灯。

## 二、计算机组成原理（核心）

- **冯·诺依曼结构**：运算器、控制器、存储器、输入、输出；存储程序思想。
- **指令集 ISA**：CISC（x86，指令变长、向后兼容）vs RISC（ARM/RISC-V，定长指令、load-store）；机器码、寻址方式、调用约定（栈帧、传参寄存器）。
- **数据表示**：补码（为什么 -x 取反加一）、IEEE 754 浮点（精度丢失根源）、大小端、对齐。
- **CPU 数据通路**：PC、指令存储器、寄存器堆、ALU、数据存储器；单周期 → 多周期 → **五级流水线**（IF/ID/EX/MEM/WB）。
- **流水线冒险**：数据冒险（旁路转发、load-use 停顿）、控制冒险（延迟槽/分支预测）、结构冒险。
- **教材**：《计算机组成与设计：硬件/软件接口》（Patterson，RISC-V 版最推荐）→《计算机组成：结构化方法》Tanenbaum →《CPU 自制入门》（动手向）。

## 三、CPU 微架构（性能的秘密）

- **Cache 层级**：L1d/L1i/L2/L3，命中率与 CPI；映射方式、替换算法（LRU 变体）、MESI 缓存一致性协议、伪共享。
- **分支预测**：静态预测、2-bit 饱和计数器、TAGE、返回地址预测器 RAS；预测错误流水线冲刷代价。
- **乱序执行 OoO**：Tomasulo 算法、重排序缓冲 ROB、寄存器重命名消除假依赖、保留站。
- **并行层次**：指令级（流水线/超标量/SIMD：SSE/AVX/NEON）→ 数据级（GPU）→ 线程级（SMT 超线程/多核）。
- **实测**：`perf stat ./prog` 看 cache-miss、branch-miss、IPC；理解"为什么链表比数组慢两个数量级"。

## 四、存储体系

- SRAM（Cache，快而贵）vs DRAM（内存，电容刷新）；DDR4/DDR5 通道与频率。
- 磁盘机械结构、IOPS 概念；SSD 的 NAND 颗粒（SLC/MLC/TLC/QLC）、FTL、磨损均衡、写入放大、OP 冗余空间。
- RAID 0/1/5/6/10 的容量、性能、可靠性权衡；NVMe 协议相比 AHCI 的队列优势。
- 本地文件系统（ext4/XFS/NTFS）、页缓存 PageCache、`mmap`、零拷贝（sendfile/splice）。

## 五、总线与外设接口

- **PCIe**：串行、点对点、差分信号；通道数 x1-x16、代际速率（3.0/4.0/5.0）、枚举与配置空间、SR-IOV 虚拟化直通。
- **USB**：2.0/3.x/USB4 速率与接口形态；Type-C 的交替模式（DP/雷电）与 PD 供电协商。
- 显示接口 DP/HDMI；SATA；音频 I2S；低速总线 UART/I2C/SPI（延伸到嵌入式分类）。
- DMA 与中断：为什么设备能直接读写内存、IRQ/APIC、MSI-X。

## 六、GPU 与并行计算

- GPU 为什么适合并行：大量简单 ALU + SIMT 执行模型；显存带宽 vs CPU 内存带宽。
- CUDA 编程模型：grid/block/thread、shared memory、warp divergence；OpenCL/Metal/ROCm 生态。
- 现代用途：图形渲染、科学计算、AI 训练推理；张量核心 Tensor Core。

## 七、固件与启动

- BIOS → UEFI：GPT 分区、Secure Boot、UEFI 应用；POST 上电自检。
- 启动链：固件 → 引导程序（GRUB/systemd-boot）→ 内核 → initramfs → 用户态。
- 嵌入式对应物：BootROM → SPL/U-Boot → 内核 → 设备树。

## 八、进阶书单与资源

| 方向 | 资源 |
|---|---|
| 组成原理 | 《COD：硬件/软件接口》、南京大学《计算机系统基础》（MOOC） |
| 体系结构 | 《量化研究方法》（Hennessy & Patterson，神书） |
| 动手 | 《CPU 自制入门》、nand2tetris（从与非门搭到跑俄罗斯方块）、自己写 RISC-V 模拟器 |
| 逆向视角 | 《汇编语言》王爽（x86 16 位入门）、《加密与解密》 |

## 九、检验是否学懂的问题

1. 为什么 64 位机上 `int` 还是 4 字节？结构体为什么有 padding？
2. `i++` 在多线程下为什么不是原子的？缓存一致性协议能解决吗？
3. 一次 `rand()` 调用可能触发多少次 cache miss？
4. NVMe SSD 为什么顺序写比随机写快、为什么有"写入放大"？
5. CPU 主频多年停在 3-5GHz，性能提升靠什么？

> 学硬件的最佳反馈来自性能实验：抓包、跑 perf、算带宽。日志里的[时间戳](/tools/timestamp)转换、报文 [Base64](/tools/base64) 解码、固件文件 [Hash 校验](/tools/hash) 这些小工具都备好了。
$md$,
'硬件底层', array['硬件', '组成原理', 'CPU', 'Cache', '学习路线', '体系结构'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;

-- ---------- 文章 4：嵌入式完整学习路线 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '嵌入式开发完整学习路线：从点灯到嵌入式 Linux（2026 修订）',
  'embedded-complete-roadmap-2026',
  '一条可执行的嵌入式成长路径：C 语言与电路基础 → STM32 裸机 → RTOS → 嵌入式 Linux/驱动 → 物联网。每阶段给出核心知识点、开发板推荐、项目阶梯和避坑建议。',
$md$> 嵌入式是"离硬件最近的软件"。本文按真实岗位（单片机工程师 / Linux BSP / 物联网）反向拆出学习路径。

## 路线全景

```
阶段0 前置：C 语言 + 电路基础 + Linux 基本操作
阶段1 裸机：STM32（Cortex-M）寄存器/HAL、中断、通信协议
阶段2 RTOS：FreeRTOS 任务调度与 IPC，做完整项目
阶段3 嵌入式 Linux：系统裁剪、交叉编译、设备树、驱动
阶段4 方向：物联网 / 工业总线 / AIoT / 汽车电子
```

## 阶段 0：前置（约 1-2 个月）

- **C 语言深度**：指针与数组、函数指针与回调、内存四区、`volatile`/`const`/`static` 语义、位操作、结构体对齐与字节序、可变参数。
- 数电模电够用即可：高低电平、上拉电阻、推挽/开漏、欧姆定律、看原理图（电源/最小系统/外设连接）。
- Linux 基础命令、Makefile、GCC 交叉编译概念。
- 工具：Git、串口助手、逻辑分析仪、万用表。

## 阶段 1：裸机开发（STM32，2-3 个月）

**核心知识**

- ARM Cortex-M 架构：寄存器（R0-R15、xPSR）、Thumb 指令集、向量表、启动流程（MSP/Reset_Handler/.data/.bss）。
- 时钟树 RCC、GPIO 八种模式、外部中断 EXTI/NVIC 优先级、SysTick。
- **三大串行协议**：UART（波特率/中断/DMA 收发）、I2C（时序/地址/EEPROM 与传感器）、SPI（四种 CPOL/CPHA、驱动 OLED/Flash）。
- 定时器：PWM（呼吸灯/舵机/电机调速）、输入捕获、编码器接口。
- ADC/DAC、DMA（减轻 CPU 搬运）、低功耗模式（Sleep/Stop/Standby）。
- HAL 库 + CubeMX 图形化配置；但**必须读懂寄存器**，否则只会复制例程。

**推荐开发板**：STM32F103/F407 最小系统板（十几元，资料最多）→ 传感器套件（DHT11、MPU6050、OLED、超声波）。

**项目阶梯**：点灯 → 按键中断/呼吸灯 → 串口 shell → 示波器上位机（采 ADC 传 PC）→ 智能小车（PWM+编码器+循迹/避障）→ 简易示波器/逻辑分析仪。

## 阶段 2：RTOS（1-2 个月）

- 为什么需要 RTOS：轮询架构在多任务下的延迟问题。
- FreeRTOS：任务创建与状态机、抢占调度、时间片；队列、信号量、互斥量（优先级继承）、事件组、任务通知、流缓冲。
- 内存管理方案 heap_1~5、栈大小估算与溢出检测、中断安全 API。
- 软件架构：事件驱动、生产者-消费者、看门狗、上下电状态机。
- **项目**：带 OLED 菜单 + 多传感器采集 + 蓝牙/Wi-Fi 上报的环境监测节点（任务划分是面试高频题）。
- 国产替代：RT-Thread（组件生态好，国内岗位多）、AliOS Things。

## 阶段 3：嵌入式 Linux（3-6 个月，高薪资深门槛）

- **Linux 基础深化**：Shell、文件系统层次、进程/线程、IPC（管道/共享内存/信号量/socket）。
- **系统构建**：交叉编译工具链、Buildroot/Yocto 裁剪根文件系统、U-Boot 移植、内核裁剪与编译。
- **设备树 Devicetree**：把硬件描述从代码里抽出来，`.dts`/`.dtsi`、compatible 匹配、pinctrl/clock/interrupts 属性。
- **驱动开发**：字符设备（cdev、file_operations）、平台总线 platform、子系统（LED/input/IIO/framebuffer）、中断与等待队列、mmap、阻塞/非阻塞 IO。
- 用户态接口：sysfs、procfs、ioctl、热插拔 udev。
- **硬件**：树莓派 4/5（入门）→ 全志/瑞芯微 RK3566/3568 开发板（正点原子/野火，工业常用）。
- **项目**：给一个自定义传感器写 platform 驱动 + 用户态读取程序；Buildroot 打包一个带 Qt/Wayland 的 HMI 系统。

## 阶段 4：方向选择

| 方向 | 加分技能 |
|---|---|
| 物联网 | MQTT/CoAP/HTTP、Wi-Fi（ESP32）、4G/NB-IoT、低功耗、OTA 升级 |
| 工业 | CAN/CANopen、Modbus RTU/TCP、EtherCAT、PLC 通信、功能安全 |
| AIoT | NPU 推理（RK3588）、TFLite Micro、音频前处理 |
| 汽车电子 | AUTOSAR、CAN/LIN/车载以太网、ISO 26262、功能安全 |
| 消费电子 | 蓝牙 BLE、低功耗设计、RTOS 裁剪、量产烧录与测试 |

## 书籍与资源

- C：《C 和指针》《C 专家编程》；体系：《ARM Cortex-M3 权威指南》。
- RTOS：《Mastering the FreeRTOS Kernel》（官方免费手册）。
- Linux：《Linux 设备驱动开发详解》、宋宝华《Linux 设备驱动开发详解》、韦东山/正点原子视频。
- 协议：各厂商 datasheet 才是第一手资料（养成读手册的习惯）。

## 避坑建议

1. 别在 HAL 库"图形化拖控件"阶段停留太久，面试和实战都要求看懂寄存器与时序图。
2. 协议出问题先上**逻辑分析仪**，90% 的问题是时序/波特率/电源。
3. 先画状态图再写多任务代码；中断里只做标记，处理放任务里。
4. 嵌入式 Linux 驱动岗最看重：datasheet 阅读能力 + 内核源码阅读能力。

> 嵌入式调试中常要解析串口协议：[Base64 编解码](/tools/base64)、[CRC/Hash](/tools/hash)、[正则提取](/tools/regex) 随用随取。
$md$,
'嵌入式', array['嵌入式', 'STM32', 'FreeRTOS', '嵌入式Linux', '设备树', '学习路线'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;

-- ---------- 文章 5：软件工程知识体系 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '软件工程师知识体系地图：语言基础到系统设计（持续更新）',
  'software-engineer-knowledge-map',
  '把"软件工程师要学什么"拆成可验收的能力清单：编程语言、数据结构算法、操作系统、数据库、网络、系统设计、工程化与软技能，附各阶段项目建议。',
$md$> 这是一张"能力地图"而不是书单堆砌：每一项都说明**为什么学、学到什么程度、怎么验证**。

## 一、编程语言（选一个深，其余能读）

- **语言模型**：编译型/解释型、静态/动态类型、值类型与引用类型、作用域与闭包、GC vs 手动管理（RAII）。
- **并发**：线程/协程/goroutine/asyncio、锁、无锁、内存模型（happens-before、Java volatile/C++ atomic）。
- 建议主线：**Java/Go（后端岗）、C++（性能/游戏/嵌入式上层）、TypeScript（全栈/前端）、Python（AI/脚本）**，再学一门 Rust 感受所有权。
- 验收：能说清 HashMap 扩容、字符串不可变、异常/错误返回值的成本；读过一次语言标准库关键源码。

## 二、数据结构与算法

- 线性表、哈希表、树（BST/平衡树/B+树/Trie）、堆、图、跳表、布隆过滤器。
- 算法：排序/查找、双指针滑窗、BFS/DFS、回溯、贪心、DP、图论最短路/最小生成树。
- 复杂度分析：时间/空间、均摊、最好最坏；知道什么数据规模对应什么复杂度上限。
- 验收：LeetCode Hot 100 独立完成；在业务里能识别 N+1 查询、深拷贝大对象等真实性能问题。

## 三、操作系统

- 进程/线程/协程、调度算法、用户态/内核态、系统调用。
- 内存：虚拟内存、分页、缺页中断、mmap；锁原理（互斥锁/自旋锁/CAS）、死锁四条件。
- IO：五种 IO 模型（阻塞/非阻塞/IO多路复用 select-poll-epoll/信号/异步）、Reactor。
- 验收：`top/vmstat/iostat/strace` 能定位 CPU/IO/锁问题；能手写一个简单线程池。

## 四、数据库

- **MySQL**：B+树索引、聚簇/二级索引、最左前缀、事务隔离级别与 MVCC、锁（记录锁/间隙锁/Next-Key）、redo/undo/binlog、主从复制、慢查询与 EXPLAIN。
- **Redis**：五大数据结构与底层（SDS/ziplist/hashtable）、持久化 RDB/AOF、过期与淘汰策略、缓存三兄弟（穿透/击穿/雪崩）、分布式锁、集群与一致性。
- 设计范式与反范式、分库分表（什么时候才需要）、NewSQL 概念。
- 验收：给一条慢 SQL 能通过 EXPLAIN 优化；能设计秒杀的库存扣减方案。

## 五、计算机网络（见网络工程分类，这里列程序员必会）

- 三次握手/四次挥手、TIME_WAIT、拥塞控制；HTTP/1.1 keep-alive、HTTP/2 多路复用、HTTP/3。
- HTTPS/TLS 握手、证书；DNS 解析全过程；CDN 原理。
- 验收：能用 tcpdump/Wireshark 定位"接口偶发超时"。

## 六、系统设计（区分高级与初级的关键）

- **设计原则**：SOLID、KISS/DRY、幂等、最终一致性。
- **高并发**：缓存、异步化（MQ）、池化、限流（令牌桶/漏桶）、降级熔断、读写分离、分库分表、CDN。
- **高可用**：冗余、故障转移、超时重试退避、幂等去重、灰度发布、回滚。
- **分布式**：CAP/BASE、一致性协议（Paxos/Raft 直觉）、分布式 ID、分布式事务（TCC/本地消息表/Saga）、注册中心/配置中心。
- 经典题：设计短链、秒杀、Feed 流、限流器、IM 消息系统（本站群聊就是练手案例）。
- 验收：从 1 台到 1000 台，每一层瓶颈在哪里、先加机器还是先加缓存，讲得出取舍。

## 七、工程化

- Git 工作流、Code Review、单元/集成测试、TDD 取舍、CI/CD、静态扫描。
- Docker、docker compose、Kubernetes 基本对象（Pod/Deployment/Service/Ingress）。
- 可观测性：日志（结构化 + traceId）、Metrics（Prometheus）、Tracing（OpenTelemetry）。
- API 设计：RESTful、版本化、错误码规范、鉴权（JWT/OAuth2）、OpenAPI 文档。

## 八、安全与合规（底线）

- OWASP Top 10、参数化查询、输出编码、最小权限、密钥管理、依赖漏洞扫描。
- 隐私合规意识（用户数据脱敏、日志不落敏感信息）。

## 九、软技能与成长

- 需求澄清（方案评审先问"不做什么"）、技术文档写作、排障方法论（分层二分、最小复现）。
- 复盘文化：故障 Postmortem 对事不对人。

## 十、项目阶梯建议

1. 单体 CRUD 博客 → 加缓存、加搜索（ES）、加消息队列（异步通知）。
2. 拆成微服务：网关 + 注册中心 + JWT（本网站本身就是现成样例：[架构图鉴](/architecture)）。
3. 加量：压测（wrk/jmeter）找瓶颈 → 上 Redis/MQ/分库分表 → 容器化 + 监控告警。
4. 读源码：Spring → Netty → Redis；参与一个有真实 issue 的开源项目。

> 日常开发高频小工具：[JSON 格式化](/tools/json)、[正则测试](/tools/regex)、[文本对比](/tools/diff)、[UUID](/tools/uuid)、[时间戳](/tools/timestamp)、[URL 编解码](/tools/url)，都在工具站常驻。
$md$,
'软件工程', array['软件工程', '学习路线', '系统设计', '后端', '知识体系'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;

-- ---------- 文章 6：网络工程师成长路线 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '网络工程师成长路线：从 TCP/IP 到园区组网与自动化',
  'network-engineer-roadmap',
  '完整网络知识体系：OSI/TCP-IP 协议栈 → 路由交换（VLAN/OSPF/BGP）→ 无线网络 → 网络安全基础 → 抓包排障 → 网络自动化/SD-WAN，附认证路径（HCIA→CCNP/IE）与实验环境。',
$md$## 一、学习路线全景

```
网络基础（OSI/TCP-IP、IP/子网划分）
  → 交换（VLAN/Trunk/STP/链路聚合）
  → 路由（静态/RIP/OSPF/BGP）
  → 广域网与接入（NAT/PPPoE/VPN/SD-WAN）
  → 无线（Wi-Fi 6/射频/漫游）
  → 网络安全（ACL/防火墙/零信任）
  → 排障（抓包/Wireshark）与自动化（Python/Ansible/NETCONF）
```

## 二、第一阶段：网络基础

- OSI 七层 vs TCP/IP 四层，每层 PDU、设备（集线器/交换机/路由器）、典型协议。
- 以太网：MAC 地址、ARP/RARP、帧格式、MTU 与分片。
- **IP 与子网划分（必须滚瓜烂熟）**：IPv4 地址分类、CIDR、网络号/广播号/可用主机数、IPv6 表示与邻居发现。
- TCP：三次握手四次挥手、滑动窗口、拥塞控制；UDP 适用场景。
- 应用层：DHCP（DORA 四步）、DNS（递归/迭代）、HTTP/HTTPS、NTP。
- 练习：`ping/tracert/arp -a/ipconfig`；用 [60秒API广场](/tools/api-plaza) 发请求配合抓包。

## 三、第二阶段：交换技术

- **VLAN**：广播域隔离、Access/Trunk/Hybrid 端口、802.1Q 标签。
- VLAN 间路由：单臂路由、三层交换机 SVI。
- **STP 生成树**：防环原理、根桥选举、端口角色状态、RSTP/MSTP；广播风暴现象与处置。
- 链路聚合 LACP、端口安全（MAC 绑定/端口隔离）、DHCP Snooping、DAI、风暴控制。
- 实验：eNSP / Huawei ENSP / Cisco Packet Tracer / EVE-NG / GNS3 搭三交换机 VLAN+STP 拓扑。

## 四、第三阶段：路由技术

- 路由表与优先级（管理距离/度量值）、最长掩码匹配原则。
- 静态路由、默认路由、浮动静态路由。
- **OSPF**：链路状态、Area 0 骨干、DR/BDR、Cost、邻居状态机（Down→Full）、LSA 类型、路由汇总。
- BGP：AS、EBGP/IBGP、路径属性（AS-Path/Local-Pref/MED）、选路原则（互联网骨干协议）。
- 策略路由 PBR、路由重分发、VLA。
- 实验室：多区域 OSPF + BGP 双 ISP 出口选路。

## 五、第四阶段：广域网、VPN 与现代组网

- NAT（SNAT/DNAT/PAT）、端口映射、NAT 穿越。
- 传统 VPN：IPSec（站点到站点）、SSL VPN（远程接入）、L2TP。
- **现代零信任组网**：WireGuard/Tailscale、ZeroTier（见我写的 [免费远程指南：RustDesk + Tailscale](/blog)）、SD-WAN（应用识别、多线负载、链路质量探测）。
- MPLS、QoS（分类/标记/队列/整形）。
- 家庭/小企业实战：OpenWrt 软路由、多拨、策略路由、VLAN 单线复用。

## 六、第五阶段：无线网络

- 802.11 a/b/g/n/ac/ax(Wi-Fi 6)/be(Wi-Fi 7)、2.4G vs 5G/6G 特性、信道与干扰。
- AP/AC 架构、瘦 AP+AC、PoE 供电、CAPWAP。
- 漫游（二层/三层、802.11r/k/v）、射频管理、负载均衡、无线安全（WPA2/WPA3-Enterprise、802.1X + RADIUS）。
- 勘测工具：inSSIDer、Wi-Fi Analyzer、Ekahau。

## 七、第六阶段：安全基础与高可用

- ACL（标准/扩展/命名）、防火墙区域（Trust/Untrust/DMZ）、安全策略、NAT 策略。
- 下一代防火墙 NGFW：应用识别、IPS、AV、URL 过滤。
- 高可用：VRRP/HSRP 网关冗余、堆叠/CSS、双机热备、链路多归。
- 等保 2.0 对网络架构的要求（分区分域、边界防护、审计留存）。

## 八、第七阶段：排障与自动化（拉开差距）

**抓包排障（核心硬功夫）**

- Wireshark：显示/捕获过滤器、Follow TCP Stream、专家信息、IO Graph、TCP 重传/零窗口分析。
- 排障方法论：分层法（物理→链路→网络→应用）、分段法、替换法。
- 典型案例：网站慢（DNS 慢？TCP RTT？TLS 握手？服务器响应？在 Wireshark 里逐段看时间差）。

**网络自动化**

- Python：netmiko/paramiko 批量下发、NAPALM 取状态；SNMP（v2c/v3）监控。
- 厂商中立 API：NETCONF/YANG、RESTCONF；gNMI 流式遥测。
- Ansible 网络模块批量配置；Zabbix/Prometheus + SNMP exporter 监控。

## 九、认证路径建议

- 入门：华为 **HCIA** Datacom → HCIP（国内就业性价比高）。
- 进阶：思科 CCNA → **CCNP Enterprise**（OSPF/BGP 讲得透）；冲 CCIE/HCIE 看岗位需要。
- 安全向：HCIA-Security / NSE（Fortinet，防火墙实操多）。
- 云网络：阿里云 ACP/华为云 HCIP-Cloud（VPC、安全组、负载均衡、云专线）。

## 十、检验清单

1. 不查资料划分 10.10.0.0/22 的子网范围和主机数。
2. 说清浏览器输入 URL 到页面展示全过程，每一步的协议与设备。
3. Wireshark 抓出三次握手、TLS 握手、TCP 重传。
4. 配置多 VLAN + OSPF 互通 + 出口 NAT + 策略放行。
5. 用 Tailscale/IPSec 把两个异地站点打通（我的博客有详细实战）。

> 排障辅助：抓包中的 Base64 payload 用 [Base64 工具](/tools/base64) 解、时间戳用 [时间戳转换](/tools/timestamp) 对、可疑文件用 [Hash 生成](/tools/hash) 验，[正则测试](/tools/regex) 提取日志字段都很方便。
$md$,
'网络工程', array['网络工程', 'OSPF', 'VLAN', 'Wireshark', '学习路线', 'CCNA', 'BGP'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;

-- ---------- 文章 7：网络攻防入门全景 ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select '网络攻防入门全景：Web 安全、渗透测试到红蓝对抗（合法学习路径）',
  'cybersecurity-offense-defense-roadmap',
  '一份合规、完整的网络安全学习地图：安全基础→Web 安全→渗透测试→内网横向→逆向二进制→防御与应急响应。强调法律边界、靶场训练和"懂攻是为了守"。',
$md$> ⚠️ **法律红线**：未经书面授权对任何系统进行扫描、入侵、数据获取都可能违反《网络安全法》《刑法》第 285/286 条。所有练习只在本地靶场或官方授权环境进行。

## 一、学习路径全景

```
安全基础（网络/Linux/Web 开发 + 密码学）
  → Web 安全（OWASP Top 10）
  → 渗透测试（信息收集/漏洞利用/提权）
  → 内网渗透与横向移动
  → 逆向与二进制（可选深水区）
  → 防御侧：蓝军/SOC/应急响应/威胁狩猎
  → 合规：等保 2.0
```
安全是"交叉学科"，先有开发和网络底子再学会快很多（见[软件工程地图](/blog)与[网络工程路线](/blog)）。

## 二、前置基础

- **Linux**：命令、权限、进程服务、bash 脚本（Kali 是工具不是魔法，先会用 Debian/Ubuntu）。
- **网络**：TCP/IP、HTTP 细节（Cookie/Session/CORS/缓存）、DNS。
- **Web 开发**：至少写过一个前后端增删改查项目，理解请求从浏览器到数据库的完整路径。
- **Python**：写脚本处理数据、调用 POC 框架。
- **密码学常识**：编码≠加密；哈希/对称/非对称/证书/JWT。

## 三、Web 安全（最主流入门方向）

按 OWASP Top 10 + 实战高频：

1. **SQL 注入**：联合/报错/布尔盲注/时间盲注；成因是字符串拼接 SQL；根治=预编译参数化。工具 sqlmap 只在靶场用。
2. **XSS**：反射/存储/DOM 型；窃取会话、键盘记录；防御=输出编码+CSP+HttpOnly。
3. **越权（IDOR/水平垂直越权）**：改 id/参数操作他人数据；服务端必须做对象级鉴权。
4. **文件上传**：绕过类型检查 getshell；白名单+重命名+隔离存储+禁用执行。
5. **命令执行/代码执行**：反序列化（Java Shiro/Fastjson 历史 CVE）、模板注入 SSTI、`${}` 表达式注入。
6. **SSRF**：借服务端请求内网与云元数据；URL 白名单+禁重定向。
7. **XXE**：XML 外部实体读文件；禁用外部实体。
8. **CSRF**：跨站冒用 Cookie；SameSite + Token。
9. **逻辑漏洞**：验证码绕过、支付金额篡改、密码找回流程、竞态条件（并发领券）。
10. **认证与会话**：弱口令、JWT none 算法/弱密钥、会话固定、OAuth 配置错误。

**靶场（全部本地/官方）**：DVWA、Pikachu、sqli-labs、upload-labs、PortSwigger Web Security Academy（免费且体系化，强烈推荐）、VulnHub、HTB（Hack The Box）、国内攻防世界。

## 四、渗透测试完整流程（PTES）

1. **授权与范围确认**：书面 scope、时间窗口、禁止事项、应急联系人。
2. **信息收集**：子域名（subfinder/OneForAll）、IP/端口指纹（nmap -sV）、Web 指纹（Wappalyzer/fofa 语法）、GitHub 泄露、目录（ffuf）。
3. **漏洞探测**：手工验证为主、扫描器（nuclei）辅助；注意扫描器的噪声和漏报。
4. **漏洞利用**：拿 Web 权限（webshell）→ 数据库 → 提权。
5. **后渗透**：权限维持（仅报告演示）、内网信息收集、横向移动。
6. **痕迹清理与报告**：真实授权测试按合同处理；报告含 CVSS 评级、复现步骤、影响、修复建议。

**Linux 提权常识（用于理解防御）**：内核版本漏洞、`sudo -l` 错误配置、SUID 异常文件、计划任务可写、capabilities、docker 组逃逸。

## 五、内网渗透（进阶，攻防对抗核心）

- 工作组/域概念、NTLM/Kerberos 认证、DC 域控。
- 攻击技术（作为防御方必须了解）：Responder/NTLM Relay、Kerberoasting、白银票据/黄金票据原理、PsExec/WMI 横向、票据传递 PtT。
- 隧道与代理：frp、nps、EarthWorm（理解攻击者怎么把内网流量带出来，才能在出口部署检测）。
- 学习重点：**日志与检测**——Sysmon、4624/4688/4768 等事件 ID、EDR 告警逻辑。

## 六、逆向与二进制（深水区，可选）

- x86/x64 汇编、栈帧、调用约定；栈溢出 → ret2text/ROP/ret2libc、堆基础（unlink/uaf）。
- 工具：x64dbg、IDA Free、Ghidra、gdb+pwndbg、checksec；pwn 环境 pwn.college/CTF-wiki。
- 软件逆向：去花指令、反调试、Hook（Frida）、小程序/APP 逆向（法律风险高，仅限自有程序或授权）。
- 现代缓解机制：ASLR/DEP/NX/Canary/PIE/RELRO。

## 七、防御侧（蓝军/安全开发/运维安全）

- **安全开发生命周期 SSDLC**：威胁建模（STRIDE）、代码审计（SonarQube/CodeQL）、依赖 SCA、DAST。
- **WAF/IPS/EDR**：规则原理（ModSecurity/OWASP CRS）、绕过与调优的攻防博弈。
- **应急响应**：事件分类、隔离取证（内存镜像、磁盘镜像）、日志分析、 webshell 排查（D 盾/河马）、勒索处置流程（断网→止损→溯源→恢复）。
- **威胁狩猎**：ATT&CK 矩阵、Sigma 规则、IOC/IOA、威胁情报。
- **加固基线**：CIS Benchmark、等保 2.0 三级要求、最小权限、补丁管理。
- **钓鱼防御**：SPF/DKIM/DMARC、MFA、安全意识培训。

## 八、证书与职业方向

| 方向 | 证书/能力 |
|---|---|
| Web 安全/渗透 | OSCP（实战金证）、PNPT、CRTE（内网）、国内 NISP/软考 |
| 防御/SOC | CompTIA Security+、CISSP（管理向）、国内 CISP |
| 云安全 | CCSK、云厂商安全认证、K8s 安全（RBAC/NetworkPolicy） |
| 二进制 | 自主项目 + CTF 成绩比证书有用 |

## 九、成长建议

1. **先做开发再做安全**：挖不出漏洞往往是因为没写过对应的功能。
2. 建本地实验环境：VMware + 靶机 + Kali 隔离网络；快照随便打。
3. 写复盘博客，把每个漏洞的"成因→利用→修复代码"写全（本站 [知识库](/knowledge) 就是我的积累方式）。
4. 关注 CVE 与真实案例：Log4Shell、SpringShell、Exchange ProxyLogit，理解补丁 diff。
5. 守住职业伦理：白帽提交走 SRC/CNVD；能力越大，边界感越重要。

> 安全研究常用小工具：payload 的 [Base64/URL 编码](/tools/base64)、样本 [Hash 计算](/tools/hash)、正则提取日志用[正则测试](/tools/regex)、批量标识可用 [UUID 生成](/tools/uuid)。
$md$,
'网络安全', array['网络安全', '渗透测试', 'OWASP', '应急响应', '学习路线', 'Web安全', '红蓝对抗'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;


-- ---------- 文章 8：Jev 学习资源汇总（群公告） ----------
insert into public.blog_articles
  (title, slug, summary, content, category, tags, author_name, views, likes, is_top, published, created_at)
select 'Jev 学习资源汇总：从排队到上手的全链接路',
  'jev-learning-resources',
  '一份围绕 TypeSafe AI Jev 模型的学习资源汇总：官网排队、OpenRouter 与 Vercel 三种接入方式、能力展示站、Codex 实战案例、GitHub 生态项目与多维度测评，带你快速玩转 Jev。',
$md$> 本文为社群公告整理，汇总 Jev 学习与上手所需的全部入口，欢迎补充 👏

## 一、Jev 是什么

Jev 是 TypeSafe AI 推出的 System One 评估模型，专为软件中的快速结构化决策设计。官网：

- 官网：[jev-llm.com](https://www.jev-llm.com/)

> 更深入的 Jev 实战体验，见本站博客《Jev：Vercel AI Gateway 上免费的结构化决策评估模型体验》。

## 二、怎么用上 Jev

三种方式任选其一：

### 1. 官网排队（送 $5 额度）

1. 进入 [typesafe.ai](https://typesafe.ai) → 点击 **Join Waitlist**；
2. 通过后到 [console.typesafe.ai](https://console.typesafe.ai) 创建 API Key；
3. 文档：[docs.typesafe.ai](https://docs.typesafe.ai)。

### 2. OpenRouter 直接使用

无需排队，打开即用：

- [openrouter.ai/~typesafe/jev-latest](https://openrouter.ai/~typesafe/jev-latest)

### 3. Vercel AI Gateway 免费使用

- [vercel.com/ai-gateway/models/jev](https://vercel.com/ai-gateway/models/jev)

## 三、Jev 能力展示站汇总

- [jevs.youware.app](https://jevs.youware.app/) —— Jev 使用能力网站汇总

## 四、Jev 用于 Codex 案例

- [X 帖子 @Saccc_c](https://x.com/Saccc_c/status/2100833094291087773?s=20)

## 五、相关 GitHub 项目与生态

- Awesome-Jev-Projects：[logicrw.github.io/awesome-jev-projects](https://logicrw.github.io/awesome-jev-projects/)
- Awesome-Jev 资源汇总：[github.com/cobanov/awesome-jev](https://github.com/cobanov/awesome-jev)
- Jev 生态项目大全（群主 🌟）：[agentskillshub.top/best/typesafe-jev](https://agentskillshub.top/best/typesafe-jev/)

## 六、测评对比

| 对比维度 | 链接 |
|---|---|
| 传统 ML vs Jev | [X 帖子](https://x.com/GoSailGlobal/status/2100973279771246861?s=20) |
| 主流大语言模型 vs Jev | [X 帖子](https://x.com/GoSailGlobal/status/2101255598256050233?s=20) |
| BERT vs Jev | [X 帖子](https://x.com/GoSailGlobal/status/2101259812709535800?s=20) |

## 七、多模态 Jev 即将上线

- [X 帖子 @GoSailGlobal](https://x.com/GoSailGlobal/status/2101626114448019637?s=20)

---

> 资源持续更新，欢迎在 [社区](/community) 或 [留言板](/comments) 补充你发现的 Jev 玩法 👏
$md$,
'前沿AI', array['Jev', '学习资源', 'TypeSafe AI', 'AI Gateway', 'OpenRouter'],
'极光星途', 0, 0, false, true, now()
on conflict do nothing;

