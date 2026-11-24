# BookStore 书店系统

一个前后端分离的网上书店示例项目，后端 Spring Boot + MySQL，前端 React + Vite。



---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2.4 · Spring Web · Spring Data JPA · Spring Security · Spring Session |
| 数据库 | MySQL 8（表结构由 `schema.sql` 维护） |
| 前端 | React 18 · React Router 6 · Ant Design 5 · Vite 5 |
| AI 能力 | DeepSeek Chat API（Function Calling 智能客服 · ReAct 导购 Agent · 政策知识库 RAG） |

## 二、目录结构

```
BookStore/
├── bookstore-backend/                 # Spring Boot 后端（端口 8080）
│   ├── src/main/java/com/bookstore/backend/
│   │   ├── controller/                # 图书 / 购物车 / 订单 / 用户 / 统计
│   │   ├── service/                   # 业务逻辑（接口 + 实现分离）
│   │   ├── repository/                # Spring Data JPA 仓储
│   │   ├── entity/ dto/               # 实体与视图对象
│   │   ├── ai/                        # 作业2：Function Calling 智能客服
│   │   ├── agent/                     # 作业3：ReAct 导购 Agent
│   │   ├── rag/                       # 作业3：政策知识库 RAG
│   │   ├── config/ util/              # 跨域、数据初始化、鉴权工具
│   │   └── ...
│   └── src/main/resources/
│       ├── application.properties     # 配置（密钥请用环境变量注入）
│       ├── schema.sql                 # 建表 + 初始书籍数据
│       └── policy/store-policy.md     # 政策知识库原文
├── bookstore-frontend/                # React 前端（端口 3000，/api 代理到 8080）
│   └── src/{pages,components,context,api,styles}/
└── *.md                               # 课程笔记与设计文档
```

## 三、运行前配置

仓库中**不包含任何真实密钥**，以下两项必须通过环境变量注入。

| 变量 | 用途 | 示例（PowerShell） |
|---|---|---|
| `DB_PASSWORD` | MySQL `root` 账号密码 | `$env:DB_PASSWORD="你的数据库密码"` |
| `DEEPSEEK_API_KEY` | DeepSeek 大模型 API Key（作业2/3 的 AI 功能） | `$env:DEEPSEEK_API_KEY="sk-你的密钥"` |

未设置 `DEEPSEEK_API_KEY` 时，图书、购物车、订单等基础功能不受影响，仅 AI 助手 / 导购 Agent 不可用。

数据库连接、端口等可在 `bookstore-backend/src/main/resources/application.properties` 中调整，
默认连接 `jdbc:mysql://localhost:3306/bookstore`，首次启动会自动建库建表并写入初始书籍。

## 四、启动

```powershell
# 1) 后端
cd bookstore-backend
.\mvnw spring-boot:run          # 或 mvn spring-boot:run
# 启动后：http://localhost:8080

# 2) 前端（另开一个终端）
cd bookstore-frontend
npm install
npm run dev
# 打开：http://localhost:3000
```

## 五、主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/users/register` / `/api/v1/users/login` / `/api/v1/users/logout` | 注册 / 登录 / 退出（Session 会话） |
| GET | `/api/v1/users/me` | 当前登录用户 |
| GET | `/api/v1/books` | 图书列表（分页 + 关键字） |
| GET | `/api/v1/book/{id}` | 图书详情 |
| POST/PUT/DELETE | `/api/v1/book` · `/api/v1/book/{id}` | 图书管理（管理员） |
| GET/POST | `/api/v1/cart` | 查看购物车 / 加入购物车 |
| DELETE | `/api/v1/cart` · `/api/v1/cart/{cartItemId}` | 清空购物车 / 移除单项 |
| POST | `/api/v1/orders/checkout` | 购物车结算下单 |
| POST | `/api/v1/orders/buyNow` | 立即购买 |
| GET | `/api/v1/orders/search` · `/api/v1/orders/searchAll` | 按时间范围 + 书名搜索订单 |
| GET | `/api/v1/stat/hotBooks` · `/topUsers` · `/personal` | 销售统计 |
| POST | `/api/v1/ai/chat` | 智能客服（Function Calling） |
| POST | `/api/v1/agent/chat` | 导购 Agent（ReAct 循环） |
| GET | `/api/v1/agent/policy/document` · `/policy/search` · `/policy/stats` | 政策知识库 RAG |

## 六、提交说明

仓库中不包含 `node_modules/`、`target/`、`dist/`、`logs/` 等第三方依赖与构建产物，
克隆后按第四节执行 `npm install` / Maven 构建即可。
