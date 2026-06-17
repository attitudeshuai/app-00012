# 逾期处理链路分析

## 一、概述

本文档梳理了工具共享系统中逾期处理的完整链路，分析了定时任务中逾期通知与逾期记录更新策略的差异设计意图，并讨论了归还动作与调度任务时序相关的边界情况。

## 二、架构分层与核心组件

### 2.1 分层架构

逾期处理逻辑分布在以下层级：

| 层级   | 主要职责                                  | 核心文件 |
|--------|-------------------------------------------|----------|
| Entity | 数据模型定义                              | [BorrowRequest.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/entity/BorrowRequest.java)、[OverdueRecord.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/entity/OverdueRecord.java) |
| Repository | 数据访问层，定义查询方法                | [BorrowRequestRepository.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/repository/BorrowRequestRepository.java)、[OverdueRecordRepository.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/repository/OverdueRecordRepository.java) |
| Service | 业务逻辑处理                              | [OverdueRecordService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/service/OverdueRecordService.java)、[BorrowRequestService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/service/BorrowRequestService.java)、[ScanService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/service/ScanService.java) |
| Scheduler | 定时任务调度                            | [OverdueNotificationScheduler.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java) |

### 2.2 核心实体字段说明

**BorrowRequest（借用申请）** 中的逾期相关字段：
- `overdueNotified` (boolean)：是否已发送逾期通知，默认 false
- `dueSoonNotified` (boolean)：是否已发送即将到期通知，默认 false
- `expectedReturnDate` (LocalDate)：预计归还日期
- `actualReturnDate` (LocalDate)：实际归还日期
- `status` (BorrowRequestStatus)：申请状态（PENDING / APPROVED / REJECTED / RETURNED）

**OverdueRecord（逾期记录）** 中的核心字段：
- `borrowRequestId` (Long)：关联的借用申请ID，唯一约束
- `overdueDate` (LocalDate)：逾期起始日（预计归还日 + 1天）
- `overdueDays` (Integer)：逾期天数
- `resolved` (boolean)：是否已解决
- `resolvedAt` (LocalDateTime)：解决时间

## 三、定时任务执行流程

定时任务由 [OverdueNotificationScheduler](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java) 实现，使用 Cron 表达式 `0 0 9 * * ?`，即每天早上 9:00 执行。

### 3.1 整体执行顺序

```
checkBorrows()
  ├─ checkDueSoonBorrows()    // 1. 检查即将到期（前3天内）
  ├─ checkOverdueBorrows()    // 2. 检查已逾期
  └─ resolveReturnedOverdueRecords()  // 3. 处理已归还的逾期记录
```

### 3.2 检查即将到期（checkDueSoonBorrows）

查询条件：状态为 APPROVED、预计归还日在未来 1~3 天内、未发送过即将到期通知。

处理逻辑：
1. 分页查询符合条件的借用申请
2. 对每条记录，检查 `dueSoonNotified` 标志
3. 未通知过的，分别向借用人和工具所有者发送通知
4. 设置 `dueSoonNotified = true`
5. 批量保存更新

**关键特征**：通知只发一次，通过 `dueSoonNotified` 标志位去重。

### 3.3 检查已逾期（checkOverdueBorrows）

查询条件：状态为 APPROVED、预计归还日早于今天、实际归还日为空。

处理逻辑包含两部分：

**（1）逾期通知**
- 检查 `overdueNotified` 标志
- 未通知过的，分别向借用人和工具所有者发送逾期通知
- 设置 `overdueNotified = true`

**（2）逾期记录更新**
- 调用 `createOrUpdateOverdueRecord()`
- 若记录已存在则更新 `overdueDays`
- 若不存在则创建新的逾期记录
- 每次调度都会执行此操作

**关键特征**：
- 通知只发一次（受 `overdueNotified` 控制）
- 逾期记录每天更新（不受标志位控制）

### 3.4 处理已归还的逾期记录（resolveReturnedOverdueRecords）

查询条件：所有未解决（resolved = false）的逾期记录。

处理逻辑：
1. 分页查询所有未解决的逾期记录
2. 逐条检查对应的借用申请
3. 若 `actualReturnDate` 不为空，则标记为已解决
4. 设置 `resolved = true` 和 `resolvedAt = now()`

**关键特征**：兜底机制，确保已归还的逾期记录最终会被标记为已解决。

## 四、通知与记录更新策略差异分析

### 4.1 策略差异对比

| 维度       | 逾期通知                          | 逾期记录更新                      |
|------------|-----------------------------------|-----------------------------------|
| 频率       | 仅第一次逾期时发送一次            | 每天调度时都更新                  |
| 去重机制   | `borrowRequest.overdueNotified` 标志位 | 按 `borrowRequestId` 唯一约束，更新同一记录 |
| 数据性质   | 提醒性质，时效性强                | 统计/审计性质，需要持续准确      |
| 用户体验   | 避免重复打扰                      | 提供准确的逾期数据                |

### 4.2 设计意图分析

**通知只发一次的设计考虑：**

1. **用户体验**：避免每天重复发送相同内容的通知，造成信息骚扰
2. **成本控制**：减少通知发送量，降低系统负载和第三方推送成本
3. **触发式提醒**：通知的核心作用是"告知用户已逾期"这一事实，而非每日报告
4. **人工干预假设**：假设用户收到通知后会采取行动，不需要反复提醒

**逾期记录每天更新的设计考虑：**

1. **数据准确性**：逾期天数是动态增长的，需要保持最新
2. **统计需求**：逾期记录用于统计分析（如逾期时长分布），需要准确数据
3. **审计追溯**：作为正式记录，需要反映真实的逾期情况
4. **列表展示**：逾期记录列表页面需要展示当前逾期天数

## 五、归还动作处理链路

### 5.1 两个归还入口

系统中有两处可以触发归还操作：

#### 入口一：BorrowRequestService.updateBorrowStatus(RETURNED)

位于 [BorrowRequestService.java#L215-L241](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/service/BorrowRequestService.java#L215-L241)，由工具所有者手动确认归还。

处理步骤：
1. 校验权限（仅工具所有者）和状态（必须是 APPROVED）
2. 更新工具状态为 AVAILABLE
3. 设置 `actualReturnDate = today`
4. 创建工具日志
5. 发送归还确认通知
6. **同步标记逾期记录为 resolved**（如果存在）
7. 更新借用申请状态为 RETURNED

#### 入口二：ScanService.returnTools()

位于 [ScanService.java#L231-L357](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/service/ScanService.java#L231-L357)，通过扫码快速归还。

处理步骤与入口一类似，同样会：
- 设置 `actualReturnDate = today`
- **同步标记逾期记录为 resolved**（如果存在）
- 更新借用申请状态为 RETURNED

### 5.2 逾期记录 resolved 标记的双重保障

系统中有两条路径可以将逾期记录标记为已解决：

| 路径       | 触发时机          | 特点                                  |
|------------|-------------------|---------------------------------------|
| 归还时同步 | 归还操作执行时    | 实时性高，归还后立即标记              |
| 调度兜底   | 每天早上9点调度   | 兜底机制，确保最终一致性              |

两条路径都是幂等操作（检查 `resolved` 标志后才更新），不会产生冲突。

## 六、时序边界情况分析

本节分析归还动作与调度任务执行时序的各种组合，评估是否存在通知漏发、逾期天数不准或状态不一致等问题。

### 6.1 场景一：归还发生在调度之前（当天逾期但调度前归还）

**时间线：**
```
预计归还日：6月15日
6月16日 00:00 —— 逾期开始
6月16日 08:00 —— 用户归还工具
6月16日 09:00 —— 调度任务执行
```

**分析：**

1. **通知是否漏发**：是。
   - 调度任务执行时，`findOverdueBorrows` 查询条件包含 `actualReturnDate IS NULL`
   - 由于已归还，`actualReturnDate` 不为空，不会被查询到
   - 因此 `overdueNotified` 仍为 false，但永远不会触发通知了
   - 虽然从技术上看确实逾期了（超过了预计归还日的24点），但用户在短时间内就归还了

2. **逾期记录是否创建**：否。
   - 同上，调度时不会查询到该记录，所以不会创建逾期记录

3. **状态一致性**：一致。
   - 没有逾期记录，借用申请状态为 RETURNED，状态匹配

**问题评估**：
这是一个潜在问题。用户确实逾期了（哪怕只有几小时），但系统既没有发送通知，也没有创建逾期记录。从业务角度看，这种"短暂逾期"是否需要记录和通知，取决于产品策略。

### 6.2 场景二：归还发生在调度之后（逾期第一天，调度后归还）

**时间线：**
```
预计归还日：6月15日
6月16日 00:00 —— 逾期开始
6月16日 09:00 —— 调度任务执行
6月16日 14:00 —— 用户归还工具
```

**分析：**

1. **通知是否发送**：是，发送了一次。
   - 调度时检测到逾期，`overdueNotified` 为 false，发送通知
   - 设置 `overdueNotified = true`

2. **逾期记录是否创建**：是。
   - 调度时创建逾期记录，`overdueDays = 1`

3. **归还时处理**：
   - 设置 `actualReturnDate = today`（6月16日）
   - 同步标记逾期记录为 `resolved = true`

4. **逾期天数准确性**：准确。
   - 调度时 overdueDays = 1（6月15日到6月16日，差1天）
   - 实际归还日也是6月16日，逾期1天
   - 两者一致

5. **状态一致性**：一致。

**问题评估**：无问题。通知、记录、状态都正确。

### 6.3 场景三：逾期多日后归还

**时间线：**
```
预计归还日：6月15日
6月16日 09:00 —— 第1次调度，发通知，创建记录（overdueDays=1）
6月17日 09:00 —— 第2次调度，更新记录（overdueDays=2）
6月18日 14:00 —— 用户归还工具
```

**分析：**

1. **通知发送**：仅在第1天发送一次，后续不再重复发送。

2. **逾期记录更新**：每天调度时更新 `overdueDays`。
   - 6月16日调度后：overdueDays = 1
   - 6月17日调度后：overdueDays = 2
   - 6月18日归还时：overdueDays 还是 2（因为当天调度还没执行）

3. **逾期天数准确性**：有轻微偏差。
   - 调度时的 overdueDays = 2（截至6月17日）
   - 实际归还日是6月18日，实际逾期3天
   - 但由于当天调度还没执行，记录中是2天
   - 不过按"整天"计算的逻辑，6月18日归还，逾期天数确实是3天（15日到18日）

4. **状态一致性**：归还后标记为 resolved，状态一致。

**问题评估**：
存在轻微的数据延迟问题。在调度执行前归还，逾期天数可能比实际少1天。但这是批处理系统的固有特性，每天更新一次。

### 6.4 场景四：API查询时的逾期天数与逾期记录不一致

**现象：**

- 借用申请详情接口（BorrowRequestResponse）中的逾期天数是**实时计算**的：见 [BorrowRequestService.java#L339-L359](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012/src/main/java/com/toolshare/service/BorrowRequestService.java#L339-L359) 的 `populateOverdueFields` 方法
- 逾期记录列表接口（OverdueRecordResponse）中的逾期天数是**调度快照**：直接使用数据库中的 `overdueDays` 字段

**影响：**

在两次调度之间的时间段内，用户可能看到：
- 借用详情页显示"逾期3天"
- 逾期记录页显示"逾期2天"

**问题评估**：
这是一个数据一致性问题，但不严重。两个页面的数据来源不同：
- 借用详情：实时计算，用于展示当前状态
- 逾期记录：历史快照，用于统计和审计

如果用户对数据一致性要求高，可能会产生困惑。

### 6.5 场景五：调度任务执行过程中发生归还

**时间线：**
```
6月16日 09:00:00 —— 调度任务开始执行
6月16日 09:00:05 —— checkOverdueBorrows 处理某条记录，发送通知，创建逾期记录
6月16日 09:00:06 —— 用户发起归还操作（并发）
6月16日 09:00:07 —— 归还操作设置 actualReturnDate，标记逾期记录为 resolved
6月16日 09:00:10 —— resolveReturnedOverdueRecords 执行，再次确认标记为 resolved
```

**分析：**

由于整个 `checkBorrows()` 方法标注了 `@Transactional`，而归还操作也是 `@Transactional`，需要考虑事务隔离级别。

但从业务逻辑看：
1. 通知已发送（合理，因为归还时确实已经逾期了）
2. 逾期记录已创建且被标记为 resolved（最终状态一致）
3. 两次 resolved 标记都是幂等的，不会有问题

**问题评估**：无实质问题。通知已发送是合理的（因为确实逾期过），状态最终一致。

## 七、总结与问题汇总

### 7.1 现有设计的优点

1. **通知去重机制清晰**：通过 `overdueNotified` 和 `dueSoonNotified` 标志位，确保通知只发一次，避免打扰用户
2. **双重 resolved 保障**：归还时同步标记 + 调度兜底标记，确保最终一致性
3. **分层清晰**：调度层负责定时检查，服务层负责业务逻辑，职责分明
4. **幂等操作**：多处检查后再更新的模式，避免重复操作导致的问题

### 7.2 潜在问题

| 问题编号 | 问题描述                                              | 影响程度 | 场景                  |
|----------|-------------------------------------------------------|----------|-----------------------|
| P1       | 调度前归还的短暂逾期，既不发通知也不创建记录           | 中       | 场景一                |
| P2       | 逾期记录的 overdueDays 有延迟（最多1天）              | 低       | 场景三                |
| P3       | 借用详情页与逾期记录页的逾期天数可能不一致             | 低       | 场景四                |

### 7.3 设计决策的权衡思考

**关于"通知只发一次"：**

- 优点：用户体验好，不骚扰
- 缺点：如果用户忽略了第一次通知，后续不会再有提醒
- 权衡：目前的设计偏向"不打扰"。如果业务需要，可以考虑增加多级提醒（如逾期第1天、第3天、第7天各发一次）

**关于"逾期记录每天更新"：**

- 优点：数据相对准确，满足统计和展示需求
- 缺点：存在最多1天的延迟，且与实时计算的数据可能不一致
- 权衡：批处理 vs 实时计算的经典权衡。目前的设计在性能和准确性之间取得了平衡

**关于"短暂逾期不记录"：**

- 优点：避免"几小时逾期"产生不良记录，更人性化
- 缺点：数据不完整，所有逾期都应该有记录
- 权衡：取决于业务对"逾期"的定义严格程度。如果逾期定义是"超过预计归还日的0点"，那么几小时也算逾期；如果定义是"超过一整天"，那当天归还可以不算逾期

---

*文档基于现有代码分析生成，未修改任何 Java 源代码。*
