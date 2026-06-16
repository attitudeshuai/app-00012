# ToolShare - 社区工具共享箱

一个管理小区/楼栋公共工具箱的小系统。电钻、梯子、扳手等不常用工具由邻居共同维护，通过扫码或在线预约借用，记录使用与归还，降低每个家庭重复购买低频工具的成本。

## 功能亮点

- 🔧 **工具箱管理**：支持多工具箱管理，每个工具箱可存放多种工具
- 📦 **工具目录**：工具分类、状态管理（可用/借用中/维护中/损坏）
- 📋 **借用申请**：在线申请、审批、归还全流程管理
- 📝 **使用日志**：完整记录工具借用、归还、报修、维修历史
- 📊 **数据统计**：总览看板、趋势分析、分类统计

## 技术栈

- **后端框架**：Java Spring Boot 3.2 (Spring Web)
- **数据库**：MySQL 8.0
- **ORM**：Spring Data JPA + Hibernate
- **认证**：JWT (Spring Security)
- **API文档**：SpringDoc OpenAPI (Swagger)
- **定时任务**：Spring Scheduler
- **容器化**：Docker + Docker Compose

## 目录结构

```
app-12/
├── src/
│   ├── main/
│   │   ├── java/com/toolshare/
│   │   │   ├── controller/      # 控制器层
│   │   │   ├── service/         # 服务层
│   │   │   ├── repository/      # 数据访问层
│   │   │   ├── entity/          # 实体类
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── config/          # 配置类
│   │   │   ├── exception/       # 异常处理
│   │   │   ├── util/            # 工具类
│   │   │   └── ToolShareApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                    # 测试代码
├── docs/
│   └── functional_intro.md      # 功能说明文档
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## 快速启动

### 方式一：Docker Compose 启动（推荐）

1. 克隆并进入项目目录：
   ```bash
   git clone <repo-url>
   cd app-12
   ```

2. 启动服务：
   ```bash
   docker-compose up --build -d
   ```

3. 查看日志：
   ```bash
   docker-compose logs -f app
   ```

4. 验证服务健康：
   ```bash
   curl http://localhost:8082/actuator/health
   ```

### 方式二：本地开发运行

1. 确保本地已安装 JDK 17+ 和 Maven 3.6+
2. 确保本地 MySQL 8.0 已启动，并创建数据库：
   ```sql
   CREATE DATABASE toolshare;
   CREATE USER 'app_user'@'%' IDENTIFIED BY 'app_pass';
   GRANT ALL PRIVILEGES ON toolshare.* TO 'app_user'@'%';
   ```

3. 修改 `application.properties` 中的数据库连接配置，或设置环境变量

4. 启动应用：
   ```bash
   mvn spring-boot:run
   ```

## API 文档

启动服务后，访问以下地址查看 API 文档：

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8082/api-docs

## 测试方式

### Postman 测试集合

项目提供了完整的 Postman 测试集合 `postman_collection.json`，覆盖所有接口。

导入方式：
1. 打开 Postman
2. 点击 "Import"
3. 选择项目根目录下的 `postman_collection.json`
4. 配置环境变量 `baseUrl` 为 `http://localhost:8082`

### 测试账号

系统启动后会自动创建以下测试账号：

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | admin123 | 管理员账号 |
| zhangsan | 123456 | 普通用户 |
| lisi | 123456 | 普通用户 |

### 主要测试场景

1. 用户注册 / 登录
2. 工具箱 CRUD
3. 工具 CRUD
4. 借用申请流程（申请 → 审批 → 归还）
5. 使用日志查询
6. 统计数据查看
7. 权限控制验证

### 运行单元测试

```bash
mvn test
```

## 停止服务

```bash
docker-compose down -v
```

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可

MIT License
