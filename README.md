# spring-ai-test

基于 **Spring AI** 的示例项目，演示大模型接入、多平台多模型切换、任务规划、多轮对话记忆、Function Calling（动态工具构建）以及基于向量存储的知识库 RAG，并附带订单查询 / 取消的业务闭环。

## 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.6 |
| Spring AI | 1.1.0（BOM 管理） |
| 阿里云百炼 DashScope | `spring-ai-alibaba-starter-dashscope` 1.1.2.3 |
| DeepSeek | `spring-ai-starter-model-deepseek` |
| 会话记忆 | Spring AI Chat Memory + 自定义增量 JDBC 存储（MySQL） |
| 向量存储 | `SimpleVectorStore`（内存实现）；pom 保留 PgVector 依赖，当前未启用 |
| 文档解析 | `PagePdfDocumentReader`、`TikaDocumentReader`、`TokenTextSplitter` |
| 数据库 | MySQL 8（`mysql-connector-java` 8.0.21）、`JdbcTemplate` |
| 其他 | Lombok、Reactor（流式输出） |

> 向量存储：`VectorStoreConfig` 注册的是内存版 `SimpleVectorStore`（配合 `spring.ai.vectorstore.type=simple`），**进程重启后向量数据会丢失**，需要重新入库。若切回 PgVector，可直接启用 pom 中已引入的 `spring-ai-pgvector-store` 依赖并调整数据源与配置。

## 功能特性

- **多平台多模型对话**：在 DashScope 与 DeepSeek 之间动态切换，支持自定义模型与 temperature，流式返回。
- **规划 + 客服多 ChatClient 编排**：规划型 ChatClient（低温度）将用户意图解析为 `AIJob.Job`（CANCEL / QUERY / OTHER），客服型 ChatClient（高温度）负责流式回复，两者共享 `ChatMemory`。
- **智能客服（动态 Function Calling）**：`DynamicToolService` 通过 `ToolDefinition` + `MethodToolCallback` 手工构建订单查询 / 取消工具，底层落到 MySQL `orders` 表；仅「待发货」订单允许取消。
- **知识库优先的混合问答**：`/api/customer/chat` 先检索知识库，命中则直接回答，未命中（或回答「我不知道」）时回退到带工具的智能客服。
- **多轮对话记忆**：基于 MySQL 持久化会话，`IncrementalChatMemory` 对已有消息去重后仅写入增量，`message_id` + `INSERT IGNORE` 保证幂等，默认仅加载最近 10 条上下文。
- **知识库 RAG**：支持文本、PDF 及 Tika 可解析文档入库，提供相似度检索与基于知识库的普通 / 流式问答。

## 项目结构

```
src/main/java/com/zxx/springaitest
├── SpringAiTestApplication.java               # 启动类
├── config
│   ├── ChatClientInitConfiguration.java       # 规划 / 客服 ChatClient 配置
│   ├── VectorStoreConfig.java                 # SimpleVectorStore（内存）Bean
│   ├── IncrementalChatMemoryConfiguration.java
│   ├── IncrementalChatMemory.java             # 增量 ChatMemory 实现
│   ├── IncrementalJdbcChatMemoryRepository.java
│   ├── MessageIdInjectionAdvisor.java         # 为消息注入 message_id
│   └── ReAgainAdvisor.java                    # 自定义 Advisor 示例（未注册）
├── controller
│   ├── MorePlatformAndModelController.java    # 多平台多模型对话
│   ├── CustomerServiceController.java         # 智能客服对话
│   ├── MultiChatClientController.java         # 规划 + 客服编排
│   └── KnowledgeBaseController.java           # 知识库管理 / 问答
├── service
│   ├── SmartCustomerService.java
│   ├── KnowledgeBaseService.java
│   └── impl
│       ├── SmartCustomerServiceImpl.java
│       └── KnowledgeBaseServiceImpl.java
├── model
│   ├── AIJob.java                             # 任务类型（CANCEL / QUERY / OTHER）
│   ├── CustomerOptions.java
│   └── MorePlatformAndModelOptions.java
├── order
│   ├── dao/OrderRepository.java               # JdbcTemplate 订单读写
│   ├── enums/OrderStatusEnum.java             # 待发货 / 已发货 / 已取消
│   ├── model/Order.java                       # 订单记录
│   └── service/OrderService.java              # 订单查询 / 取消业务规则
└── tools
    ├── DynamicToolService.java                # 动态构建 ToolCallback（当前启用）
    └── OrderTools.java                        # 注解式工具示例（已注释）
```

```
src/main/resources
├── application.properties        # 模型、日志、记忆、相似度阈值配置
├── application.yml               # 数据源配置
├── sql/schema-mysql.sql          # 会话记忆表结构
├── sql/schema-order.sql          # 订单表结构 + 示例数据
└── file/prompet.sd               # 提示词示例
```

```
src/test/java/com/zxx/springaitest
├── SpringAiTestApplicationTests.java
├── TestStructuredOut.java
└── memory                        # ChatMemory 相关实验测试
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven（或直接使用项目自带的 `mvnw`）
- MySQL 8（会话记忆 + 订单业务）

当前使用内存向量库，**无需 PostgreSQL**。

### 2. 配置 API Key

项目从环境变量读取密钥，启动前需设置：

```bash
export DASHSCOPE_API_KEY=你的阿里云百炼Key
export DEEPSEEK_API_KEY=你的DeepSeek Key
```

### 3. 初始化数据库

**MySQL**：创建数据库 `spring_ai`，并执行建表脚本。

```sql
CREATE DATABASE spring_ai DEFAULT CHARSET utf8mb4;
```

```bash
# 会话记忆表
mysql -uroot -p spring_ai < src/main/resources/sql/schema-mysql.sql
# 订单表（含 10001 / 10002 两条示例数据）
mysql -uroot -p spring_ai < src/main/resources/sql/schema-order.sql
```

数据源在 `src/main/resources/application.yml` 中配置，默认连接 `localhost:3306/spring_ai`。

### 4. 启动项目

```bash
./mvnw spring-boot:run
```

默认端口 `8080`。

### 5. 体验示例

- 取消订单：`取消订单 10001`（待发货，可取消）
- 取消受阻：`取消订单 10002`（已发货，拒绝取消）

## API 接口

| 接口 | 方法 | 参数 | 说明 |
| --- | --- | --- | --- |
| `/chat` | GET / POST | `message`、`plaform`（dashscope/deepseek）、`model`、`temperature` | 多平台多模型流式对话 |
| `/api/customer/chatWithStream` | GET / POST | `message`、`sessionId` | 智能客服流式对话（记忆 + 动态工具） |
| `/api/customer/chat` | GET / POST | `message`、`sessionId` | 先查知识库，未命中回退客服（记忆 + 工具），非流式 |
| `/api/multi/chat` | GET | `message`、`sessionId` | 先规划任务，再按任务类型处理，流式返回 |
| `/api/v1/knowledge-base/insert-text` | GET | `content` | 文本切片后写入向量库 |
| `/api/v1/knowledge-base/upload-file` | POST | `file`（multipart） | PDF / Tika 文档入库 |
| `/api/v1/knowledge-base/search` | GET | `query`、`topK`（默认 5） | 向量相似度检索 |
| `/api/v1/knowledge-base/chat` | GET | `query`、`topK` | 基于知识库回答 |
| `/api/v1/knowledge-base/chat-stream` | GET | `query`、`topK` | 基于知识库流式回答（SSE） |

> `sessionId` 缺省时会生成 UUID，但不会返回给调用方；需要多轮上下文时请显式传入。
> 多平台参数名在代码中拼写为 `plaform`，调用时请保持一致。

示例：

```bash
# 多平台多模型
curl -G 'http://localhost:8080/chat' \
  --data-urlencode 'message=你好' \
  --data-urlencode 'plaform=dashscope' \
  --data-urlencode 'model=qwen3.8-flash' \
  --data-urlencode 'temperature=0.1'

# 智能客服（工具调用 + 记忆）
curl -G 'http://localhost:8080/api/customer/chatWithStream' \
  --data-urlencode 'message=取消订单10001' \
  --data-urlencode 'sessionId=1111'

# 规划 + 客服编排
curl -G 'http://localhost:8080/api/multi/chat' \
  --data-urlencode 'message=查询订单10001' \
  --data-urlencode 'sessionId=1111'

# 知识库检索
curl -G 'http://localhost:8080/api/v1/knowledge-base/search' \
  --data-urlencode 'query=退款规则' \
  --data-urlencode 'topK=5'
```

## 核心实现说明

- **规划 + 客服双 ChatClient**：`ChatClientInitConfiguration` 中 `planningChatClient`（temperature 0.1）将用户意图解析为 `AIJob.Job`；`botChatClient`（temperature 1.2）生成客服回复，两者共享同一个 `ChatMemory`。`MultiChatClientController` 先规划，再按 CANCEL / QUERY / OTHER 分支处理。
- **智能客服装配**：`SmartCustomerServiceImpl` 基于 `DashScopeChatModel` 构建独立的 ChatClient，挂载 `MessageIdInjectionAdvisor`、`PromptChatMemoryAdvisor` 以及 `DynamicToolService` 提供的动态工具。
- **混合问答回退**：`SmartCustomerServiceImpl.chat` 先调用知识库（topK=3、min score=0.3）；无相关文档或模型回复「我不知道」时返回 `null`，再回退到带工具的客服模型。
- **增量记忆**：`IncrementalChatMemory` 每次保存前对比库中已有消息，仅写入新消息；`MessageIdInjectionAdvisor` 在请求 / 响应阶段为消息注入 `message_id`，配合 `INSERT IGNORE` 实现幂等去重，读取时仅返回最近 10 条。
- **动态工具**：`DynamicToolService` 通过反射定位业务方法，用 `ToolDefinition` + `MethodToolCallback` 手工组装 `cancelOrder` / `queryOrder` 工具（`toolObject` 必须是 Spring Bean，否则无法依赖注入）。注解式实现保留在 `OrderTools` 中（已注释）。
- **订单业务规则**：`OrderService.cancelOrder` 先查询订单校验状态，仅 `PENDING_SHIPMENT` 可取消，并通过 `UPDATE ... WHERE status = 待发货` 做乐观并发控制，受影响行数为 0 时提示刷新重试。
- **RAG 流程**：PDF 走 `PagePdfDocumentReader`，其他文档走 `TikaDocumentReader`，直接写入向量库；`insert-text` 则先经 `TokenTextSplitter` 切片。问答时按 `topK` 检索并拼接到提示词中。

## 配置说明

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `spring.ai.dashscope.*` / `spring.ai.deepseek.*` | `application.properties` | 模型、Embedding 与 API Key（环境变量注入） |
| `spring.ai.vectorstore.type` | `application.properties` | 当前为 `simple`（内存），重启后数据丢失 |
| `spring.ai.chat.memory.repository.jdbc.schema` | `application.properties` | 会话记忆建表脚本路径 |
| `similarity.search.min.score` | `application.properties` | 混合问答路径的相似度阈值（0.3） |
| `logging.level.org.springframework.ai` | `application.properties` | 当前为 DEBUG，便于观察模型调用与工具执行，生产环境建议下调 |
| `spring.datasource.*` | `application.yml` | MySQL 数据源；PostgreSQL 配置以注释形式保留 |

- 数据源账号密码为本地开发默认值，请勿直接用于生产环境。
- 日志中的 Jackson DEBUG 配置用于排查消息序列化问题，可按需关闭。
