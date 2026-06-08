# 审计智能体应用平台

一个面向审计工作场景的 AI 智能体应用项目，包含 Spring Boot 后端与 Vue 3 前端。项目围绕审计问答、审计底稿生成、文件上传、表格导入、自然语言生成 SQL 与 SQL 查询分析等流程构建，适合用于审计辅助、底稿自动化和数据分析场景的原型验证。

## 项目特点

- AI 审计问答：支持同步问答与 SSE 流式对话。
- 审计底稿生成：根据审计过程描述生成 Word 底稿文件。
- 超级智能体：集成工具调用能力，可结合文件内容完成更复杂的审计任务。
- 文件上传处理：支持上传审计相关资料并参与问答或生成流程。
- 表格数据分析：支持上传 Excel 表格，导入 MySQL 后执行 SQL 查询。
- 自然语言转 SQL：根据用户问题和表名生成查询语句。
- 前后端分离：前端使用 Vue 3 + Vite，后端使用 Spring Boot + Spring AI。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.3.12
- Spring AI
- Spring AI Alibaba / DashScope
- Ollama
- MySQL
- Apache POI / poi-tl
- Knife4j / SpringDoc OpenAPI

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia / Vuex
- Vue Router
- Ant Design Vue
- Axios

## 目录结构

```text
.
├── aiagent/          # Spring Boot 后端服务
├── aiagentfront/     # Vue 3 前端应用
├── package-lock.json # 根目录 npm 锁文件
├── query             # 项目辅助文件
└── start             # 项目辅助文件
```

## 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 20+
- npm 10+
- MySQL 8+
- Ollama 本地服务

## 后端配置

后端配置文件位于：

```text
aiagent/src/main/resources/application.yml
```

默认服务地址：

```text
http://localhost:8123/api
```

默认数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aiagent
    username: root
    password: 123456
```

AI Key 已通过环境变量读取：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}

deepseek:
  apikey: ${DEEPSEEK_API_KEY:}
```

启动前请根据自己的环境设置：

```powershell
$env:DASHSCOPE_API_KEY="你的 DashScope API Key"
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
```

如果使用 Ollama，请确保本地服务已启动，并已准备配置中的模型：

```text
http://localhost:11434
deepseek-r1:1.5b
```

## 启动后端

进入后端目录：

```powershell
cd aiagent
```

安装依赖并启动：

```powershell
mvn spring-boot:run -Dmaven.test.skip=true
```

也可以先打包再运行：

```powershell
mvn package -Dmaven.test.skip=true
java -jar target/aiagent-0.0.1-SNAPSHOT.jar
```

接口文档地址：

```text
http://localhost:8123/api/doc.html
http://localhost:8123/api/swagger-ui.html
http://localhost:8123/api/v3/api-docs
```

## 启动前端

进入前端目录：

```powershell
cd aiagentfront
```

安装依赖：

```powershell
npm install
```

启动开发服务：

```powershell
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

如果端口被占用，Vite 会自动切换到其他端口，例如：

```text
http://localhost:5174
```

## 主要页面

- `/`：首页
- `/audit-master`：AI 审计大师
- `/super-agent`：超级智能体
- `/audit-generate`：审计底稿生成
- `/table-sql`：表格上传与 SQL 查询分析

## 主要接口

后端统一上下文路径为 `/api`，控制器路径为 `/ai`。

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 审计问答 | GET | `/api/ai/audit_app/chat/sync` |
| 审计流式问答 | GET | `/api/ai/audit_app/chat/sse` |
| 带文件问答 | GET | `/api/ai/audit_app/chat/sse/file` |
| 工具调用问答 | GET | `/api/ai/audit_app/chat/ssetools` |
| 超级智能体对话 | GET | `/api/ai/manus/chat` |
| 生成 Word 底稿 | POST | `/api/ai/audit_app/generateword` |
| 上传文件 | POST | `/api/ai/audit_app/generateword/file` |
| 上传表格 | POST | `/api/ai/audit_app/upload_table` |
| 执行 SQL | POST | `/api/ai/audit_app/execute_sql` |
| 自然语言生成 SQL | POST | `/api/ai/audit_app/generate_sql` |
| 审计过程分析 | POST | `/api/ai/audit_app/analyze` |

## 常见问题

### 1. 后端测试编译失败怎么办？

当前项目中部分测试类可能与现有包路径或类名不一致。仅运行项目时可以跳过测试编译：

```powershell
mvn spring-boot:run -Dmaven.test.skip=true
```

### 2. 前端请求后端失败怎么办？

请确认后端已启动在：

```text
http://localhost:8123/api
```

前端开发环境会通过接口配置请求本地后端。如果端口或后端地址发生变化，需要同步修改前端 API 配置。

### 3. 表格上传后无法查询怎么办？

请确认：

- MySQL 服务已启动。
- 已创建 `aiagent` 数据库。
- 数据库账号密码与后端配置一致。
- 上传的表格第一行为表头。

## 后续优化方向

- 将数据库账号、文件上传路径等配置进一步环境变量化。
- 补充数据库初始化 SQL。
- 修复测试类编译问题，完善自动化测试。
- 增加 Docker Compose，统一启动 MySQL、后端与前端。
- 完善生产环境部署说明。

## 许可证

本项目暂未声明开源许可证。如需公开复用，请先补充 LICENSE 文件。
