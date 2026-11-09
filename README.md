# spring-ai-test

基于 **Spring AI** 的示例项目，演示大模型接入、多平台多模型切换、任务规划、多轮对话记忆、Function Calling（含动态工具）、以及基于向量数据库的知识库 RAG。

## 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.6 |
| Spring AI | 1.1.0（BOM 管理） |
| 阿里云百炼 DashScope | `spring-ai-alibaba-starter-dashscope` 1.1.2.3 |
| DeepSeek | `spring-ai-starter-model-deepseek` |
| 会话记忆 | Spring AI Chat Memory + 自定义 JDBC 增量存储（MySQL） |
| 向量存储 | PgVector（PostgreSQL） |
| 文档解析 | PDF Reader、Tika Reader + TokenTextSplitter |
| 数据库驱动 | MySQL Connector 8.0.21、PostgreSQL |
| 其他 | Lombok、Reactor（流式输出） |

## 功能特性

- **多平台多模型对话**：在 DashScope 与 DeepSeek 之间动态切换，支持自定义模型与 temperature。
- **多 ChatClient 编排**：规划型 ChatClient 负责识别用户意图（取消 / 查询 / 其他），客服型 ChatClient 负责流式回复。
- **Function Calling**：内置订单查询 / 取消工具，并演示如何根据配置动态构建 `ToolCallback`。
- **多轮对话记忆**：基于 MySQL 持久化会话，自定义增量保存逻辑，通过 `message_id` 去重，避免重复写入。
- **知识库 RAG**：支持文本、PDF 及 Tika 可解析的文档入库，提供相似度检索与基于知识库的普通 / 流式问答。

## 项目结构

```
src/main/java/com/zxx/springaitest
├── SpringAiTestApplication.java        # 启动类
├── config
│   ├── ChatClientInitConfiguration.java    # 规划 / 客服 ChatClient 配置
│   ├── IncrementalChatMemory.java          # 增量 ChatMemory 实现
│   ├── IncrementalChatMemoryConfiguration.java
│   ├── IncrementalJdbcChatMemoryRepository.java
│   ├── MessageIdInjectionAdvisor.java      # 为消息注入 message_id
│   └── ReAgainAdvisor.java                 # 自定义 Advisor 示例
├── controller
│   ├── CustomerServiceController.java      # 智能客服对话
│   ├── KnowledgeBaseController.java        # 知识库管理 / 问答
│   ├── MorePlatformAndModelController.java # 多平台多模型对话
│   └── MultiChatClientController.java      # 规划 + 客服编排
├── model
│   ├── AIJob.java                          # 任务类型（CANCEL / QUERY / OTHER）
│   ├── CustomerOptions.java
│   └── MorePlatformAndModelOptions.java
├── service
│   ├── SmartCustomerService.java
│   ├── KnowledgeBaseService.java
│   └── impl
└── tools
    ├── OrderTools.java                     # 注解式工具
    └── DynamicToolService.java             # 动态构建 ToolCallback
```

```
src/main/resources
├── application.properties        # Spring AI、日志、记忆表初始化配置
├── application.yml               # 数据源配置
├── sql/schema-mysql.sql          # 会话记忆表结构
└── file/prompet.sd               # 提示词示例
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven（或直接使用项目自带的 `mvnw`）
- MySQL 8（会话记忆）
- PostgreSQL + pgvector（知识库功能）

### 2. 配置 API Key

项目从环境变量读取密钥，启动前需设置：

```bash
export DASHSCOPE_API_KEY=你的阿里云百炼Key
export DEEPSEEK_API_KEY=你的DeepSeek Key
```

### 3. 初始化数据库

**MySQL**（会话记忆）：创建数据库 `spring_ai`，表结构见 `src/main/resources/sql/schema-mysql.sql`（已配置为启动时初始化，若未生效可手动执行）。

```sql
CREATE DATABASE spring_ai DEFAULT CHARSET utf8mb4;
```

**PostgreSQL**（知识库）：需要启用 pgvector 扩展。

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

数据源在 `src/main/resources/application.yml` 中配置。默认启用 MySQL；使用知识库功能时需切换为 PostgreSQL 数据源（文件中已提供注释示例）。

### 4. 启动项目

```bash
./mvnw spring-boot:run
```

默认端口 `8080`。

## API 接口

| 接口 | 方法 | 参数 | 说明 |
| --- | --- | --- | --- |
| `/chat` | GET | `message`、`plaform`（dashscope/deepseek）、`model`、`temperature` | 多平台多模型流式对话 |
| `/api/customer/chat` | GET | `message`、`sessionId` | 智能客服（工具调用 + 记忆），流式返回 |
| `/api/multi/chat` | GET | `message`、`sessionId` | 先规划任务，再按任务类型处理，流式返回 |
| `/api/v1/knowledge-base/insert-text` | GET | `content` | 文本切片后写入向量库 |
| `/api/v1/knowledge-base/upload-file` | POST | `file`（multipart） | PDF / 其他文档入库 |
| `/api/v1/knowledge-base/search` | GET | `query`、`topK` | 向量相似度检索 |
| `/api/v1/knowledge-base/chat` | GET | `query`、`topK` | 基于知识库回答 |
| `/api/v1/knowledge-base/chat-stream` | GET | `query`、`topK` | 基于知识库流式回答 |

示例：

```bash
# 多平台多模型
curl -G 'http://localhost:8080/chat' \
  --data-urlencode 'message=你好' \
  --data-urlencode 'plaform=dashscope' \
  --data-urlencode 'model=deepseek-v4-pro' \
  --data-urlencode 'temperature=0.1'

# 智能客服
curl -G 'http://localhost:8080/api/customer/chat' \
  --data-urlencode 'message=取消订单id为11111的订单' \
  --data-urlencode 'sessionId=1111'

# 知识库检索
curl -G 'http://localhost:8080/api/v1/knowledge-base/search' \
  --data-urlencode 'query=退款规则' \
  --data-urlencode 'topK=5'
```

## 核心实现说明

- **规划 + 客服双 ChatClient**：`planningChatClient` 以低 temperature 将用户意图解析为 `AIJob.Job`；`botChatClient` 以较高 temperature 生成客服回复，两者共享 `ChatMemory`。
- **增量记忆**：`IncrementalChatMemory` 保存前对比库中已有消息，仅写入新消息；`MessageIdInjectionAdvisor` 在请求 / 响应阶段为消息注入 `message_id`，配合 `INSERT IGNORE` 实现幂等去重。
- **动态工具**：`DynamicToolService` 通过 `ToolDefinition` + `MethodToolCallback` 手工构建工具，方便后续改为从数据库读取工具配置。
- **RAG 流程**：文档经 `PagePdfDocumentReader` / `TikaDocumentReader` 解析、`TokenTextSplitter` 切片后写入 `VectorStore`，问答时按 `topK` 检索并拼接到提示词中。

## 配置说明

- `application.properties` 中 `logging.level.org.springframework.ai=DEBUG` 等配置用于观察模型调用与工具执行过程，生产环境建议下调。
- 对话记忆表初始化通过 `spring.ai.chat.memory.repository.jdbc.schema=classpath:/sql/schema-mysql.sql` 指定。
- 数据源账号密码目前为本地开发默认值，请勿直接用于生产环境。
