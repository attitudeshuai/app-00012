# 逾期处理链路分析

## 1. 概述

本文档梳理了工具共享系统中逾期处理的完整链路，分析了定时任务中「逾期通知」与「逾期记录更新」策略差异的设计意图，并探讨了归还动作与调度任务时序关系可能带来的边界问题。

---

## 2. 核心组件与分层架构

### 2.1 涉及的主要模块

| 层级 | 关键类/文件 | 职责 |
|------|------------|------|
| 调度层 | [OverdueNotificationScheduler.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java) | 定时触发逾期检查 |
| 服务层 | [BorrowRequestService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/BorrowRequestService.java) | 借用申请管理（含归还逻辑） |
| 服务层 | [OverdueRecordService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/OverdueRecordService.java) | 逾期记录管理 |
| 服务层 | [ScanService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/ScanService.java) | 扫码借用/归还 |
| 服务层 | [NotificationService.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/NotificationService.java) | 通知发送 |
| 实体层 | [BorrowRequest.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/entity/BorrowRequest.java) | 借用申请实体 |
| 实体层 | [OverdueRecord.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/entity/OverdueRecord.java) | 逾期记录实体 |
| 仓储层 | [BorrowRequestRepository.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/repository/BorrowRequestRepository.java) | 借用申请数据访问 |
| 仓储层 | [OverdueRecordRepository.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/repository/OverdueRecordRepository.java) | 逾期记录数据访问 |

---

## 3. 定时任务执行流程

### 3.1 触发时机

调度任务通过 `@Scheduled(cron = "0 0 9 * * ?")` 注解配置，**每天早上 9:00 执行一次**。整个方法标注了 `@Transactional`，保证操作的原子性。

入口方法：[checkBorrows()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L48-L53)

### 3.2 执行顺序

调度任务按以下三步顺序执行：

```
1. checkDueSoonBorrows()    —— 检查即将到期的借用（提前3天提醒）
2. checkOverdueBorrows()    —— 检查已逾期的借用（发通知 + 更新逾期记录）
3. resolveReturnedOverdueRecords()  —— 标记已归还的逾期记录为已解决
```

---

## 4. 策略差异分析：通知 vs 逾期记录

### 4.1 逾期通知策略：只发一次

**核心逻辑**位于 [checkOverdueBorrows()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L104-L152) 方法中：

```java
if (!borrowRequest.isOverdueNotified()) {
    // 发送逾期通知（借用人 + 工具所有者各一条）
    notificationService.createNotification(...);
    borrowRequest.setOverdueNotified(true);  // 标记为已通知
    toUpdate.add(borrowRequest);
}
```

**控制机制**：
- [BorrowRequest.java](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/entity/BorrowRequest.java#L41-L42) 中的 `overdueNotified` 字段（boolean，默认 false）
- 通知发送后立即设为 `true`，后续调度周期不再重复发送

### 4.2 逾期记录更新策略：每天更新

**核心逻辑**位于 [createOrUpdateOverdueRecord()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L154-L172) 方法中：

```java
if (existingOpt.isPresent()) {
    record = existingOpt.get();
    record.setOverdueDays(overdueDays);  // 更新逾期天数
} else {
    // 创建新的逾期记录
    record = new OverdueRecord();
    record.setOverdueDays(overdueDays);
    record.setResolved(false);
}
overdueRecordRepository.save(record);
```

**行为特点**：
- 每次调度都会执行，**无条件更新** `overdueDays` 字段
- 逾期天数 = `今天 - 预计归还日期`
- 记录一旦创建就持续更新，直到被标记为 `resolved = true`

### 4.3 设计意图推测

| 维度 | 通知（一次） | 逾期记录（每日更新） |
|------|-------------|---------------------|
| **业务目标** | 提醒用户，避免打扰 | 记录事实，供统计/展示 |
| **用户体验** | 每天发通知会造成骚扰，一次提醒足够 | 需要反映当前真实逾期时长 |
| **数据用途** | 触发用户行动 | 信用评估、逾期统计、管理报表 |
| **幂等性要求** | 必须保证不重复发送 | 每天更新覆盖即可，天然幂等 |

---

## 5. 归还动作链路

系统中有两条归还路径，都会触发逾期记录的结清：

### 5.1 路径一：手动确认归还

位于 [BorrowRequestService.updateBorrowStatus()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/BorrowRequestService.java#L215-L241)：

```java
case RETURNED:
    // 1. 更新工具状态为可用
    tool.setStatus(ToolStatus.AVAILABLE);
    // 2. 设置实际归还日期
    borrowRequest.setActualReturnDate(LocalDate.now());
    // 3. 记录操作日志
    toolLogService.createLogInternal(...);
    // 4. 发送归还通知
    notificationService.createNotification(...);
    // 5. 结清逾期记录
    overdueRecordRepository.findByBorrowRequestId(borrowRequest.getId()).ifPresent(record -> {
        if (!record.isResolved()) {
            record.setResolved(true);
            record.setResolvedAt(LocalDateTime.now());
            overdueRecordRepository.save(record);
        }
    });
    break;
```

### 5.2 路径二：扫码归还

位于 [ScanService.returnTools()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/ScanService.java#L232-L357)，逻辑与手动归还基本一致，同样会：
1. 设置 `actualReturnDate` 为当天
2. 更新借用状态为 `RETURNED`
3. 立即结清关联的逾期记录

### 5.3 路径三：调度任务被动结清

调度任务的第三步 [resolveReturnedOverdueRecords()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L174-L202) 作为兜底：
- 遍历所有未结清的逾期记录
- 检查对应的借用申请是否已有 `actualReturnDate`
- 如有，标记为 `resolved = true`

> **注意**：由于归还动作本身已主动结清逾期记录，这一步更多是容错兜底，处理那些可能因异常导致未结清的情况。

---

## 6. 时序边界问题分析

### 6.1 关键时间节点

以「预计归还日为 6 月 15 日」为例，时间轴如下：

```
6/14        6/15        6/16        6/17        6/18
  |           |           |           |           |
  |       预计归还日    逾期第1天   逾期第2天   逾期第3天
  |                       9:00        9:00        9:00
  |                       调度        调度        调度
  |                       任务        任务        任务
```

### 6.2 场景一：归还发生在调度任务执行前

**假设**：6 月 16 日 8:30 归还（逾期第 1 天，调度任务 9:00 才执行）

**执行路径**：
1. 归还操作设置 `actualReturnDate = 6/16`，状态 = RETURNED
2. 归还操作立即将逾期记录标记为 `resolved = true`（假设有前一天的记录？不，6/16 才是逾期第一天，调度还没跑过，所以**还没有逾期记录**）
3. 9:00 调度任务执行：
   - `findOverdueBorrows` 查询条件包含 `actualReturnDate IS NULL`，因此**不会查到这条记录**
   - 既不会发通知，也不会创建/更新逾期记录

**潜在问题**：
- ❓ **逾期通知漏发**：用户确实逾期了（哪怕只有几个小时），但从未收到逾期通知
- ❓ **逾期记录缺失**：这次短暂逾期可能不会留下任何记录

**是否属于问题**：取决于业务定义。如果「逾期」以「天」为粒度计算，且当天归还视为可接受，那么这是预期行为；如果需要精确记录所有逾期，哪怕只有几小时，那就存在漏记。

### 6.3 场景二：归还发生在调度任务执行后

**假设**：6 月 17 日 14:00 归还（逾期第 2 天，调度任务已于当天 9:00 执行）

**执行路径**：
1. 9:00 调度任务执行：
   - 查询到该借用已逾期 2 天（6/17 - 6/15 = 2 天）
   - 假设是首次逾期，发送逾期通知（通知内容写的是「逾期 2 天」）
   - 更新逾期记录，`overdueDays = 2`
2. 14:00 用户归还：
   - 设置 `actualReturnDate = 6/17`
   - 立即标记逾期记录为 `resolved = true`
   - 但 `overdueDays` 仍为 **2**（调度任务早上更新的值）

**潜在问题**：
- ❓ **逾期天数与实际不符**：记录中逾期天数为 2 天，但实际归还日就是 6/17，从业务角度看可能是 2 天也可能是 1.xx 天，取决于计算口径
- ℹ️ 这个问题不严重，因为：
  - 逾期天数通常按「天」为粒度（不计小时）
  - 记录结清后，逾期天数更多是历史参考
  - 实际业务中，6/15 到期，6/17 归还，算 2 天逾期是合理的（6/16、6/17 各算一天）

### 6.4 场景三：逾期第一天刚好当天归还

**假设**：预计归还日 6/15，用户 6/16 早上 8:00 归还（严格来说逾期了，但当天就还了）

**结果**：
- 调度任务 9:00 才跑，归还时还没通知，也没逾期记录
- 最终：**没有逾期通知，没有逾期记录**
- 相当于「宽限一天」的效果

### 6.5 场景四：归还日恰好是调度日，且已产生过逾期记录

**假设**：逾期多日后，在某一天调度任务执行后归还

**结果**：
- 调度任务已更新了当天的逾期天数
- 归还时结清记录，逾期天数停留在调度更新的值
- **不会出现状态不一致**：归还操作主动结清 + 调度任务兜底结清，双重保证

---

## 7. 数据一致性分析

### 7.1 通知与状态的一致性

**问题**：通知发送了，但用户可能在同一天内归还，导致通知「过时」

**分析**：
- 通知是「已发生事实」的告知，不是「当前状态」的实时反映
- 用户收到通知后去归还，属于正常流程
- 通知内容中的「逾期 N 天」是发送时的状态，不会动态更新
- 这是所有异步通知系统的共性，不算设计缺陷

### 7.2 逾期记录与借用状态的一致性

**问题**：借用已归还，但逾期记录还显示未结清？

**分析**：
- 两条归还路径都**主动**结清逾期记录（在同一事务内）
- 调度任务还有**兜底**结清逻辑
- 双重保障，一致性较好
- 极端情况（归还事务提交后、逾期记录结清前系统崩溃）可能出现短暂不一致，但下一次调度会修复

### 7.3 逾期天数的准确性

**问题**：逾期天数只在每天 9 点更新一次，不是实时的

**分析**：
- 在查询接口中，[BorrowRequestService.toResponse()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/BorrowRequestService.java#L312-L337) 是**实时计算**逾期天数的（通过 `populateOverdueFields` 方法）
- 但 [OverdueRecord](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/entity/OverdueRecord.java) 表中的 `overdueDays` 是**每天更新一次**的快照值
- 两者可能不一致：实时查询总是准的，逾期记录表里的数据可能滞后最多 24 小时

**影响**：
- 前端展示借用列表时，逾期天数是准确的（实时计算）
- 逾期记录详情页展示的 `overdueDays` 可能不是最新的（从 OverdueRecord 表读取）
- 统计报表如果基于 OverdueRecord 表，统计截止到昨天的数据是准的，当天的数据可能不准

---

## 8. 设计评价总结

### 8.1 设计优点

1. **职责分离清晰**：通知负责「提醒」，记录负责「记账」，两者策略不同是合理的
2. **通知幂等性有保障**：通过 `overdueNotified` 标记避免重复发送
3. **归还结清双重保障**：主动结清 + 调度兜底，数据一致性较好
4. **实时查询与离线记录分离**：借用查询实时计算逾期天数，保证用户看到的总是准确的

### 8.2 潜在风险点

| 风险点 | 影响程度 | 说明 |
|--------|---------|------|
| 逾期第一天归还无通知、无记录 | 低 | 相当于自动宽限 1 天，业务上可能可接受 |
| OverdueRecord 表中逾期天数非实时 | 中 | 如果用于统计报表，需注意统计时间点 |
| 通知内容中的逾期天数可能过时 | 低 | 用户收到通知时可能已归还，属正常现象 |

### 8.3 优化建议（仅供参考，非必须）

如果需要更精确的逾期管理，可考虑：

1. **归还时回写准确逾期天数**：在归还操作中，根据 `actualReturnDate` 和 `expectedReturnDate` 重新计算并更新逾期记录的 `overdueDays`，确保结清时的天数是最终准确值。

2. **逾期记录创建时机提前**：如果业务要求「只要过了预计归还日 0 点就算逾期」，可以考虑在归还时检查是否逾期，若逾期则主动创建逾期记录（即使调度还没跑）。

3. **增加逾期天数计算口径说明**：明确逾期天数是「自然日差」还是「24 小时为一天」，避免业务理解偏差。

---

## 9. 关键代码索引

| 功能点 | 位置 |
|--------|------|
| 调度任务入口 | [OverdueNotificationScheduler.checkBorrows()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L48-L53) |
| 逾期检查主逻辑 | [OverdueNotificationScheduler.checkOverdueBorrows()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L104-L152) |
| 逾期记录创建/更新 | [OverdueNotificationScheduler.createOrUpdateOverdueRecord()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L154-L172) |
| 逾期记录结清（调度兜底） | [OverdueNotificationScheduler.resolveReturnedOverdueRecords()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/scheduler/OverdueNotificationScheduler.java#L174-L202) |
| 手动确认归还 | [BorrowRequestService.updateBorrowStatus()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/BorrowRequestService.java#L215-L241) |
| 扫码归还 | [ScanService.returnTools()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/ScanService.java#L232-L357) |
| 实时逾期天数计算 | [BorrowRequestService.populateOverdueFields()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/service/BorrowRequestService.java#L339-L359) |
| 逾期查询 SQL | [BorrowRequestRepository.findOverdueBorrows()](file:///d:/charles/program/ai/apps/02.work%20session/session-gsb0617/source%20code/app-00012/app-00012-jupiter/src/main/java/com/toolshare/repository/BorrowRequestRepository.java#L51-L56) |
