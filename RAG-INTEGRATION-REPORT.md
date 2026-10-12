# LangChain4j RAG 能力接入报告

> 项目：食客外卖（shike-take-out）
> 主题：接入 LangChain4j + 本地 Qdrant，落地最小可用的 RAG 检索能力（Embedding + 向量库）
> 完成时间：2026-10-08

---

## 1. 目标与交付范围

| 目标 | 状态 | 说明 |
| --- | --- | --- |
| 接入 LangChain4j 依赖 | ✅ | 核心 + Qdrant 适配器 + 本地 ONNX Embedding 模型 |
| 具备 RAG 能力 | ✅ | 进程内加载 embedding 模型、连接本地 Qdrant、支持「入库 + 语义检索」闭环 |
| Docker 本地部署 Qdrant（仅必要功能） | ✅ | 单节点、关闭 TLS、关闭遥测、仅暴露 6333/6334 |
| 产出报告 | ✅ | 即本文档 |

### 1.1 文件变更总览（新增 5 / 修改 3 / 删除 0）

| 类型 | 数量 | 文件 |
| --- | --- | --- |
| ✅ 新增 | 5（本报告另计） | `docker-compose.qdrant.yml`、`RagProperties.java`、`RagConfiguration.java`、`RagService.java`、`RagServiceImpl.java` |
| ✏️ 修改 | 3 | `pom.xml`、`shike-server/pom.xml`、`application-dev.yml`（被 gitignore） |
| ❌ 删除 | 0 | — |
| ❌ 重命名 | 0 | — |

> 详细的逐文件说明见第 5 节「文件变更清单」；其中区分了「本次 RAG 接入引入」与「工作区中已有的其它改动」。

---

## 2. 版本选型：为什么是 LangChain4j 0.35.0

这是本次接入**最关键的一个决策点**，直接决定兼容性。

### 2.1 约束条件

- 父 POM 基于 `spring-boot-starter-parent:2.7.3`，属于 **Spring Boot 2.x + `javax.*`** 体系。
- 项目编译产物实测为 **Java 8 字节码**（`major version 52`）。
- 因此：

  1. **不能用 `langchain4j-spring-boot-starter`**：它依赖 Spring Boot 3 / `jakarta.*`，与本项目不兼容。本次全部改为**编程式 `@Configuration` 装配**。
  2. **不能用 LangChain4j 1.x**：1.0.0 / 1.0.1 / 1.1.0 的字节码为 Java 17（`major version 61`），在 Java 8 目标下无法使用。

### 2.2 实测验证

通过下载 jar 并解析 class 文件版本号验证：

| 版本 | class major version | 兼容 Java 8 |
| --- | --- | --- |
| langchain4j 0.35.0 | 52 | ✅ |
| langchain4j 1.0.0 | 61 | ❌ |
| langchain4j 1.0.1 | 61 | ❌ |
| langchain4j 1.1.0 | 61 | ❌ |

**结论：选用 `0.35.0`**，它是仍然兼容 Java 8 的最后一个版本，无需升级项目 JDK 与 Spring Boot。

---

## 3. 依赖变更

### 3.1 父 POM（`pom.xml`）

在 `<properties>` 增加版本号，在 `<dependencyManagement>` 统一管理版本：

```xml
<properties>
    ...
    <langchain4j.version>0.35.0</langchain4j.version>
</properties>

<dependencyManagement>
    <dependencies>
        ...
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-qdrant</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-embeddings-all-minilm-l6-v2-q</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 3.2 `shike-server/pom.xml`

仅引入 RAG 所需的最小集合：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-qdrant</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings-all-minilm-l6-v2-q</artifactId>
</dependency>
```

### 3.3 实际引入的依赖树（关键项）

```
+- dev.langchain4j:langchain4j:0.35.0
|  +- dev.langchain4j:langchain4j-core:0.35.0
|  \- org.apache.opennlp:opennlp-tools:1.9.4
+- dev.langchain4j:langchain4j-qdrant:0.35.0
|  \- io.qdrant:client:1.11.0
|     +- io.grpc:grpc-protobuf:1.65.1
|     +- io.grpc:grpc-netty-shaded:1.65.1
|     +- com.google.protobuf:protobuf-java:3.25.1
|     \- com.google.guava:guava:32.0.0-jre
\- dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2-q:0.35.0
   \- dev.langchain4j:langchain4j-embeddings:0.35.0
      +- com.microsoft.onnxruntime:onnxruntime:1.17.1
      +- ai.djl:api:0.28.0
      \- ai.djl.huggingface:tokenizers:0.28.0
```

要点：

- **Embedding 模型内置**：`langchain4j-embeddings-all-minilm-l6-v2-q`（约 16.4 MB）已把 ONNX 权重与词表打包进 jar，**首次 `embed` 时在进程内加载，无需联网、无需任何 API Key**。
- **Qdrant 走 gRPC**：`io.qdrant:client:1.11.0` 基于 gRPC，因此 Qdrant 必须暴露 **6334（gRPC）**，而不只是 6333（REST）。这是最容易踩的坑。
- 依赖数量：`shike-server` 依赖树共约 199 个节点，其中 LangChain4j 相关约 30 个。

---

## 4. Docker 部署 Qdrant（仅必要功能）

新增文件：`docker-compose.qdrant.yml`（项目根目录）。

### 4.1 设计取舍

| 项目 | 选择 | 原因 |
| --- | --- | --- |
| 镜像 | `qdrant/qdrant:v1.12.4` | 与 gRPC 客户端 `io.qdrant:client:1.11.0` 兼容稳定 |
| 端口 | 仅 `6333`(REST) + `6334`(gRPC) | 6334 是 LangChain4j 必需；6333 用于调试/控制台 |
| TLS | 关闭 | 本地开发明文即可 |
| 遥测 | 关闭（`QDRANT__TELEMETRY_DISABLED=true`） | 无数据外发 |
| 集群 | 关闭（`QDRANT__CLUSTER__ENABLED=false`） | 单节点，无需分布式 |
| API Key | 不设置 | 本地无需鉴权（如需开启见 4.4） |
| 持久化 | 命名卷 `shike_qdrant_storage` | 数据不随容器销毁而丢失，且不污染项目目录 |
| 资源限制 | 内存上限 1 GB | 避免本地开发占满内存 |
| 健康检查 | `bash /dev/tcp/127.0.0.1/6333` | 官方镜像自带 bash，TCP 探测足够 |

### 4.2 核心配置（节选）

```yaml
services:
  qdrant:
    image: qdrant/qdrant:v1.12.4
    container_name: shike-qdrant
    restart: unless-stopped
    ports:
      - "6333:6333"   # REST / Web dashboard
      - "6334:6334"   # gRPC（langchain4j-qdrant 使用）
    volumes:
      - qdrant_storage:/qdrant/storage
    environment:
      QDRANT__TELEMETRY_DISABLED: "true"
      QDRANT__SERVICE__ENABLE_TLS: "false"
      QDRANT__LOG_LEVEL: "INFO"
      QDRANT__CLUSTER__ENABLED: "false"
    deploy:
      resources:
        limits:
          memory: 1g
    healthcheck:
      test: ["CMD", "bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/6333"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 10s

volumes:
  qdrant_storage:
    name: shike_qdrant_storage
```

### 4.3 启动与验证（已在本地实测通过）

```bash
# 启动
docker compose -f docker-compose.qdrant.yml up -d

# 状态（应为 healthy）
docker compose -f docker-compose.qdrant.yml ps

# 就绪探针
curl http://localhost:6333/readyz          # -> all shards are ready

# 列出集合
curl http://localhost:6333/collections

# 查看集合详情（维度 / 距离 / 点数）
curl http://localhost:6333/collections/shike_knowledge

# Web 控制台
# 浏览器打开 http://localhost:6333/dashboard

# 停止（保留数据） / 停止并删除数据
docker compose -f docker-compose.qdrant.yml down
docker compose -f docker-compose.qdrant.yml down -v
```

实测结果：

```
容器     shike-qdrant   qdrant/qdrant:v1.12.4   Up (healthy)   6333-6334->6333-6334
readyz   all shards are ready
集合     shike_knowledge  size=384  distance=Cosine  points=0
```

### 4.4 可选：开启 API Key 鉴权

如需开启，在 compose 的 `environment` 中增加自定义 Key（须与 `application-dev.yml` 中 `shike.rag.qdrant.api-key` 一致）：

```yaml
environment:
  QDRANT__SERVICE__API_KEY: "your-qdrant-api-key"
```

---

## 5. 文件变更清单（新增 / 删除 / 修改）

严格遵循 `AGENTS.md` 的分层放置规则。

> 说明：本仓库工作区同时存在**本次 RAG 接入之前就已存在**的其它改动（CRLF 换行、MQ/百度地图/为你推荐等功能）。
> 为方便审查，下面分为两类列出：**5.1 本次 RAG 接入直接相关的变更**，以及 **5.2 工作区中其它已存在的变更（非本次 RAG 引入，仅供参考）**。
> 判定方式：`git diff --ignore-cr-at-eol --numstat`（过滤纯换行差异）+ `git status`。

### 5.1 本次 RAG 接入直接相关的变更

#### 5.1.1 新增文件（5 个，本报告另计）

| 文件 | 模块 | 职责 |
| --- | --- | --- |
| `docker-compose.qdrant.yml` | 根目录 | Qdrant 本地部署（最小功能），新增 |
| `RAG-INTEGRATION-REPORT.md` | 根目录 | 本报告，新增 |
| `shike-common/src/main/java/com/shike/properties/RagProperties.java` | shike-common | `shike.rag.*` 配置属性（配置属性归 common） |
| `shike-server/src/main/java/com/shike/config/RagConfiguration.java` | shike-server | 编程式装配 RAG 各 Bean（配置类归 server） |
| `shike-server/src/main/java/com/shike/service/RagService.java` | shike-server | RAG 服务接口（入库 / 检索 / 清空） |
| `shike-server/src/main/java/com/shike/service/impl/RagServiceImpl.java` | shike-server | RAG 服务实现 |

#### 5.1.2 修改文件（3 个）

| 文件 | 新增/删除行 | 改动 |
| --- | --- | --- |
| `pom.xml` | +34 / -0 | 新增 `<langchain4j.version>0.35.0</langchain4j.version>` 及 3 个依赖的 `dependencyManagement` |
| `shike-server/pom.xml` | +46 / -0 | 引入 `langchain4j`、`langchain4j-qdrant`、`langchain4j-embeddings-all-minilm-l6-v2-q` 三个依赖 |
| `shike-server/src/main/resources/application-dev.yml` | 新增配置块 | 新增 `shike.rag.*` 配置（**该文件被 `.gitignore` 忽略，不会提交，需手动同步**，见 6.2） |

#### 5.1.3 删除文件

> **无。** 本次 RAG 接入未删除任何文件，也未重命名任何文件。

---

### 5.2 工作区中其它已存在的变更（非本次 RAG 引入）

以下变更在本次会话开始前就已存在于工作区（见 `change.md` 及既有功能开发），**不是本次 RAG 接入引入的**，列出仅供参考，便于整体审查时区分。

#### 5.2.1 其它新增文件（未跟踪）

| 文件 | 说明 |
| --- | --- |
| `shike-common/src/main/java/com/shike/constant/HotCategoryConstant.java` | “大家都在买”虚拟分类常量 |
| `shike-common/src/main/java/com/shike/constant/RecommendCategoryConstant.java` | “为你推荐”虚拟分类常量 |
| `shike-common/src/main/java/com/shike/properties/BaiduMapProperties.java` | 百度地图配置属性 |
| `shike-common/src/main/java/com/shike/utils/BaiduMapUtil.java` | 百度地图工具类 |
| `shike-common/src/main/java/com/shike/result/DeliveryDistanceResult.java` | 配送距离结果 |
| `shike-pojo/src/main/java/com/shike/dto/event/OrderCreatedEvent.java` | 订单创建事件 |
| `shike-server/src/main/java/com/shike/mq/config/OrderMqConfiguration.java` | 订单 MQ 配置 |
| `shike-server/src/main/java/com/shike/mq/config/RabbitMQConfiguration.java` | RabbitMQ 配置 |
| `shike-server/src/main/java/com/shike/service/RecommendService.java` | “为你推荐”服务接口 |
| `shike-server/src/main/java/com/shike/service/impl/RecommendServiceImpl.java` | “为你推荐”服务实现 |
| `shike-server/src/main/java/com/shike/task/RecommendTask.java` | “为你推荐”定时任务 |
| `AGENTS.md` | 项目协作说明 |

#### 5.2.2 其它修改文件（真实内容变更，忽略 CRLF 后仍有差异）

| 文件 | 新增/删除行 |
| --- | --- |
| `shike-common/pom.xml` | +36 / -0 |
| `shike-common/.../constant/MessageConstant.java` | +2 / -0 |
| `shike-common/.../constant/MqOrderConstant.java` | +67 / -1 |
| `shike-common/.../constant/StatusConstant.java` | +2 / -0 |
| `shike-common/.../mq/DelayMessageProcessor.java` | +308 / -1 |
| `shike-pojo/pom.xml` | +19 / -0 |
| `shike-server/.../ShikeApplication.java` | +2 / -0 |
| `shike-server/.../config/ErrorConfiguration.java` | +1 / -1 |
| `shike-server/.../config/MqConfirmConfig.java` | +50 / -1 |
| `shike-server/.../config/RedisConfiguration.java` | +72 / -0 |
| `shike-server/.../controller/admin/CategoryController.java` | +8 / -0 |
| `shike-server/.../controller/user/DishController.java` | +76 / -3 |
| `shike-server/.../controller/user/OrderController.java` | +31 / -0 |
| `shike-server/.../interceptor/JwtTokenUserInterceptor.java` | +1 / -2 |
| `shike-server/.../mapper/CategoryMapper.java` | +32 / -4 |
| `shike-server/.../mapper/DishMapper.java` | +29 / -0 |
| `shike-server/.../mq/consumer/OrderTimeoutConsumer.java` | +138 / -1 |
| `shike-server/.../mq/publisher/OrderTimeoutMqSender.java` | +15 / -17 |
| `shike-server/.../service/CategoryService.java` | +12 / -0 |
| `shike-server/.../service/OrderService.java` | +21 / -0 |
| `shike-server/.../service/impl/CategoryServiceImpl.java` | +104 / -0 |
| `shike-server/.../service/impl/OrderServiceImpl.java` | +177 / -22 |
| `shike-server/.../service/impl/SetmealServiceImpl.java` | +3 / -3 |
| `shike-server/.../service/impl/ShopServiceImpl.java` | +10 / -1 |
| `shike-server/.../task/CategoryTask.java` | +40 / -1 |
| `shike-server/src/main/resources/mapper/AddressBookMapper.xml` | +18 / -0 |
| `shike-server/src/main/resources/mapper/CategoryMapper.xml` | +20 / -0 |
| `shike-server/src/main/resources/mapper/DishMapper.xml` | +73 / -0 |
| `shike-server/src/test/java/com/shike/test/GeoCoderTest.java` | +252 / -1 |
| `shike-server/src/test/java/com/shike/test/mqConsumerTest.java` | +14 / -1 |

> 除此之外，工作区中还有约 160 个文件在 `git status` 中显示为已修改，但 `git diff --ignore-cr-at-eol` 后**无任何内容差异**，属于**纯换行符（LF ↔ CRLF）差异**，与功能无关，审查时可忽略。

#### 5.2.3 删除文件

> **无。** 工作区当前没有任何已跟踪文件被删除。

### 5.3 装配的 Bean（`RagConfiguration`）

| Bean | 类型 | 说明 |
| --- | --- | --- |
| `qdrantClient` | `io.qdrant.client.QdrantClient` | 共享 gRPC 客户端，`destroyMethod="close"` 随容器关闭释放 |
| `embeddingModel` | `EmbeddingModel` | `AllMiniLmL6V2QuantizedEmbeddingModel`（384 维，进程内 ONNX） |
| `embeddingStore` | `EmbeddingStore<TextSegment>` | `QdrantEmbeddingStore`；创建前确保集合存在（自动建集合） |
| `embeddingStoreIngestor` | `EmbeddingStoreIngestor` | 切分 → 向量化 → 入库一站式 |
| `contentRetriever` | `ContentRetriever` | `EmbeddingStoreContentRetriever`，供上层 RAG 流程复用 |

> 说明：`langchain4j-qdrant:0.35.0` 的 `QdrantEmbeddingStore` 在写入时**不会自动创建集合**，因此在 `embeddingStore` Bean 中先调用 `collectionExistsAsync` / `createCollectionAsync` 显式建集合（`Cosine` + 384 维），避免首次写入报错。

### 5.4 对外服务接口（`RagService`）
```java
int ingest(String text);                                   // 入库
int ingest(String text, Map<String, Object> metadata);     // 入库（带元数据）
List<RagSearchResult> search(String query);                // 语义检索
List<RagSearchResult> search(String query, int maxResults); // 语义检索（指定条数）
void clear();                                              // 清空当前集合
```

使用示例：

```java
@Autowired
private RagService ragService;

// 1) 把菜品描述写入知识库
ragService.ingest(
    "宫保鸡丁是一道经典川菜，主要食材是鸡胸肉、花生米和干辣椒，口味香辣微甜，售价32元。",
    Map.of("source", "menu", "dish", "宫保鸡丁"));

// 2) 语义检索
List<RagService.RagSearchResult> hits = ragService.search("有什么不辣的鸡肉菜?", 3);
hits.forEach(h -> System.out.println(h.getScore() + " | " + h.getText() + " | " + h.getMetadata()));
```

---

## 6. 配置说明

### 6.1 配置项（`shike.rag.*`）

```yaml
shike:
  rag:
    enabled: true                      # 总开关，false 时不装配任何 RAG Bean
    init-collection-on-startup: true   # 启动时集合不存在则自动创建
    qdrant:
      host: localhost
      port: 6334                       # 注意：gRPC 端口，不是 REST 的 6333
      use-tls: false
      api-key:                         # 本地无鉴权时留空
      collection-name: shike_knowledge
      payload-text-key: text
      dimension: 384                   # 与 all-MiniLM-L6-v2 一致
      timeout-seconds: 10
    embedding:
      max-segment-size: 500            # 单片段最大字符数
      max-overlap-size: 50             # 相邻片段重叠字符数
    search:
      max-results: 5                   # 默认返回条数
      min-score: 0.6                   # 余弦相似度阈值
```

### 6.2 开箱即用

`RagProperties` 的所有字段**都带默认值**，且默认值就是本地方案（`localhost:6334` / `shike_knowledge` / 384 维 / Cosine）。

因此：**只要 Qdrant 已启动，即使不写任何 `shike.rag.*` 配置，应用也能正常使用 RAG**。`application-dev.yml` 中的配置块主要用于显式化与调优。

---

## 7. 验证记录

### 7.1 编译与字节码校验

```
mvn compile  ->  BUILD SUCCESS（shike-common / shike-pojo / shike-server）
RagConfiguration.class  major version = 52 (Java 8)  ✅
RagServiceImpl.class    major version = 52 (Java 8)  ✅
```

### 7.2 独立冒烟测试（不依赖 Spring，直连本地 Qdrant）

使用真实依赖与真实 Qdrant 执行「建集合 → 入库 3 条 → 语义检索 → 删除集合」：

```
[1] 构建 QdrantClient (gRPC localhost:6334)
[2] 初始化 embedding 模型 (AllMiniLmL6V2Quantized)  -> dimension = 384
[3] 创建集合 (Cosine, size=384)
[4] 入库 3 条菜品知识
[5] 语义检索: '有什么不辣的鸡肉菜?'
    score=0.7007  text=宫保鸡丁...   metadata={source=menu, dish=宫保鸡丁}
    score=0.6992  text=白切鸡...     metadata={source=menu, dish=白切鸡}
    score=0.6778  text=蛋炒饭...     metadata={source=menu, dish=蛋炒饭}
[6] 清理测试集合
SMOKE TEST OK
```

> 结论：Embedding 生成、向量入库、gRPC 通信、元数据 payload 往返、相似度排序**全部正确**。

### 7.3 Spring Boot 集成启动验证

完整启动应用（MySQL / Redis / RabbitMQ / Qdrant 均在运行），RAG 相关日志：

```
RagConfiguration : 初始化本地 Embedding 模型: AllMiniLmL6V2QuantizedEmbeddingModel (dimension=384)
RagConfiguration : 初始化 Qdrant 客户端: localhost:6334 (gRPC, tls=false)
RagConfiguration : 已创建 Qdrant 集合: shike_knowledge (dimension=384, distance=Cosine)
RagConfiguration : 初始化 QdrantEmbeddingStore: collection=shike_knowledge
ShikeApplication : Started ShikeApplication in 5.175 seconds
ShikeApplication : server started
```

Qdrant 侧确认：

```
GET /collections          -> {"collections":[{"name":"shike_knowledge"}]}
GET /collections/shike_knowledge
    -> status=green, vectors.size=384, vectors.distance=Cosine, points_count=0
```

> 结论：Spring  Bean 装配、集合自动创建、客户端连接**全部正常**。

---

## 8. 踩坑与解决方案

| 问题 | 现象 | 解决方案 |
| --- | --- | --- |
| LangChain4j 1.x 不兼容 | 无法在 Java 8 目标下加载 | 降级到 0.35.0 |
| `langchain4j-spring-boot-starter` 不兼容 | 需要 Spring Boot 3 / `jakarta.*` | 改为编程式 `@Configuration` |
| Qdrant 端口选错 | 连不上 | 必须用 **gRPC 6334**，不是 REST 6333 |
| 集合不存在 | 首次写入报错 | `QdrantEmbeddingStore` 不会自动建集合，在配置类中显式创建 |
| Bean 依赖顺序 | 用 `Object` 类型做依赖导致 `NoUniqueBeanDefinitionException` | 不再用 `Object` 占位，直接在 `embeddingStore()` 内保证集合存在 |
| `application*.yml` 被 gitignore | 配置无法随代码提交 | 属性类提供合理默认值，开箱即用；同时在报告中给出完整配置 |

---

## 9. 后续可扩展方向（本次未实现）

1. **接入 ChatModel（LLM）**：当前仅完成 RAG 的「检索」侧。若要完整问答，可再引入 `langchain4j-open-ai`（或 Ollama）并组合 `ContentRetriever` + `ChatModel`，或使用 `RetrievalAugmentor` 组装 `AiServices`。
2. **菜品知识自动入库**：新增启动任务 / 管理端接口，把 `dish.description` 批量写入 `RagService`，即可支撑「按口味/食材自然语言找菜」。
3. **结合既有“为你推荐”**：用 RAG 语义召回替代/增强当前基于销量与历史类目的推荐（`OrderController.getRecommandShopsByHistoryOrders()` 中已有相关 TODO 注释）。
4. **PDF/Word 文档解析**：可再加 `langchain4j-document-parser-apache-pdfbox`，支持知识库文档导入。
5. **集合与多租户**：按业务域拆分多个 collection（如 `menu`、`faq`、`policy`）。

---

## 10. 快速开始（TL;DR）

```bash
# 1. 启动 Qdrant
docker compose -f docker-compose.qdrant.yml up -d

# 2. 确认就绪
curl http://localhost:6333/readyz

# 3. 正常启动应用（配置默认即可用；如需自定义见第 6 节）
mvn -pl shike-server spring-boot:run

# 4. 在业务代码中注入 RagService 使用
```

---

## 附录 A：本次 RAG 接入文件清单（新增 / 修改 / 删除）

```text
shike-take-out/
├── docker-compose.qdrant.yml                        # ✅ 新增
├── RAG-INTEGRATION-REPORT.md                        # ✅ 新增（本报告）
├── pom.xml                                          # ✏️ 修改（langchain4j 版本管理，+34）
├── shike-common/
│   └── src/main/java/com/shike/properties/RagProperties.java        # ✅ 新增
└── shike-server/
    ├── pom.xml                                      # ✏️ 修改（引入依赖，+46）
    ├── src/main/java/com/shike/config/RagConfiguration.java          # ✅ 新增
    ├── src/main/java/com/shike/service/RagService.java               # ✅ 新增
    ├── src/main/java/com/shike/service/impl/RagServiceImpl.java      # ✅ 新增
    └── src/main/resources/application-dev.yml       # ✏️ 修改（shike.rag.*，该文件被 gitignore）

删除：无
重命名：无
```

统计：

| 类型 | 数量 | 文件 |
| --- | --- | --- |
| 新增 | 5（+本报告共 6） | `docker-compose.qdrant.yml`、`RagProperties.java`、`RagConfiguration.java`、`RagService.java`、`RagServiceImpl.java` |
| 修改 | 3 | `pom.xml`、`shike-server/pom.xml`、`application-dev.yml`（gitignore） |
| 删除 | 0 | — |
| 重命名 | 0 | — |

> 工作区中其余“已修改/未跟踪”文件属于本次 RAG 接入**之前**就存在的改动（含大量纯 CRLF 换行差异），不代表本次 RAG 接入的范围。详见正文 5.2 节。

## 附录 B：Docker 常用命令速查

```bash
docker compose -f docker-compose.qdrant.yml up -d        # 启动
docker compose -f docker-compose.qdrant.yml ps           # 状态
docker compose -f docker-compose.qdrant.yml logs -f      # 日志
docker compose -f docker-compose.qdrant.yml restart      # 重启
docker compose -f docker-compose.qdrant.yml down         # 停止（保留数据）
docker compose -f docker-compose.qdrant.yml down -v      # 停止并删除数据卷
docker exec -it shike-qdrant bash                        # 进入容器
curl http://localhost:6333/collections                   # REST 验证
```
