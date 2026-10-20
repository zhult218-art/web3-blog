# 反编译源码清单与重写计划

> 最后更新：2026-09-20 · 当前状态：**已全部重写完毕，0 个文件残留 CFR 反编译产物**

## 背景

2026-09-17 的体检发现 `backend/**` 下有约 **100 个 Java 文件**头部含
`Decompiled with CFR 0.152` 注释，说明这些文件不是项目原始源码，而是从已构建的 jar
用 [CFR](https://www.benf.org/other/cfr/) 反编译的产物。其中
**resource-service / software-service** 两个服务 100% 反编译，所有 Java 文件均为
CFR 输出；体检报告 §5.1 记录了该问题。

反编译产物的问题：

1. **可读性差**：CFR 会输出冗长的 `equals/hashCode/toString` 全字段比对实现（例如
   `admin-service/.../entity/OperationLog.java` 原为 244 行），代码量是 Lombok `@Data`
   注解的 10 倍以上。
2. **元数据丢失**：原作者注释、Lombok 注解、泛型边界全部丢失，剩下大量 `(Object)` 强转、
   `for (Object o : list)` + 手动强转循环。
3. **潜在风险**：反编译可能引入微妙的行为差异（如 `@TableLogic` 是否正确恢复、
   `equals` 中字段顺序敏感性）。
4. **维护成本高**：修改任一字段都要手动同步 equals/hashCode/toString，极易遗漏。

## 重写进度

| 阶段 | 范围 | 状态 |
|---|---|---|
| Phase 1 | `admin-service/entity/OperationLog.java`（样板：244 行 → 30 行 `@Data`） | ✅ 2026-09-17 |
| Phase 2 | `common-core`（PageResult / BaseService / ApiResponse）、`common-util/JsonUtils`、`common-security` 6 个文件 | ✅ |
| Phase 3 | 各服务 `vo/`、`dto/`、`entity/`（约 60 个，Lombok 化） | ✅ |
| Phase 4 | `resource-service` / `software-service` 全量（Controller / Service / Mapper） | ✅ 已无反编译头残留 |
| 收尾 | `admin-service/service/AdminService.java`（清理反编译头注释、未使用的 `Wrapper` 导入、`(Object)` 强转与强转循环，改回泛型与菱形语法） | ✅ 2026-09-20 |

反编译历史不再逐个文件保留在源码注释里，统一记录在本文件。

## 检测命令

```bash
# 应返回 0：列出仍含反编译头的文件
grep -r "Decompiled with CFR" backend --include="*.java" -l
```

## 编译校验

重写后必须能通过编译（反编译产物曾长期掩盖真实编译错误）：

```bash
cd backend
mvn -DskipTests -B compile
```