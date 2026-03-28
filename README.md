# agtext

个人多功能 AI 助手 V1（本地运行）。

## 快速开始（本地）

### 0) 环境要求

- Java 17 + Maven
- Node.js 18+
- MySQL（本机已启动也可；或使用 `docker compose`）

### 1) 准备环境变量

```bash
copy .env.example .env
```

按需填写：`OPENAI_API_KEY`、MySQL 连接信息等。

### 2) 数据库（MySQL）

两种方式任选其一：

**方式 A：使用你本机已开启的 MySQL（端口 3306）**

- 确保已创建数据库（默认名 `agtext`）并配置账号（默认 `agtext/agtext`），或在 `.env` 中覆盖：
  - `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE`
  - `MYSQL_USERNAME` / `MYSQL_PASSWORD`

**方式 B：Docker 启动 MySQL（可选）**

```bash
cd infra
docker compose up -d
```

### 3) 启动后端（Spring Boot）

```bash
mvn -f backend/pom.xml spring-boot:run
```

健康检查：`http://localhost:8080/actuator/health`

### 4) 启动前端（React + Vite）

```bash
npm -C frontend install
npm -C frontend run dev
```

前端默认：`http://localhost:5173`

## 本地“无外部依赖”启动（推荐用于演示/验收）

后端使用 `test` profile：H2 内存库 + 内置 `mock` 模型（不需要 OpenAI Key，也不会消耗额度）。

```bash
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=test
node scripts/e2e-smoke.mjs
```

## 最小演示路径（UI）

- Chat：发送消息（可指定 provider/model）
- Knowledge：创建知识库 → 导入 Markdown/Web/PDF → 查看导入作业/parse report → Chat 里选择知识库提问
- Memory：审核候选记忆 → 通过/禁用 → 关联 goal/plan/task
- Tasks：Inbox/Today 管理 → 设置提醒/稍后提醒/关闭提醒
- Settings：工具开关、domain allowlist、模型默认 provider 与覆盖

## 文档

- 测试与验收：`docs/测试与验收.md`
- 遗留问题：`docs/遗留问题清单.md`
