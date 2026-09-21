-- 批次 B：扩充 4 条偏短知识库条目到 4000+ 字
UPDATE kb_entries SET
  content = $kb$# HTTPS/TLS 握手与证书体系详解：从 RSA 到 ECDHE，再到 Let's Encrypt 自动签发

> 这一篇咱们把 HTTPS 底层的 TLS 协议扒个底朝天。从握手流程、密钥交换算法、证书链验证，到生产环境的 Let's Encrypt 自动签发，都是我在博客上线和后续迁移过程中真实踩过的坑。

## 一、为什么需要 TLS：HTTP 的原罪

HTTP 是明文传输，链路上任何一个节点（运营商路由、AP、CDN 节点）都能看到、篡改你的请求。早年间博客圈常见的"页面被插广告""DNS 污染跳转"都是这么来的。

TLS（Transport Layer Security）就是在 TCP 和 HTTP 之间塞一层加密通道，干三件事：

1. **加密**：内容只有双方能看懂
2. **完整性**：内容在传输中没被改
3. **身份**：你连的 github.com 真的就是 github.com

下面这段是一个最简化的 HTTPS 抓包示意（用 Wireshark 看 TLS 1.2 握手）：

```
Client Hello → Server Hello
Server → Certificate, Server Key Exchange, Server Hello Done
Client → Client Key Exchange, Change Cipher Spec, Finished
Server → Change Cipher Spec, Finished
Application Data (加密)
```

注意 TLS 1.3 已经把流程压到 1-RTT 甚至 0-RTT，握手包数量少了一半，这个下面专门讲。

## 二、TLS 1.2 握手全流程拆解

### 2.1 Client Hello

客户端第一个包，告诉服务器：
- 支持的 TLS 版本（比如 0x0303 = TLS 1.2）
- 支持的密码套件（Cipher Suites）列表，按优先级排
- 客户端随机数 Client Random（32 字节）
- SNI（Server Name Indication）：你要连的域名——这个字段很关键，没它一个 IP 上多个站就没法分证书

```
TLSv1.2 Record Layer: Handshake Protocol: Client Hello
    Version: TLS 1.2 (0x0303)
    Random: 5c8f...
    Cipher Suites (17 suites)
        Cipher suite: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 (0xc02f)
        Cipher suite: TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 (0xc030)
    Extension: server_name (len=16)
        Server Name: blog.example.com
```

### 2.2 Server Hello + Certificate

服务器回应：
- 选定的密码套件（比如 TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256）
- 服务端随机数 Server Random
- 证书链：服务器证书 + 中间 CA 证书

证书长这样（PEM 格式）：

```bash
-----BEGIN CERTIFICATE-----
MIIFjTCC...
-----END CERTIFICATE-----
```

要看具体内容，用 OpenSSL：

```bash
openssl s_client -connect blog.example.com:443 -servername blog.example.com </dev/null 2>/dev/null | openssl x509 -noout -text
```

输出里重点关注几个字段：
- Issuer：签发方
- Subject Alternative Name：覆盖的域名列表
- Not Before / Not After：有效期
- Public Key Algorithm：RSA-2048 还是 ECDSA-P256
- Signature Algorithm：sha256WithRSAEncryption 或 ecdsa-with-SHA256

### 2.3 密钥交换：RSA vs ECDHE

历史上两种主流密钥交换算法差别巨大：

#### RSA 密钥交换（已淘汰）

客户端生成一个 PreMaster Secret，用服务器证书里的 RSA 公钥加密发给服务器。双方基于 Client Random + Server Random + PreMaster 派生出会话密钥。

问题：**服务器私钥一旦泄漏，历史抓包全部可解**。这就是前向保密（Forward Secrecy）缺失。Chrome 2016 年就开始干掉 RSA 密钥交换。

#### ECDHE 密钥交换（主流）

双方各自生成一对临时椭圆曲线密钥对（每次会话新一对），交换公钥，各自算出共享密钥：

```
服务器：临时私钥 d_s，公钥 Q_s = d_s × G
客户端：临时私钥 d_c，公钥 Q_c = d_c × G
共享密钥：d_c × Q_s = d_s × Q_c = d_c × d_s × G
```

服务器私钥只用来**签名**（证明自己身份），不参与密钥派生。私钥泄漏不影响历史会话。ECDHE 一般用 secp256r1 曲线（也叫 P-256）。

Cipher suite 名字里 ECDHE 表示密钥交换，RSA 表示签名算法，AES_128_GCM 是对称加密，SHA256 是 PRF/校验：

```
TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
     └─┬─┘ └─┬─┘       └─┬──┘  └─┬─┘
       │     │            │       └─ MAC/PRF
       │     │            └─ 对称加密
       │     └─ 服务器证书签名算法
       └─ 密钥交换
```

### 2.4 Finished 消息

握手最后双方互发一条 Finished 消息，内容是整个握手过程的摘要 + 协商好的密钥做 HMAC。这一步是防降级攻击的最后一道闸——如果中间人篡改过 Client Hello 里的密码套件列表，Finished 校验必然对不上。

## 三、TLS 1.3 的简化与提速

TLS 1.3 把密钥交换挪到 Client Hello 阶段，握手从 2-RTT 砍到 1-RTT。如果用 PSK（Pre-Shared Key）模式还能 0-RTT，第一个包就能带应用数据。

```
Client Hello (含 KeyShare) →
                          ← Server Hello, Encrypted Extensions, Finished
Finished, Application Data →
Application Data ↔
```

但 0-RTT 不防重放攻击，**不要在 0-RTT 数据里做非幂等操作**（POST 下单、转账），nginx 默认 ssl_early_data on 也只能保护 GET/HEAD。

TLS 1.3 还强制要求前向保密，**RSA 密钥交换被删掉了**。老服务器的 RSA-only 套件在 1.3 下没法协商。

## 四、证书链验证

服务器一般发两条证书：自己的 + 中间 CA 的。客户端验证链：

```
服务器证书 ─→ 中间 CA ─→ 根 CA（系统/浏览器内置）
```

验证逻辑（OpenSSL 简化版）：

```bash
# 验证链
openssl verify -CAfile chain.pem server.crt
# 输出：server.crt: OK
```

实战最常见的坑是**只发了自己的证书，没发中间证书**。手机/老浏览器还能凭系统的中间证书缓存救一下，新浏览器/Go HTTP 客户端会直接报 x509: certificate signed by unknown authority。

nginx 配置一定要 fullchain.pem 而不是 cert.pem：

```nginx
ssl_certificate     /etc/letsencrypt/live/blog.example.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/blog.example.com/privkey.pem;
```

### 证书透明（CT）和 OCSP

现代证书都有 SCT（Signed Certificate Timestamp），证明证书被 CT 日志收录。可以防 CA 偷偷给你签了张证书你没发现。OCSP Stapling 让服务器把 OCSP 状态打包发给客户端，省一次客户端→OCSP 服务器的查询：

```nginx
ssl_stapling on;
ssl_stapling_verify on;
resolver 1.1.1.1 valid=300s;
```

## 五、Let's Encrypt 自动签发

Let's Encrypt 是免费的 CA，签发 90 天短期证书。配合 ACME 协议，证书申请、续期全自动。

### 5.1 acme.sh 方案（推荐，零依赖）

我博客用的是 acme.sh，纯 shell 脚本，不依赖 Python，dns 验证方式最省事。

```bash
# 安装
curl https://get.acme.sh | sh -s email=admin@example.com
# 切换 CA 到 Let's Encrypt
~/.acme.sh/acme.sh --set-default-ca --server letsencrypt

# DNS 验证签发（以阿里云 DNS 为例）
export Ali_Key="LTAI..."
export Ali_Secret="..."
~/.acme.sh/acme.sh --issue --dns dns_ali -d blog.example.com -d www.example.com

# 安装证书到 nginx
~/.acme.sh/acme.sh --install-cert -d blog.example.com \
  --key-file       /etc/nginx/ssl/blog.key \
  --fullchain-file /etc/nginx/ssl/blog.crt \
  --reloadcmd      "systemctl reload nginx"
```

acme.sh 自动装 cron 每天 60 天续期，省心。

### 5.2 HTTP-01 验证踩坑

如果用 HTTP-01 验证（不配 DNS API），CA 会访问 http://blog.example.com/.well-known/acme-challenge/<token>。坑在于：

1. 80 端口必须开（很多云厂商默认只开 443）
2. nginx 301 跳 HTTPS 不能跳过这个路径，否则验证失败
3. Cloudflare 代理（橙云）可能挡掉这个请求，需要临时灰云

正确写法：

```nginx
location ^~ /.well-known/acme-challenge/ {
    root /var/www/html;
    # 不要 301 跳转，直接服务静态文件
}
location / {
    return 301 https://$host$request_uri;
}
```

### 5.3 通配符证书

Let's Encrypt 支持通配符（*.example.com），但**只能用 DNS-01 验证**。这点在博客早期没经验时栽过——以为 HTTP-01 也能签发通配符，搞了一下午。

```bash
~/.acme.sh/acme.sh --issue --dns dns_ali -d example.com -d '*.example.com'
```

## 六、生成自签证书用于测试

本地开发或内网调试时常用自签证书。OpenSSL 一条命令搞定：

```bash
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout server.key -out server.crt \
  -days 365 \
  -subj "/CN=dev.local" \
  -addext "subjectAltName=DNS:dev.local,IP:127.0.0.1"
```

客户端要信任这个证书，否则 curl 报错：

```bash
# 临时跳过验证（仅测试）
curl -k https://dev.local/api
# 或信任特定 CA
curl --cacert server.crt https://dev.local/api
```

Java/Go 程序不信任系统证书库以外的自签证书。Go 客户端要么 InsecureSkipVerify: true（仅测试），要么把自签证书塞进 x509.SystemCertPool() 再 AppendCertsFromPEM。

## 七、TLS 握手调试三板斧

线上 TLS 报错先不要瞎搜，三个命令基本能定位：

### 7.1 openssl s_client 看握手详情

```bash
echo | openssl s_client -connect blog.example.com:443 -servername blog.example.com -showcerts 2>&1 | head -50
```

-showcerts 打印完整证书链，看链是否完整。verify return code: 0 (ok) 表示验证通过；如果是 20 或 21，就是中间证书缺失或过期。

### 7.2 强制指定 TLS 版本排查兼容性

```bash
# 强制 TLS 1.2
openssl s_client -connect blog.example.com:443 -tls1_2
# 强制 TLS 1.3
openssl s_client -connect blog.example.com:443 -tls1_3
# 指定 cipher
openssl s_client -connect blog.example.com:443 -cipher 'ECDHE-RSA-AES128-GCM-SHA256'
```

### 7.3 curl -v 看协商过程

```bash
curl -v https://blog.example.com 2>&1 | grep -E 'SSL|TLS|cipher|certificate'
```

输出里能看到协商出的 TLS 版本、cipher、证书链。最近排查一个老 Java 8 客户端连不上博客的 bug，就是用这条命令发现服务器只支持 TLS 1.2+，而 Java 8 默认开 TLS 1.0。

## 八、生产环境 nginx 安全配置

贴一段我博客 nginx 的实际配置，按 Mozilla SSL Configuration Generator（Intermediate 档）改的：

```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305';
ssl_prefer_server_ciphers off;
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 1d;
ssl_session_tickets off;

# OCSP stapling
ssl_stapling on;
ssl_stapling_verify on;
resolver 1.1.1.1 valid=300s ipv6=off;
resolver_timeout 5s;
```

不要随便开 ssl_prefer_server_ciphers on——在 TLS 1.3 下这个选项无效，1.2 下也容易让老客户端选到弱套件。Modern 档配置只支持 TLS 1.3，会干掉一批老客户端（Java 8、Android 7），博客上 JWT 客户端刚好有这俩，所以选了 Intermediate。

## 九、踩坑总结

1. **证书链不全**：nginx 一定用 fullchain.pem，别用 cert.pem
2. **SNI 不传**：Python requests、curl 直接连 IP 时不带 SNI，要 --resolve 或在 Host header 指定
3. **CT log 缺失**：自签证书在某些 HSTS 浏览器下被拒，需要 -addext sct 或换正式 CA
4. **私钥泄漏重置**：吊销证书走 OCSP/CRL，但浏览器对 CRL 的更新不勤，必要时换新私钥 + 新证书
5. **TLS 1.3 0-RTT 重放**：nginx 默认不开 ssl_early_data，开了要在应用层做 nonce 防重放
6. **acme.sh 续期失败**：阿里云/腾讯云 API key 权限太宽或太窄都会失败，最小权限要给 DNS 全读写

## 十、小结

TLS 这套机制历经 25 年迭代，本质思路没变：握手协商密钥，证书证明身份，对称加密保护数据。理解 ECDHE 为什么替代 RSA 密钥交换、为什么 TLS 1.3 砍掉 0-RTT 之外的所有明文，再去看任何 TLS 报错都不会一头雾水。

写博客这种小项目，Let's Encrypt + acme.sh 是最省心的方案，配 cron 之后基本可以忘掉证书这回事。但理解原理，能让你在出问题时第一眼定位到是哪一段——是握手、是证书、还是 cipher 协商。$kb$,
  updated_at = now()
WHERE slug = 'net-https-tls-handshake-certificates';

UPDATE kb_entries SET
  content = $kb$# FreeRTOS 核心机制：任务、调度器、IPC 一锅端（含死锁实战排查）

> 在嵌入式项目里 FreeRTOS 是绕不开的，从 STM32 到 ESP32 几乎都用它。但很多人会调 API 不会排查问题——任务莫名其妙卡死、互斥量拿不到、ISR 里调了不该调的 API 报 assert。这一篇把任务创建、调度抢占、队列/信号量/互斥量、死锁排查整套串起来，全是项目里真实踩过的坑。

## 一、FreeRTOS 是什么，不是什么

FreeRTOS 是个**抢占式实时操作系统内核**，主要做三件事：
1. 多任务调度（每个任务一个独立栈和上下文）
2. 任务间通信（队列、信号量、互斥量、事件组）
3. 时间管理（tick、软件定时器）

它**不是** Linux 那种完整 OS——没有用户态/内核态分离、没有 MMU、没有文件系统、没有 shell。FreeRTOS 本质上就是个调度器加上一组 IPC 原语，常驻在 MCU 的特权模式下。

配置在 FreeRTOSConfig.h 里，关键参数：

```c
#define configCPU_CLOCK_HZ          ( ( unsigned long ) 72000000UL )
#define configTICK_RATE_HZ          ( ( TickType_t ) 1000 )
#define configMAX_PRIORITIES        ( 7 )
#define configMINIMAL_STACK_SIZE    ( ( uint16_t ) 128 )
#define configTOTAL_HEAP_SIZE       ( ( size_t ) ( 32 * 1024 ) )
#define configUSE_PREEMPTION        1
```

configMAX_PRIORITIES 是个常被忽略的值。它是优先级数，不是任务数。优先级越大数字越大（0 是最低/空闲任务）。所有任务都用同一个优先级会让抢占式调度器退化为时间片轮询。

## 二、任务创建：xTaskCreate 一行背后的玄机

```c
void led_task(void *arg) {
    for (;;) {
        HAL_GPIO_TogglePin(LED_GPIO_Port, LED_Pin);
        vTaskDelay(pdMS_TO_TICKS(500));
    }
}

// 创建
BaseType_t ret = xTaskCreate(led_task, "led", 128, NULL, 2, &led_handle);
configASSERT(ret == pdPASS);
```

参数依次是：函数指针、名字（用于调试）、栈大小（字，不是字节；STM32 上 1 word = 4 bytes）、传给任务的参数、优先级、句柄输出。

### 2.1 栈大小怎么估

新手最容易栽的就是给小了。栈大小估算经验值：

- 纯控制任务（无深调用）：256 word = 1KB
- 含 printf 的任务：至少 512 word = 2KB（printf 在 newlib 下吃栈凶猛）
- 用浮点或调用大量库函数：1024 word 起步

栈溢出检测要开两个钩子：

```c
#define configCHECK_FOR_STACK_OVERFLOW 2
```

模式 2 比模式 1 多检查栈尾的填充模式（被覆盖就 assert），但开销略大。栈溢出钩子里通常闪烁个 LED 或写一条日志：

```c
void vApplicationStackOverflowHook(TaskHandle_t xTask, char *pcTaskName) {
    // 不要在这里调 printf！可能死锁
    (void)xTask;
    HAL_GPIO_WritePin(LED_GPIO_Port, LED_Pin, GPIO_PIN_SET);
    for(;;);
}
```

### 2.2 xTaskCreate vs xTaskCreateStatic

xTaskCreate 从 FreeRTOS 的堆（pvPortMalloc）分配栈和 TCB。xTaskCreateStatic 让用户传静态数组，避免堆碎片。

```c
StaticTask_t led_tcb;
StackType_t led_stack[128];

TaskHandle_t h = xTaskCreateStatic(led_task, "led", 128, NULL, 2, led_stack, &led_tcb);
```

生产环境强烈推荐 static 版本——MCU 上堆碎片化是定时炸弹，特别是任务频繁创建销毁时。

## 三、调度器：抢占 + 时间片 + 优先级

vTaskStartScheduler() 启动后，调度器在每次 tick（默认 1ms）中断里检查：
1. 有没有更高优先级任务 ready
2. 同优先级任务用时间片轮询

抢占逻辑：当前 tick 中断把当前任务挂起，切到 ready 队列里优先级最高的任务。

```c
// 任务 A 优先级 3
void taskA(void *arg) {
    for (;;) {
        // 干活
        vTaskDelay(pdMS_TO_TICKS(10));
    }
}
```

vTaskDelay 把当前任务挂到 delayed 列表，10ms 后再 ready，期间低优先级任务有机会跑。

### 3.1 临界区 vs 关中断

不要随便 taskDISABLE_INTERRUPTS()——会拖累实时性。优先用 taskENTER_CRITICAL()：

```c
taskENTER_CRITICAL();
shared_counter++;
taskEXIT_CRITICAL();
```

临界区里**绝对不能**调用任何带 FromISR 后缀的 API、阻塞 API（vTaskDelay、xQueueReceive 超时非 0），否则可能死锁或断言。

### 3.2 优先级反转

经典场景：低优先级 T1 拿了互斥量 M，高优先级 T3 等 M，中优先级 T2 抢占 T1。结果是 T2 在跑，T3 在等，T1 让不了 M。T3 被 T2 间接卡住，这叫优先级反转。

FreeRTOS 的互斥量带**优先级继承**：T3 等 M 时，T1 的优先级被临时抬到 T3 级别，T2 就抢不动 T1 了，T1 赶紧释放 M 还原。

注意：**优先级继承只在 Mutex 上有，Binary Semaphore 没有**。所以保护共享资源用 Mutex 不要用 Semaphore。

## 四、队列：任务间消息传递主力

队列是 FreeRTOS 最常用的 IPC，本质是个 FIFO，支持多生产多消费、按值拷贝。

```c
typedef struct {
    uint16_t sensor_id;
    float value;
} sensor_msg_t;

QueueHandle_t sensor_q;

void sensor_task(void *arg) {
    sensor_msg_t msg = {1, 23.5f};
    BaseType_t ok = xQueueSend(sensor_q, &msg, pdMS_TO_TICKS(100));
    if (ok != pdPASS) {
        // 100ms 没人收，超时
    }
}

void consumer_task(void *arg) {
    sensor_msg_t rx;
    xQueueReceive(sensor_q, &rx, portMAX_DELAY);  // 永久等
    // 处理
}
```

创建：

```c
sensor_q = xQueueCreate(32, sizeof(sensor_msg_t));
```

队列深度 32，元素大小 sizeof(sensor_msg_t)。坑：**元素按值拷贝**，传指针要小心生命周期——传完后原指针指向的数据不能立刻被改。

### 4.1 队列集（Queue Set）

一个任务要监听多个队列时（比如同时收传感器数据和命令），用队列集：

```c
QueueSetHandle_t qs = xQueueCreateSet(16);
xQueueAddToSet(sensor_q, qs);
xQueueAddToSet(cmd_q, qs);

for (;;) {
    QueueSetMemberHandle_t member = xQueueSelectFromSet(qs, portMAX_DELAY);
    if (member == sensor_q) {
        sensor_msg_t msg;
        xQueueReceive(sensor_q, &msg, 0);
    } else if (member == cmd_q) {
        cmd_t cmd;
        xQueueReceive(cmd_q, &cmd, 0);
    }
}
```

### 4.2 ISR 里用队列

中断里必须用 FromISR 后缀的 API，且不能阻塞：

```c
void USART1_IRQHandler(void) {
    BaseType_t hpw = pdFALSE;
    uint8_t byte = USART1->DR;
    xQueueSendFromISR(uart_q, &byte, &hpw);
    portYIELD_FROM_ISR(hpw);  // 如果唤醒了比当前更高优先级的任务，强制切换
}
```

hpw 是 Higher Priority Task Woken 标志，告诉 ISR 退出时要不要触发一次 PendSV。

## 五、信号量与互斥量

### 5.1 二值/计数信号量

信号量本质是个计数器，给/取。用于"事件通知"和"资源计数"。

```c
SemaphoreHandle_t sem = xSemaphoreCreateBinary();

// ISR 里给
void EXTI0_IRQHandler(void) {
    BaseType_t hpw = pdFALSE;
    xSemaphoreGiveFromISR(sem, &hpw);
    portYIELD_FROM_ISR(hpw);
}

// 任务里等
void btn_task(void *arg) {
    for (;;) {
        if (xSemaphoreTake(sem, portMAX_DELAY) == pdPASS) {
            // 处理按键
        }
    }
}
```

计数信号量用于资源池：

```c
SemaphoreHandle_t pool = xSemaphoreCreateCounting(4, 4);  // 4 个槽位
xSemaphoreTake(pool, portMAX_DELAY);  // 申请
// 用资源
xSemaphoreGive(pool);  // 归还
```

### 5.2 互斥量（Mutex）

Mutex 用于保护共享资源，带优先级继承。拿不到会阻塞，但是**任务里**用，**ISR 里不能用**（中断里不能阻塞）。

```c
SemaphoreHandle_t mu = xSemaphoreCreateMutex();

void write_log(const char *msg) {
    xSemaphoreTake(mu, portMAX_DELAY);
    fprintf(log_fp, "%s\n", msg);  // 临界区
    xSemaphoreGive(mu);
}
```

### 5.3 递归互斥量

同一个任务可以多次 take 同一个 Recursive Mutex，必须等次数相同的 give 才释放：

```c
SemaphoreHandle_t rmu = xSemaphoreCreateRecursiveMutex();

void parent() {
    xSemaphoreTakeRecursive(rmu, portMAX_DELAY);
    child();
    xSemaphoreGiveRecursive(rmu);
}

void child() {
    xSemaphoreTakeRecursive(rmu, portMAX_DELAY);  // OK，不会死锁
    // ...
    xSemaphoreGiveRecursive(rmu);
}
```

普通 Mutex 在 parent/child 互相 take 会**立刻死锁**。

## 六、死锁排查实战

### 6.1 经典死锁

T1 拿了 M1 等 M2，T2 拿了 M2 等 M1。两个任务互相等。FreeRTOS 不会自动检测死锁——任务会永远卡在 xSemaphoreTake。

排查思路：

1. 烧个 debug 版本，开 configUSE_TRACE_FACILITY 和 configUSE_STATS_FORMATTING_FUNCTIONS
2. 调 vTaskList() 看任务状态：

```c
char buf[512];
vTaskList(buf);
printf("%s\n", buf);
```

输出像这样：

```
Name          State  Prio  Stack  Num
led_task      R      2     100    1
log_task      B      3     80     2  ← 阻塞
```

B 是 Blocked，卡在等某个对象。再调 uxTaskGetStackHighWaterMark() 看栈水位——0 表示快溢出了，但死了的任务栈水位会一直卡住，可以反推它在哪里 take 阻塞。

3. **关断点**：在每个 xSemaphoreTake 前加 log 打印任务名和要拿哪个 Mutex，拿到再 log 一次。卡死后看日志最后一行就知道死在哪。

### 6.2 中断里调错 API

报错 Assertion failed: xQueueGenericSendFromISR 或者干脆 HardFault。原因是中断里调了非 FromISR 后缀的 API，FreeRTOS 在 configASSERT 打开时直接断。

经验：**所有中断处理函数里调用的 IPC API 必须有 FromISR 后缀**，且超时参数必须传 0 或 portMAX_DELAY，但前者更安全——ISR 里**禁止阻塞**。

### 6.3 优先级反转没开继承

Binary Semaphore 没有优先级继承。如果用它保护共享资源又碰上 3 个优先级的任务，会发生上面说的反转。**保护资源永远用 Mutex**，事件通知用 Binary Semaphore。

### 6.4 临界区里调阻塞 API

```c
taskENTER_CRITICAL();
xQueueReceive(q, &msg, portMAX_DELAY);  // 灾难
taskEXIT_CRITICAL();
```

xQueueReceive 在临界区里没法把自己挂起（调度器被关），要么直接返回失败要么 HardFault。

## 七、定时器和事件组

### 7.1 软件定时器

不要用 vTaskDelay 当长期定时器，浪费任务槽位。用 xTimerCreate：

```c
TimerHandle_t t = xTimerCreate("report", pdMS_TO_TICKS(10000), pdTRUE, NULL, report_cb);
xTimerStart(t, 0);
```

pdTRUE 是 auto-reload，10 秒周期触发一次回调。回调运行在 FreeRTOS 内部的 Timer Service Task 上，**不能阻塞**，否则会影响所有定时器回调。重活儿用回调里 xQueueSend 通知工作任务处理。

### 7.2 事件组

事件组用来等"多个事件中任一发生"或"一组事件全部发生"：

```c
#define EVT_WIFI_OK    (1 << 0)
#define EVT_MQTT_OK    (1 << 1)

EventGroupHandle_t boot = xEventGroupCreate();

// 等两个事件都发生
xEventGroupWaitBits(boot, EVT_WIFI_OK | EVT_MQTT_OK,
                    pdTRUE,  // 退出时清位
                    pdTRUE,  // 等所有
                    portMAX_DELAY);
```

事件组尤其适合"等所有依赖初始化完成再启动业务任务"。

## 八、性能与内存监控

### 8.1 CPU 占用率

vTaskGetRunTimeStats() 输出每个任务 CPU 占用百分比。需要配一个高频定时器做时间统计源：

```c
#define configGENERATE_RUN_TIME_STATS 1
volatile unsigned long ulHighFrequencyTimerTicks;
void configureTimerForRunTimeStats(void) {
    // 配个 10kHz 计数器
}
unsigned long getRunTimeCounterValue(void) {
    return ulHighFrequencyTimerTicks;
}
```

调出统计：

```c
char buf[512];
vTaskGetRunTimeStats(buf);
printf("%s\n", buf);
```

### 8.2 堆水位

```c
size_t free = xPortGetFreeHeapSize();
size_t min_ever = xPortGetMinimumEverFreeHeapSize();
```

min_ever 是历史最低水位。如果它持续下降但 free 不变，说明有任务/队列/信号量泄漏——每次创建用完没销毁。

## 九、踩坑总结

1. **栈大小看 word**：STM32 是 4 字节/word，128 不是 128 字节是 512 字节
2. **printf 烧栈**：newlib 的 printf 在浮点路径下能吃掉 1KB 栈，给够
3. **ISR 里禁阻塞**：所有 IPC 调用带 FromISR 后缀，超时只能传 0
4. **保护资源用 Mutex**：Binary Semaphore 没有优先级继承
5. **static 创建防碎片**：生产环境尽量用 xTaskCreateStatic 和静态分配队列
6. **Tick 钩子里别干重活**：vApplicationTickHook 在 tick ISR 里调，干重活会拖累所有任务实时性
7. **任务名传可读字符串**：pcTaskName 在调试输出里能看到，名字起清楚省排查时间

## 十、小结

FreeRTOS 学透了不是背 API 列表，是理解它背后的抢占调度模型和 IPC 语义。每个任务都有自己的状态机（Running/Ready/Blocked/Suspended），状态切换的触发是 IPC 原语——队列满了 take 阻塞，give 唤醒。把这套模型想清楚，遇到死锁、优先级反转、栈溢出都能从机制上推。

实战里我把每个任务都用 static 创建，所有共享资源用 Mutex 保护，事件通知用 Binary Semaphore，跨任务消息用队列。这套约定执行下来，过去三年没再出过死锁 bug。$kb$,
  updated_at = now()
WHERE slug = 'emb-freertos-task-scheduler-ipc';

UPDATE kb_entries SET
  content = $kb$# Linux 命令行肌肉记忆清单：文本三剑客 + 进程/网络/磁盘排查（实战组合）

> 在服务器上排障，能肌肉记忆地敲出常用组合比翻文档快十倍。这一篇把日常最高频的命令组合抄成一张表——grep/sed/awk 三剑客、进程排查、网络排查、磁盘排查——全是处理博客后端出问题时反复用的命令。

## 一、文本处理三剑客：grep / sed / awk

### 1.1 grep：行过滤之王

最常用的就是 -E（扩展正则）+ -i（忽略大小写）+ -n（行号）+ -r（递归）。

```bash
# 在当前目录递归查关键字，带行号
grep -rn "TODO" --include="*.go" .

# 排除目录
grep -rn "panic" --include="*.go" --exclude-dir=vendor .

# 只看匹配的文件名
grep -rl "func main" .

# 反向匹配
grep -v "^#" /etc/ssh/sshd_config

# 多模式
grep -E "error|warn|fatal" /var/log/syslog
```

实战高密度组合——找 panic 但排除测试文件：

```bash
grep -rEn "panic\(.*\)" --include="*.go" --exclude="*_test.go" .
```

### 1.2 sed：流编辑器，正则替换主战场

sed 默认不对原文件修改，加 -i 才真改。**坑**：macOS 的 BSD sed -i 必须带参数（-i ''），Linux GNU sed 是 -i。脚本里跨平台要兼容：

```bash
# GNU
sed -i 's/foo/bar/g' file.txt
# BSD/macOS
sed -i '' 's/foo/bar/g' file.txt
```

常用：

```bash
# 替换
sed 's/old/new/g' file.txt

# 只打印匹配行（-n 抑制自动输出，p 打印）
sed -n '/ERROR/p' log.txt

# 删除空行
sed '/^$/d' file.txt

# 删除注释行
sed '/^\s*#/d' nginx.conf

# 第 5-10 行
sed -n '5,10p' file.txt

# 把整行替换成大写（GNU 扩展）
sed 's/.*/\U&/' file.txt
```

组合例子——批量改一组 YAML 文件里某个字段：

```bash
sed -i 's/^  port: 8080/  port: 9090/' services/*.yaml
```

### 1.3 awk：列处理之王

awk 按"列"工作，默认空格分列，$1、$2 是第 1、2 列，$0 是整行。

```bash
# 打印第 2 列
awk '{print $2}' file.txt

# 改分隔符
awk -F: '{print $1}' /etc/passwd

# 条件过滤
awk '$3 > 1000 {print $1, $3}' data.txt

# 总和
awk '{sum+=$1} END {print sum}' numbers.txt

# 行号
awk '{print NR": "$0}' file.txt
```

实战：从 nginx access log 里算每个 IP 的请求量 Top 10：

```bash
awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -rn | head -10
```

sort 是因为 uniq -c 只对相邻重复计数，必须先排序。

### 1.4 三剑客组合

找 nginx 报 500 的请求路径 Top 5：

```bash
grep ' 500 ' /var/log/nginx/access.log | awk '{print $7}' | sort | uniq -c | sort -rn | head -5
```

把所有 TODO(john): 替换成 TODO(jane):，但只在 *.java 里：

```bash
find . -name "*.java" -exec grep -l "TODO(john)" {} \; | xargs sed -i 's/TODO(john)/TODO(jane)/g'
```

## 二、进程排查

### 2.1 ps + grep：找进程

```bash
# 找某端口/进程名
ps -ef | grep nginx
ps aux | grep -E "java|spring"

# 按 CPU 占用排序
ps aux --sort=-%cpu | head

# 按内存排序
ps aux --sort=-%mem | head
```

更现代的 pgrep：

```bash
pgrep -af nginx
# -a 显示完整命令行
# -f 全命令行匹配（否则只匹配进程名）
```

### 2.2 top / htop / atop

```bash
# 单次
top -bn1 | head -30
# -b batch 模式（不交互），-n1 只跑 1 次
```

htop 交互界面好。**atop** 不仅能看 CPU/内存，还能看磁盘 IO 和网络，强烈推荐装上：

```bash
atop 1   # 每秒刷新一次
```

### 2.3 找僵尸进程

```bash
ps -ef | grep defunct
# 或
ps aux | awk '$8 == "Z"'
```

僵尸的父进程 kill 不掉子进程（孩子已经死了，但父进程没 wait 回收）。修办法：找父进程，让它回收，或 kill 父进程：

```bash
ps -o ppid= -p <zombie_pid>
kill -CHLD <ppid>     # 提示父进程回收
# 实在不行
kill <ppid>
```

### 2.4 看进程的资源占用趋势

```bash
# pidstat 1 = 每秒报告
pidstat -urdh 1
# -u CPU, -r 内存, -d IO, -h 全部合一
```

pidstat 比 top 强在能精准看单个 PID，且能跑持续数据。

### 2.5 找吃 CPU 的线程

Java 应用 100% CPU，但 top 看不到线程级别。要 -H：

```bash
top -H -p <pid>
# 或
ps -L -p <pid> -o tid,pcpu,comm
```

拿到线程 TID（十进制），转十六进制，去 JVM 的 jstack 输出里找 nid=0xXXX 对应的栈，定位到具体方法。

## 三、网络排查

### 3.1 端口与连接

```bash
# 看监听端口
ss -tlnp
# -t TCP, -l listening, -n 不解析名字, -p 显示进程

# 看所有连接
ss -tan | awk '{print $1}' | sort | uniq -c | sort -rn
# 输出 ESTAB / TIME-WAIT 等的数量分布
```

老 netstat 已被 ss 替代，但很多机器上还在用：

```bash
netstat -anp | grep ESTABLISHED | grep :443
```

### 3.2 TCP 状态分布排查

服务卡顿时第一个看的是 TIME-WAIT 堆积：

```bash
ss -tan | awk 'NR>1 {print $1}' | sort | uniq -c
```

如果 TIME-WAIT 几万条，端口要耗光。修办法：开 tcp_tw_reuse 或缩短 fin_timeout：

```bash
sysctl -w net.ipv4.tcp_tw_reuse=1
# 永久生效：写 /etc/sysctl.conf 然后 sysctl -p
```

### 3.3 抓包

```bash
# 抓某端口
tcpdump -i eth0 -nn port 443

# 抓某主机
tcpdump -i eth0 -nn host 1.2.3.4

# 写文件后续用 wireshark 分析
tcpdump -i eth0 -w /tmp/cap.pcap port 443 and host 1.2.3.4

# 看握手
tcpdump -i eth0 -nn 'tcp[tcpflags] & tcp-syn != 0'
```

-nn 双重 n：不解析 IP→主机名、不解析 port→服务名。少这两个 n 抓包会慢死。

### 3.4 traceroute / mtr

```bash
# 路径追踪
traceroute -n 8.8.8.8
mtr -n 8.8.8.8   # 持续刷新
```

mtr 比 traceroute 强在持续输出，能看出哪一跳丢包。

### 3.5 curl 调 HTTPS

```bash
# 详细握手过程
curl -v https://blog.example.com 2>&1 | grep -E "TLS|cipher|cert"

# 只看响应头
curl -sI https://blog.example.com

# 跟随 301 跳转
curl -sL https://example.com

# 指定 Host 解析
curl --resolve blog.example.com:443:1.2.3.4 https://blog.example.com
```

--resolve 在做 SSL 证书测试但 DNS 还没切换时是神器——直接打 IP，但 SNI 仍带正确域名。

### 3.6 nc / ncat 测试端口

```bash
# 测 TCP 端口连通
nc -zv 1.2.3.4 443

# 测 UDP（不一定准）
nc -zuv 1.2.3.4 53
```

-z zero IO（不发数据），-v 详细输出。

## 四、磁盘排查

### 4.1 df：看分区使用率

```bash
df -h
df -i   # 看 inode 使用率，小文件多了会爆 inode
```

df -i 经常被忘。一个日志目录上百万个 1KB 的小文件，分区大小还有空间但 inode 用光，写不进。

### 4.2 du：找大目录

```bash
du -sh /var/log/* | sort -hr | head
du -h --max-depth=2 /var 2>/dev/null | sort -hr | head
```

--max-depth=1 限制深度，省得输出炸屏幕。

### 4.3 找大文件

```bash
find /var -type f -size +500M -exec ls -lh {} \;
# 按修改时间排序，找最近变大的
find /var -type f -mtime -7 -size +100M -exec ls -lh {} \;
```

### 4.4 iotop / iostat：看磁盘 IO

```bash
# 实时看哪个进程在读写
iotop -o
# -o 只显示有 IO 的进程

# 看设备级 IO
iostat -xz 1
# -x 扩展统计，-z 跳过空闲设备，1 每秒刷新
```

iostat 的关键列：%util（设备利用率）、await（IO 平均延迟 ms）。%util 接近 100% 就是瓶颈。

### 4.5 lsof：谁在用文件/端口

```bash
# 谁在用某文件
lsof /var/log/syslog

# 谁在用某端口
lsof -i :443

# 谁在用删除但未释放的文件（du 看不到但 df 看到的空间）
lsof +L1
```

经典坑：df 看到磁盘满了，du 算起来文件总和远远不到——是有进程持有已删除文件的句柄。lsof +L1 找到它，重启/kill 该进程，空间立刻回收。

### 4.6 文件描述符

```bash
# 看进程的 fd
ls -l /proc/<pid>/fd | head
# 看数量
ls /proc/<pid>/fd | wc -l
```

服务跑久了 fd 涨到几千——多半是 socket 泄漏。看 /proc/<pid>/fd 里都是 socket 就定位到方向了。

## 五、组合命令实战

### 5.1 找最耗 CPU 的进程

```bash
ps -eo pid,pcpu,pmem,comm --sort=-pcpu | head -10
```

### 5.2 找监听 80 端口的进程

```bash
ss -tlnp | grep :80
# 或
lsof -i :80
```

### 5.3 看某个请求的服务端处理延迟

```bash
# nginx access log 第 10 列是 request_time
awk '{sum+=$10; print sum}' /var/log/nginx/access.log | tail -1
# 算 P99 略复杂，用 sort:
awk '{print $10}' access.log | sort -n | awk 'NR==int(NR*0.99){print}'
```

### 5.4 找最近 5 分钟新增的 ERROR 日志

```bash
find /var/log -mmin -5 -name "*.log" -exec grep -l ERROR {} \; | xargs grep -i error
```

### 5.5 一键 dump 当前服务器关键指标

```bash
echo "=== CPU ==="; uptime
echo "=== MEM ==="; free -h
echo "=== DISK ==="; df -h | grep -vE 'tmpfs|loop'
echo "=== TOP CPU ==="; ps -eo pid,pcpu,pmem,comm --sort=-pcpu | head -5
echo "=== TOP MEM ==="; ps -eo pid,pcpu,pmem,comm --sort=-pmem | head -5
echo "=== CONN ==="; ss -tan | awk 'NR>1{print $1}' | sort | uniq -c
echo "=== LISTEN ==="; ss -tlnp
```

这个组合我放在 ~/bin/snap.sh，每次 SSH 上去先跑一遍快速摸底。

## 六、踩坑总结

1. **BSD sed 和 GNU sed 的 -i 不兼容**：跨平台脚本要兼容，sed -i.bak 然后删 .bak 是个折中
2. **uniq 必须先 sort**：相邻去重，不是全表去重
3. **df vs du 差异**：差异在于已删除但被进程持有的文件，找 lsof
4. **tcpdump 慢**：忘了 -nn，每包都 DNS 解析
5. **TIME-WAIT 堆积**：高 QPS 短连接服务必踩，开 tcp_tw_reuse=1
6. **inode 用光**：日志分片文件太多，写不进但 df 看还有空间
7. **TIME_WAIT 不一定是问题**：高并发短连接是常态，只有端口耗尽才真要管
8. **top -H 看线程**：Java 100% CPU 但 top 看进程才 30%，要 -H 才能定位到线程
9. **jstack 看线程栈**：top -H 拿到 TID 转十六进制，jstack 找 nid=0xXXX

## 七、小结

服务器排障是个熟练工种，命令本身不复杂，难的是组合使用和知道什么时候用什么。这一篇清单里的命令组我都用了很多年，每次上线必带的几个组合：ss -tan | awk | sort | uniq -c 看连接分布，ps -eo ... --sort 找大头，grep ... | awk ... | sort | uniq -c | sort -rn | head 是高频日志分析的万能模板。

记熟了之后排查故障的速度会变成肌肉记忆——看到现象手就敲出对应组合，比打开浏览器搜文档快十倍。$kb$,
  updated_at = now()
WHERE slug = 'sw-linux-cli-muscle-memory';

UPDATE kb_entries SET
  content = $kb$# 本博客的微服务架构演进：Spring Cloud Gateway + 双服务 + Supabase + JWT

> 这一篇讲本博客后端从单体到微服务的演进历程，技术底座是什么、为什么这么选、JWT 鉴权怎么穿过整个调用链。不是 PPT 吹架构，是真实工程实现。

## 一、为什么从单体拆到微服务

最早博客是 Spring Boot 单体：一坨 controller + service + repository 都在一个 jar 里。功能少时没问题，痛点逐渐显现：

1. 一改前端调用要重发整个后端
2. 用户和论坛耦合——论坛加个新功能要回归测试用户登录
3. 数据库连接池占用集中在一个进程，雪崩时一个挂全挂
4. 发布节奏不一致——论坛一天能发 5 次，用户一周才改一次

于是按业务领域拆：用户相关独立成 user-service，论坛相关独立成 forum-service，外面套一层 Spring Cloud Gateway 做统一入口和路由。数据库选了 Supabase（PostgreSQL 托管），不再自维护。

## 二、整体架构

```
                    浏览器 / 移动端
                          │
                          ▼
                  Spring Cloud Gateway
                  (路由 / 限流 / JWT 解析)
                  /                 \
                 ▼                  ▼
           user-service        forum-service
              │                    │
              │                    │
              └────── Supabase (PostgreSQL) ──────┘
```

关键设计：
- **网关层无状态**：所有会话用 JWT，不维护服务端 session
- **服务间通过 OpenFeign 调用**：网关解析 JWT 后透传，下游服务用同一套 JWT 校验
- **数据库统一在 Supabase**：但每个服务用各自的 schema，避免表相互依赖

## 三、Spring Cloud Gateway

### 3.1 为什么用 Gateway 而不是 Nginx

Nginx 做路由也行，但 Spring Cloud Gateway 胜在：
1. **Java 生态原生**：用 Java 写 filter，跟业务同栈，复用 JWT 工具类
2. **断言和谓词**：Path、Host、Header 等内置断言，写起来比 nginx location 直观
3. **集成服务发现**：自动从 Nacos/Eureka 拿后端实例列表，nginx 要靠 consul-template 刷配置

### 3.2 路由配置

application.yaml：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**,/api/auth/**
          filters:
            - StripPrefix=0
        - id: forum-service
          uri: lb://forum-service
          predicates:
            - Path=/api/posts/**,/api/comments/**
          filters:
            - StripPrefix=0
```

lb:// 前缀表示走 LoadBalancer，从注册中心解析服务名到实例列表。

### 3.3 JWT 解析 Filter

网关里写个全局 Filter，所有进入的请求先解 JWT：

```java
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange, "missing token");
        }
        String token = auth.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            // 把用户 ID 透传给下游
            ServerHttpRequest req = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Roles", claims.get("roles", String.class))
                .build();
            return chain.filter(exchange.mutate().request(req).build());
        } catch (Exception e) {
            return unauthorized(exchange, "invalid token");
        }
    }

    @Override
    public int getOrder() { return -100; }
}
```

JwtUtil.parse 用 io.jsonwebtoken（jjwt）库做签名校验和过期检查：

```java
public Claims parse(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .build()
        .parseClaimsJws(token)
        .getBody();
}
```

签名算法用 HS256，密钥 32+ 字符。HS512 更安全但密钥要 64 字节。**不要用 RS256** 在小项目里——私钥分发麻烦，HS 系列足够，前提是密钥别泄漏。

### 3.4 路径白名单

登录、注册、健康检查不需要 JWT。用一个 Set 维护白名单：

```java
private static final Set<String> PUBLIC_PATHS = Set.of(
    "/api/auth/login",
    "/api/auth/register",
    "/actuator/health"
);

private boolean isPublic(String path) {
    return PUBLIC_PATHS.contains(path);
}
```

更优雅的方案是路径打标签（如 @Public 注解），但小项目用 Set 够。

## 四、user-service

### 4.1 职责

- 注册、登录、改密、改邮箱
- 发/校验邮箱验证码
- 维护用户基本信息（昵称、头像、签名）
- 签发 JWT

### 4.2 密码哈希

**严禁明文存密码**。用 BCrypt：

```java
@Autowired
private PasswordEncoder encoder;

public User register(String email, String rawPassword) {
    String hash = encoder.encode(rawPassword);  // 自带盐
    User u = new User();
    u.setEmail(email);
    u.setPassword(hash);
    return userRepo.save(u);
}

public boolean login(String email, String raw) {
    User u = userRepo.findByEmail(email);
    return u != null && encoder.matches(raw, u.getPassword());
}
```

BCrypt 自带盐，每次 encode 出来不同，但 matches 内部能正确比较。**不要**用 MD5/SHA1 加固定盐——GPU 暴破分分钟。

### 4.3 JWT 签发

```java
public String issue(User user) {
    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("roles", user.getRoles())   // ["user","admin"]
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000))  // 7 天
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();
}
```

坑：
1. **subject 一定放 user_id**：不要放 email，因为 email 可能改，user_id 不变
2. **过期别太长**：7 天够了。1 个月的 token 一旦泄漏攻击窗口巨大。配合 refresh token 更好，但小项目 JWT 长一点也行
3. **roles 写进 claim**：下游服务能直接拿，不用每次查库

### 4.4 退出登录

JWT 无状态——服务端没有"登出"。常用三种处理：
1. **客户端删 token**：简单但客户端不可信
2. **黑名单**：把退出前的 token 加 Redis 黑名单，每次校验查一遍。需要 Redis
3. **短期 token + refresh token**：access token 15 分钟，refresh 7 天，refresh 可在服务端吊销

博客用的是方案 1 + 方案 3：access token 1 小时，refresh 7 天，前端每次刷新。退出时前端删两个 token，refresh 在服务端打一个简单的内存过期表。

## 五、forum-service

### 5.1 职责

- 帖子的 CRUD
- 评论的 CRUD
- 点赞
- 分页查询、热门排序

### 5.2 鉴权模型

forum-service 不直接解 JWT，**信任网关透传的 X-User-Id 和 X-User-Roles 头**：

```java
@Component
public class AuthContext {
    public Long currentUserId(HttpServletRequest req) {
        String id = req.getHeader("X-User-Id");
        if (id == null) throw new ForbiddenException("no auth");
        return Long.valueOf(id);
    }
}
```

但这要求 forum-service **绝不能直接对外**——必须只通过网关访问。生产环境做法：
1. 内网 IP 白名单：网关 IP 才能连 forum-service 端口
2. 共享密钥头：网关加一个 X-Internal-Sign，forum-service 校验
3. mTLS：互相认证

博客现在用的是方案 1+2 组合。

### 5.3 服务间调用

forum-service 有时要查用户昵称（发帖时显示作者）。不直接读 users 表（数据库耦合），调 user-service：

```java
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/internal/users/{id}")
    UserDTO getById(@PathVariable Long id);
}
```

/internal/** 路径在 user-service 里加个 Filter，只接受内网调用。这样保证外部访问不到内部接口。

Feign + 注册中心自动负载均衡，单实例故障自动跳过。但 Feign 默认超时 10s 太长，要调短：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 1000
            read-timeout: 3000
```

### 5.4 缓存热门帖子

热门列表查询频繁，加 Redis 缓存：

```java
public Page<Post> hotList(int page) {
    String key = "hot:" + page;
    Page<Post> cached = redis.get(key, Page.class);
    if (cached != null) return cached;

    Page<Post> data = postRepo.findHot(page);
    redis.setex(key, 60, data);  // 60 秒过期
    return data;
}
```

缓存击穿用单飞模式：缓存 miss 时只放一个请求穿透到 DB，其他等待。Spring 没现成 API，自己用 ConcurrentHashMap<String, Future> 简单实现：

```java
private final Map<String, CompletableFuture<Page<Post>>> inFlight = new ConcurrentHashMap<>();

public Page<Post> hotList(int page) {
    String key = "hot:" + page;
    Page<Post> cached = redis.get(key, Page.class);
    if (cached != null) return cached;

    CompletableFuture<Page<Post>> f = inFlight.computeIfAbsent(key,
        k -> CompletableFuture.supplyAsync(() -> {
            Page<Post> d = postRepo.findHot(page);
            redis.setex(key, 60, d);
            return d;
        }).whenComplete((d, e) -> inFlight.remove(key)));
    return f.join();
}
```

## 六、Supabase 作为 PostgreSQL

### 6.1 为什么用 Supabase 不自建 PG

1. **托管免运维**：备份、监控、扩展都自动
2. **免费额度够小博客用**：500MB 存储、2 个项目
3. **自带 REST API**：可以直接 https://xxx.supabase.co/rest/v1/posts?select=id,title 查表，前端某些场景直接调，不走 Spring Boot

### 6.2 连接配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.<project>.supabase.co:5432/postgres
    username: postgres
    password: <password>
    hikari:
      maximum-pool-size: 5      # Supabase 默认 60 个连接上限
      connection-timeout: 5000
      idle-timeout: 600000
```

Supabase 直连模式的连接数有限，**一定要配 PgBouncer 通道**：

```yaml
url: jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?prepareThreshold=0
```

prepareThreshold=0 关掉 prepared statement 复用——PgBouncer 在 transaction 模式下会乱掉。

### 6.3 schema 隔离

```sql
CREATE SCHEMA user_service;
CREATE SCHEMA forum_service;

-- user-service 表
CREATE TABLE user_service.users (...);
-- forum-service 表
CREATE TABLE forum_service.posts (...);
```

应用层用 schema 前缀：

```java
@Entity
@Table(name = "users", schema = "user_service")
public class User {...}
```

这样两个服务用一个 DB 实例，但表完全隔离。**严禁跨 schema 外键**——一旦外键耦合，拆分时痛不欲生。

## 七、JWT 调用链全图

```
浏览器
  │
  │ 1. POST /api/auth/login {email, password}
  ▼
Gateway
  │
  │ 2. 路由匹配 /api/auth/** → user-service
  ▼
user-service
  │ 3. BCrypt.matches，签 JWT
  │ 4. 返回 {access_token, refresh_token}
  ▼
浏览器（持有 JWT）
  │
  │ 5. POST /api/posts {title, content}，Authorization: Bearer xxx
  ▼
Gateway
  │ 6. JwtAuthFilter.parse(token)
  │ 7. 注入 X-User-Id 头
  ▼
forum-service
  │ 8. AuthContext.currentUserId() 取 X-User-Id
  │ 9. 写库 + 调 user-service 取作者信息（带 X-User-Id 透传）
  ▼
返回响应
```

注意点：
- 网关做签名校验，下游只信任 X-User-Id，性能更高（不用重复解析）
- 服务间调用（如 forum→user）也要透传 X-User-Id，保持调用链用户上下文
- 内部接口（/internal/**）必须配 IP 白名单 + 共享密钥双保险

## 八、踩坑记录

### 8.1 JWT 在 query 参数里被日志泄漏

排查时把完整 URL 写进日志，刚好 token 在 query 里。生产事故。修法：
1. JWT 强制走 Header（Authorization）
2. 日志过滤掉 Authorization 头
3. 网关 access log 配置不记 query

```yaml
server:
  tomcat:
    accesslog:
      pattern: '%h %l %u %t "%m %U %H" %s %b'
      # %U 不含 query，%r 才含
```

### 8.2 Feign 调用超时雪崩

forum-service 调 user-service 慢，forum-service 线程池堆满，整个论坛挂了。修法：
1. Feign 超时设短（connect 1s，read 3s）
2. 用 Hystrix/Resilience4j 熔断：连续失败 N 次直接 fallback
3. 关键路径降级：拿不到用户昵称时显示"匿名用户"

```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {...}

@Component
public class UserClientFallback implements UserClient {
    public UserDTO getById(Long id) {
        return UserDTO.builder().id(id).nickname("匿名").build();
    }
}
```

### 8.3 Gateway 重启后路由丢失

Spring Cloud Gateway 的路由配置如果走注册中心动态发现，重启后第一次请求可能 miss。修法：
1. 启动完成后预热：ApplicationReadyEvent 触发一次 /actuator/health 自调
2. 用 Nacos 配置中心存路由，热更新

### 8.4 Supabase 连接数打满

HikariCP 配 50 个连接，Supabase 直连 60 上限，单服务没问题。但 user-service + forum-service 加一起 100 个就爆。要么换 PgBouncer 通道，要么把池调到 20。**生产环境一定要走 PgBouncer**。

### 8.5 schema 命名冲突

最早叫 user、forum，但 user 是 PG 的关键字，迁移时各种坑。改成 user_service、forum_service 后清净了。命名规则：用 <service>_service 这种下划线分隔的全小写。

## 九、监控与可观测性

### 9.1 日志

每个服务用 Logback，统一 JSON 输出，关键字段：
- trace_id：网关生成，下游透传
- user_id：从 X-User-Id 拿
- service：服务名
- latency_ms：请求耗时

trace_id 透传用 Spring Cloud Sleuth（已并入 Micrometer Tracing）：

```yaml
spring:
  cloud:
    sleuth:
      sampler:
        probability: 1.0   # 全采样，生产环境按需调
```

### 9.2 指标

每个服务暴露 Prometheus metrics：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  metrics:
    export:
      prometheus:
        enabled: true
```

Grafana 看板至少三个：
1. **QPS / 延迟分位（P50/P95/P99）**
2. **错误率**（5xx / 4xx 比率）
3. **资源水位**（CPU/MEM/连接池/DB 慢查询）

### 9.3 告警

阈值简单点：5xx 率 > 1% 持续 1 分钟告警，P99 > 1s 持续 2 分钟告警。工具用 Grafana Alerting 或 Uptime Kuma。

## 十、小结

本博客从单体拆到双服务 + 网关的微服务，不是为拆而拆——是发布节奏不一致和故障隔离需求驱动的。Spring Cloud Gateway 做统一鉴权入口，user-service 和 forum-service 各自独立部署、独立数据库 schema，Supabase 托管 PostgreSQL 省运维，JWT 串起整个无状态调用链。

这套架构对小团队/中小项目是个甜点组合：
- 简单：3 个 Spring Boot 应用 + 1 个托管 PG
- 可演进：再加服务只需注册到 Gateway 加路由
- 可独立扩展：高峰时只扩 forum-service 不动 user-service

微服务不是银弹，对更小的项目单体足够。但走到要拆分时，按领域拆服务、网关统一鉴权、JWT 透传用户上下文、共享托管 DB，这套模式可以照搬。$kb$,
  updated_at = now()
WHERE slug = 'sw-microservice-architecture-of-site';
