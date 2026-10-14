# 食客外卖 · shike-take-out

> 一个基于 **Spring Boot 3 + JDK 17** 的外卖点餐 / 配送后端服务。
> 在经典「商家后台 + 微信小程序」双端业务之上，落地了 **RabbitMQ 阶梯式延时关单、LangChain4j + Qdrant 的 RAG 语义检索、AI 智能客服、个性化推荐、百度地图配送范围校验**等进阶能力。

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.0.3-blue)](https://mybatis.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-FF6600)](https://www.rabbitmq.com/)
[![Qdrant](https://img.shields.io/badge/Qdrant-v1.12-purple)](https://qdrant.tech/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.1-blueviolet)](https://github.com/langchain4j/langchain4j)

---

## ✨ 项目亮点

| 方向 | 亮点 | 关键实现 |
| --- | --- | --- |
| 🤖 **AI / RAG** | 接入 LangChain4j + Qdrant，实现「知识库文档检索 + 商品语义检索」双场景 RAG | `RagConfiguration`、`RagServiceImpl`、`ItemVectorIngestor` |
| 💬 **智能客服** | 声明式 `@AiService` + 会话记忆 + RAG 检索增强，提示词外置到资源文件 | `AssistantService`、`AiCreator`、`system-prompt.txt` |
| ⏱ **订单超时** | RabbitMQ **消息级 TTL + 死信交换机** 实现**阶梯式**延时关单（30s→30s→1min→3min→5min→5min，累计 15min），用户中途支付即停止续发，杜绝无用消息堆积 | `DelayMessageProcessor`、`OrderTimeoutConsumer`、`OrderMqConfiguration` |
| 📨 **MQ 可靠性** | 发送端 confirm / return 回调 + 消费端异常重试 + `RepublishMessageRecoverer` 兜底转入 error 队列，全链路可观测 | `MqConfirmConfig`、`ErrorConfiguration` |
| 🎯 **个性化推荐** | 「为你推荐」按用户懒加载计算并缓存，无历史订单自动回退「大家都在买」热门快照 | `RecommendServiceImpl`、`RecommendCategoryConstant` |
| 🔥 **热门榜单** | 「大家都在买」虚拟分类 + Redis 快照，定时刷新近 14 天热销 TOP-15，**不回写 `dish.category_id`** 避免破坏商品归属 | `CategoryTask`、`HotCategoryConstant` |
| 🗺 **配送范围** | 百度地图地理编码 + 驾车算路，下单前校验收货地址是否在 **5km** 配送圈内 | `BaiduMapUtil`、`OrderServiceImpl#checkDeliveryRange` |
| 🔔 **实时提醒** | WebSocket 推送「来单提醒 / 客户催单」 | `WebSocketServer`、`WebSocketTask` |
| 📊 **数据统计** | 工作台总览、营业额 / 用户 / 订单统计、销量 TOP10，支持 Apache POI 导出 Excel | `WorkspaceServiceImpl`、`ReportServiceImpl`、`ReportController` |
| 🔐 **认证与审计** | 双端 JWT 拦截器 + ThreadLocal 上下文 + `@AutoFill` 公共字段自动填充 + `@Log` 操作日志切面 | `JwtToken*Interceptor`、`AutoFillAspect`、`OperateLogAspect` |

---

## 🧱 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 / 框架 | Java 17、Spring Boot 3.3.5、`jakarta.*` |
| 持久层 | MyBatis 3、PageHelper 分页、MySQL 8、Druid 连接池 |
| 缓存 | Redis（Lettuce）、Spring Cache |
| 消息队列 | RabbitMQ（`spring-boot-starter-amqp`，TTL + DLX 延时，confirm/return/重试/兜底队列） |
| AI / RAG | LangChain4j 1.0.1、Qdrant 向量库、Ollama / OpenAI 兼容 Embedding、OpenAI 兼容 ChatModel |
| 实时通信 | WebSocket（JSR-356，`jakarta.websocket`） |
| 定时任务 | Spring Task（`@Scheduled`） |
| 接口文档 | SpringDoc OpenAPI 2.6 + Knife4j 4.5（`doc.html`） |
| 第三方 | 阿里云 OSS、微信小程序 / 微信支付 v3、百度地图开放平台 |
| 工具 | Lombok、JJWT 0.12、FastJSON、Apache POI 5、AspectJ |

---

## 🏗 模块结构

Maven 多模块，依赖方向严格为 `shike-server → shike-pojo / shike-common`；后两者互不依赖。

```
shike-take-out/                 # 父 POM：版本与依赖统一管理
├── shike-common/               # 通用层：常量、异常、工具、通用配置属性、统一返回体、MQ 延时处理器
│   └── com.shike.{constant, context, enumeration, exception, json,
│                  mq, properties, result, utils}
├── shike-pojo/                 # 数据模型：entity / dto / vo，无业务逻辑
│   └── com.shike.{entity, dto, vo}
├── shike-server/               # 应用层：启动类 + 全部 Web / 业务 / AI / MQ 代码
│   └── com.shike.{ai, anno, aop, aspect, config, controller,
│                  handler, interceptor, mapper, mq, service, task, websocket}
├── docker-compose.qdrant.yml   # 本地 Qdrant 向量库（单节点，仅暴露 6333/6334）
└── shike_take_out.sql          # 建库建表脚本（13 张表）
```

**新增类的放置原则：** 纯数据类 → `shike-pojo`；工具 / 常量 / 异常 / 通用属性 → `shike-common`；其余 → `shike-server`。

---

## 🚀 快速开始

### 1. 环境依赖

| 组件 | 默认地址 | 说明 |
| --- | --- | --- |
| JDK | 17 | 项目基于 Java 17 编译运行 |
| MySQL | `localhost:3306/shike_take_out` | 依次执行 `shike_take_out.sql`、`shike_take_out_test_data.sql` |
| Redis | `localhost:6379` (db 0) | 缓存 / 店铺状态 / 热门与推荐快照 |
| RabbitMQ | `localhost:5672` | 订单超时延时关单 |
| Qdrant | `localhost:6334` (gRPC) | RAG 向量库，`docker compose -f docker-compose.qdrant.yml up -d` |
| Ollama（可选） | `localhost:11434` | 本地 Embedding 模型；也可改用远程 OpenAI 兼容 API |
| 百度地图 AK / 阿里云 OSS / 微信小程序 | — | 见 `application-dev.yml`，缺失时相应功能不可用 |

### 2. 启动 Qdrant（RAG 必需）

```bash
docker compose -f docker-compose.qdrant.yml up -d
curl http://localhost:6333/readyz          # 就绪探针
# 浏览器打开 http://localhost:6333/dashboard 可查看集合
```

> LangChain4j 的 Qdrant 适配器走 **gRPC 6334**，不是 REST 的 6333，配置时注意区分。

### 3. 构建与运行

```bash
# 全量构建
mvn clean package -DskipTests

# 仅编译 server 模块
mvn -pl shike-server -am compile

# 运行（默认激活 dev profile，服务端口 8080）
mvn -pl shike-server spring-boot:run
# 或
java -jar shike-server/target/shike-server-1.0-SNAPSHOT.jar
```

启动后访问接口文档：**http://localhost:8080/doc.html**（分组：管理端接口 / 用户端接口）。

### 4. 关闭 / 降级 RAG

RAG 全部 Bean 均带 `@ConditionalOnProperty`，只需在配置中设置：

```yaml
shike:
  rag:
    enabled: false   # 不装配任何 Qdrant / Embedding Bean，AI 客服退化为纯对话模式
```

---

## 🔌 接口约定

| 端 | 路径前缀 | 使用者 | 认证 Header |
| --- | --- | --- | --- |
| 管理端（商家后台） | `/admin/**` | 商家员工 | `token`（JWT） |
| 用户端（微信小程序） | `/user/**` | 小程序用户 | `authentication`（JWT） |
| 支付回调 | `/notify/**` | 微信支付平台 | 无需认证 |
| WebSocket | `/ws/{sid}` | 后台来单/催单提醒 | — |

- 所有接口统一返回 `Result<T>`：`code`（1 成功 / 0 失败）、`msg`、`data`；分页统一用 `PageResult`。
- 需要放行的接口可标注 `@NoAuth`；认证通过后当前登录 id 写入 `CurrentHolder`（ThreadLocal）。

### RAG 调试接口

```bash
# 导入 classpath 下的知识库文档（默认 classpath*:docs/*.md）
curl -X POST "http://localhost:8080/admin/rag/import?path=classpath*:docs/*.md"

# 语义检索（只走向量库，不调大模型，用于验证 embedding + 检索）
curl "http://localhost:8080/admin/rag/search?query=费曼技巧是什么"

# RAG 问答（检索 + 大模型生成）
curl "http://localhost:8080/admin/rag/chat?msg=费曼技巧是什么"

# 商品向量全量重建（菜品 + 套餐，reset=true 先按 type 清理旧向量）
curl -X POST "http://localhost:8080/admin/rag/rebuild-items?reset=true"
```

---

## 🧠 RAG 与向量化设计要点

### 单 collection 双数据源

商品向量（菜品 / 套餐）与知识库文档向量**共用同一个 Qdrant collection**，依靠元信息 `type` 字段区分，`clearByType` 只清理商品类向量，不会误删知识库文档。

### 语料一致性

`ItemVectorTextBuilder` 被抽为**纯函数组件**，批量重建与实时向量化必须使用完全一致的拼接模板，否则同款商品会互相召不回：

```
菜品：名称；分类：xx；口味：xxx；描述：xxx；价格档位：中档(28元)
套餐：名称；分类：xx；含：xxx；描述：xxx；价格档位：高档(88元)
```

### 幂等重建

`ItemVectorIngestor.rebuildAll(reset=true)` 先按 `type` 删除旧向量再批量写入 —— Qdrant 不按业务 id 去重，必须显式清理；入库范围仅限**在售**商品（`status = 1`），停售商品不占向量也不参与检索。

### 外部化 Embedding

Embedding 模型**不在 JVM 进程内运行**，改为调用外部服务（默认本地 Ollama `xagi/qwen3-embedding`，1024 维；可切换为任意 OpenAI 兼容远程 API），避免模型加载拖垮应用内存与启动速度。

---

## ⏱ 阶梯式延时关单

订单超时不是一个 15 分钟的长延时，而是拆成多段短延时逐段续发：

```
生产者 ──► order.delay.exchange ──► order.delay.queue
                                         │ 消息携带单条 TTL（expiration）等待到期
                                         ▼ 成为死信，按队列参数转发
                              order.dlx.exchange ──► order.dlx.queue
                                                          │
                                          OrderTimeoutConsumer 监听这里
                                                          │
                            未支付 → 续发下一段 ／ 阶梯走完 → 关闭订单
                            已支付 → 幂等返回（ack），不再续发
```

- **为什么逐条设 TTL**：阶梯每段时长不同（30s/30s/1min/3min/5min/5min），无法用队列级 `x-message-ttl`。
- **为什么分阶梯**：用户中途支付后不再产生后续消息，避免无用长延时消息在队列里堆积。
- **队首阻塞**：RabbitMQ 只检查队首消息是否过期，订单超时独占一个延时队列，避免与其他延时业务互相阻塞。
- **发送时机**：`@TransactionalEventListener(AFTER_COMMIT)` 在**下单事务提交后**才发首段消息，事务回滚则不发，杜绝「订单不存在」的孤儿消息。
- **幂等**：消费者按订单号查订单，非「待付款」状态直接返回，重投不产生副作用。

---

## 📊 各端功能一览

**管理端 `/admin`**：员工登录、分类管理、菜品管理（含口味、起停售、缓存清理）、套餐管理、订单接单/派送/完成、店铺营业状态、工作台总览、数据统计与 Excel 导出、文件上传（OSS）、RAG 调试。

**用户端 `/user`**：微信登录、店铺状态、分类 / 菜品 / 套餐浏览、购物车、地址簿、**配送范围校验**、下单支付、订单查询与催单；额外提供**【大家都在买】热门分类**与**【为你推荐】个性化分类**、**AI 智能客服**。

---

## ⚠️ 注意事项

1. `application.yml`、`application-dev.yml` 与两个 `.sql` 文件均在 `.gitignore` 中，**不入版本库**，含真实密钥，严禁提交。
2. **微信支付当前为 Mock**：`OrderServiceImpl#payment()` 中真实微信下单调用已注释（标记「勿删除」），直接走 `paySuccess()` 置为「已支付 + 待接单」并推送 WebSocket 来单提醒；恢复真实支付前不要删除注释块。
3. 订单状态常量定义在 `Orders` 实体（`Orders.PENDING_PAYMENT` 等），**禁止魔法数字**。
4. Windows 下 JDK 17 的 `file.encoding` 默认为 GBK，`pom.xml` 已通过 `-Dfile.encoding=UTF-8` 与显式 UTF-8 编码保证中文资源与提示词不乱码。
5. 项目曾在 Windows(CRLF) 下产生大规模行尾 diff，**不要做无意义的整文件格式化**，提交前用 `git diff --stat` 确认改动范围。

---

## 📄 License

本项目仅用于学习与技术验证，可自由参考。
