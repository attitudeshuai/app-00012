# 功能说明

## 1. 业务背景与解决的问题

### 业务背景
在社区生活中，许多工具（如电钻、梯子、扳手等）使用频率很低，但每个家庭都可能偶尔需要。重复购买不仅浪费金钱，还占用存储空间。

### 解决的问题
- 降低家庭工具购买成本
- 提高工具的使用效率
- 促进邻里互助与交流
- 建立工具维护与共享机制

## 2. 用户角色与核心用例

### 用户角色
1. **普通用户**
   - 浏览工具箱和工具
   - 提交借用申请
   - 查看自己的借用记录
   - 发布/管理自己的工具

2. **工具箱管理员**
   - 创建和管理工具箱
   - 管理工具箱内的工具
   - 审批借用申请
   - 记录工具使用日志

### 核心用例
| 用例 | 参与者 | 说明 |
|------|--------|------|
| 用户注册 | 访客 | 创建新账号 |
| 用户登录 | 注册用户 | 登录系统获取Token |
| 浏览工具 | 所有用户 | 查看可用工具列表和详情 |
| 申请借用 | 注册用户 | 提交工具借用申请 |
| 审批申请 | 工具所有者 | 批准或拒绝借用申请 |
| 归还工具 | 工具所有者 | 确认工具归还 |
| 发布工具 | 注册用户 | 将个人工具加入共享 |
| 管理工具箱 | 管理员 | 创建和维护工具箱 |
| 查看统计 | 注册用户 | 查看平台数据统计 |

## 3. 功能模块详细说明

### 3.1 用户认证模块
- 用户注册：用户名、邮箱、密码
- 用户登录：支持用户名或邮箱登录
- JWT Token 鉴权
- 个人信息查看与修改

### 3.2 工具箱管理模块
- 工具箱增删改查
- 支持分页、搜索、排序
- 工具箱状态管理（启用/停用）
- 权限控制：仅管理员可操作

### 3.3 工具管理模块
- 工具增删改查
- 工具分类管理
- 工具状态流转
- 支持分页、搜索、筛选
- 权限控制：仅所有者可操作

### 3.4 借用申请管理模块
- 借用申请创建与查询
- 申请状态流转：待审核 → 已批准/已拒绝 → 已归还
- 借用时间管理
- 权限控制：申请人可查看/删除待审核申请，所有者可审批

### 3.5 使用日志管理模块
- 工具使用记录查询
- 支持按工具、用户、操作类型筛选
- 自动记录借用、归还等操作
- 支持手动添加日志

### 3.6 统计与搜索模块
- 总览统计：用户数、工具箱数、工具数、申请数等
- 工具状态分布
- 工具分类统计
- 借用趋势分析
- 操作类型统计

## 4. 数据库 ER 图文字描述

### 表关系

```
Users (1) ────── (N) ToolBoxes (管理)
  │
  │ (1) ──────── (N) Tools (拥有)
  │                   │
  │                   │ (1) ───── (N) BorrowRequests (申请)
  │                   │                │
  │ (1) ────────────────────── (N) BorrowRequests (发起)
  │
  │ (1) ──────── (N) ToolLogs (操作)
                      │
Tools (1) ────────────┘ (产生)
```

### 表详细说明

#### Users（用户表）
- 主键：id
- 唯一约束：username, email
- 主要字段：username, email, passwordHash, avatar, createdAt, updatedAt

#### ToolBoxes（工具箱）
- 主键：id
- 外键：managerId → Users.id
- 主要字段：name, location, managerId, code, isActive, createdAt

#### Tools（工具）
- 主键：id
- 外键：boxId → ToolBoxes.id, ownerId → Users.id
- 主要字段：name, category, status, description, purchaseDate, ownerId, boxId, createdAt
- 状态枚举：AVAILABLE, BORROWED, MAINTENANCE, BROKEN

#### BorrowRequests（借用申请）
- 主键：id
- 外键：toolId → Tools.id, requesterId → Users.id
- 主要字段：startDate, expectedReturnDate, actualReturnDate, status, remark, createdAt
- 状态枚举：PENDING, APPROVED, REJECTED, RETURNED

#### ToolLogs（使用日志）
- 主键：id
- 外键：toolId → Tools.id, userId → Users.id
- 主要字段：action, description, createdAt
- 操作类型：BORROW, RETURN, REPORT, REPAIR

## 5. 关键业务规则

### 5.1 状态流转规则

#### 工具状态流转
- **AVAILABLE（可用）** → BORROWED（借用中）：申请被批准时
- **BORROWED（借用中）** → AVAILABLE（可用）：工具被归还时
- **AVAILABLE（可用）** → MAINTENANCE（维护中）：手动设置
- **MAINTENANCE（维护中）** → AVAILABLE（可用）：维护完成
- **任意状态** → BROKEN（损坏）：报损坏时
- **BROKEN（损坏）** → AVAILABLE（可用）：修复完成

#### 借用申请状态流转
- **PENDING（待审核）** → APPROVED（已批准）：工具所有者批准
- **PENDING（待审核）** → REJECTED（已拒绝）：工具所有者拒绝
- **APPROVED（已批准）** → RETURNED（已归还）：工具所有者确认归还

### 5.2 权限规则

- 所有用户需登录后才能访问系统接口（注册、登录接口除外）
- 工具箱只能由其管理员修改/删除
- 工具只能由其所有者修改/删除/修改状态
- 借用申请只能由申请人创建和删除（仅待审核状态）
- 借用申请的审批只能由工具所有者操作
- 借用归还确认只能由工具所有者操作
- 使用日志只能由创建者修改/删除

### 5.3 时间计算逻辑

- 借用开始日期不能晚于预计归还日期
- 实际归还日期在确认归还时自动设为当天
- 借用天数 = 预计归还日期 - 开始日期 + 1

## 6. 接口调用示例

### 示例 1：用户注册

**请求**：
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "123456"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 4,
      "username": "testuser",
      "email": "test@example.com",
      "avatar": null,
      "createdAt": "2024-01-15T10:00:00",
      "updatedAt": "2024-01-15T10:00:00"
    }
  }
}
```

### 示例 2：获取工具列表

**请求**：
```http
GET /api/tools?page=0&size=10&status=AVAILABLE&sortBy=createdAt&sortDir=desc
Authorization: Bearer <token>
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "boxId": 1,
        "boxName": "1号楼工具箱",
        "name": "电钻",
        "category": "电动工具",
        "status": "AVAILABLE",
        "description": "博世冲击钻，配多种钻头",
        "ownerId": 1,
        "ownerName": "admin",
        "createdAt": "2024-01-10T08:00:00"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 示例 3：创建借用申请

**请求**：
```http
POST /api/borrowrequests
Content-Type: application/json
Authorization: Bearer <token>

{
  "toolId": 2,
  "startDate": "2024-01-20",
  "expectedReturnDate": "2024-01-25",
  "remark": "家里装修需要用一下梯子"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 5,
    "toolId": 2,
    "toolName": "梯子",
    "requesterId": 2,
    "requesterName": "zhangsan",
    "startDate": "2024-01-20",
    "expectedReturnDate": "2024-01-25",
    "actualReturnDate": null,
    "status": "PENDING",
    "remark": "家里装修需要用一下梯子",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```
