package com.shike.ai.config;

import com.shike.properties.RagProperties;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RAG（检索增强生成）基础设施配置。
 *  嵌入模型、Store、多种内容 召回器
 *
 * <p>装配内容（均为单例，随应用生命周期存活）：</p>
 * <ol>
 *     <li>{@link QdrantClient}      —— gRPC 客户端，连接本地 Qdrant；</li>
 *     <li>{@link EmbeddingModel}    —— 外部 Embedding 服务客户端（本地 Ollama 或 OpenAI 兼容远程 API），
 *         模型在服务端运行，JVM 进程内不再加载任何模型；</li>
 *     <li>{@link EmbeddingStore}    —— Qdrant 向量库适配器；集合不存在时按配置自动创建；</li>
 *     <li>{@link EmbeddingStoreIngestor} —— 文档切分 + 向量化 + 入库的一站式工具；</li>
 *     <li>{@link ContentRetriever}  —— 供上层（如 RAG 问答）直接复用的检索器。</li>
 * </ol>
 *
 * <p>可通过 {@code shike.rag.enabled=false} 整体关闭。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "shike.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagConfiguration {

    /**
     * 共享的 Qdrant gRPC 客户端。
     *
     * <p>langchain4j-qdrant 0.35.0 的 {@code QdrantEmbeddingStore} 既支持内部自建客户端，
     * 也支持注入外部客户端（{@code new QdrantEmbeddingStore(client, collection, payloadTextKey)}）。
     * 这里使用外部注入方式，避免同时建立多条 gRPC 连接，并便于统一在容器关闭时释放。</p>
     */
    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient(RagProperties properties) {
        RagProperties.Qdrant qdrant = properties.getQdrant();
        log.info("初始化 Qdrant 客户端: {}:{} (gRPC, tls={})",
                qdrant.getHost(), qdrant.getPort(), qdrant.isUseTls());

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                qdrant.getHost(), qdrant.getPort(), qdrant.isUseTls());
        if (qdrant.getApiKey() != null && !qdrant.getApiKey().trim().isEmpty()) {
            builder.withApiKey(qdrant.getApiKey().trim());
        }
        builder.withTimeout(Duration.ofSeconds(qdrant.getTimeoutSeconds()));
        return new QdrantClient(builder.build());
    }

    /**
     * 外部 Embedding 模型客户端。
     *
     * <p>Embedding 模型不再在 JVM 进程内运行，而是通过 HTTP 调用外部服务：</p>
     * <ul>
     *     <li>{@code provider=ollama}：本地 Ollama（默认 http://localhost:11434），
     *         例如 {@code nomic-embed-text}、{@code bge-m3}；</li>
     *     <li>{@code provider=openai}：OpenAI 兼容的远程 Embedding API，
     *         例如 {@code text-embedding-3-small}，也可指向任意兼容网关。</li>
     * </ul>
     */
    @Bean
    public EmbeddingModel embeddingModel(RagProperties properties) {
        RagProperties.Embedding embedding = properties.getEmbedding();
        String provider = embedding.getProvider() == null
                ? "ollama" : embedding.getProvider().trim().toLowerCase();
        log.info("初始化外部 Embedding 模型: provider={}, baseUrl={}, model={}, dimension={}",
                provider, embedding.getBaseUrl(), embedding.getModelName(), embedding.getDimension());

        if ("openai".equals(provider)) {
            OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                    .httpClientBuilder(new JdkHttpClientBuilder())
                    .baseUrl(embedding.getBaseUrl())
                    .modelName(embedding.getModelName())
                    .timeout(Duration.ofSeconds(embedding.getTimeoutSeconds()))
                    .logRequests(embedding.isLogRequests())
                    .logResponses(embedding.isLogResponses());
            if (embedding.getApiKey() != null && !embedding.getApiKey().trim().isEmpty()) {
                builder.apiKey(embedding.getApiKey().trim());
            }
            return builder.build();
        }

        if ("ollama".equals(provider)) {
            OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
                    .httpClientBuilder(new JdkHttpClientBuilder())
                    .baseUrl(embedding.getBaseUrl())
                    .modelName(embedding.getModelName())
                    .timeout(Duration.ofSeconds(embedding.getTimeoutSeconds()))
                    .logRequests(embedding.isLogRequests())
                    .logResponses(embedding.isLogResponses());
            return builder.build();
        }

        throw new IllegalStateException(
                "不支持的 shike.rag.embedding.provider=" + embedding.getProvider()
                        + "，可选值：ollama、openai");
    }

    /**
     * Qdrant 向量库，作为 {@code EmbeddingStore<TextSegment>} 暴露。
     *
     * <p>在创建 store 之前先确保集合存在：langchain4j-qdrant 0.35.0 在写入时不会自动建集合，
     * 因此这里按配置的维度与 Cosine 距离显式创建（已存在则跳过）。</p>
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(QdrantClient qdrantClient,
                                                      RagProperties properties) {
        RagProperties.Qdrant qdrant = properties.getQdrant();
        ensureCollection(qdrantClient, properties);
        log.info("初始化 QdrantEmbeddingStore: collection={}", qdrant.getCollectionName());
        return new QdrantEmbeddingStore(qdrantClient, qdrant.getCollectionName(), qdrant.getPayloadTextKey());
    }

    /**
     * 文档入库工具：自动完成「切分 -> 向量化 -> 写入向量库」。
     */
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(EmbeddingModel embeddingModel,
                                                         EmbeddingStore<TextSegment> embeddingStore,
                                                         RagProperties properties) {
        return EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(DocumentSplitters.recursive(
                        properties.getEmbedding().getMaxSegmentSize(),
                        properties.getEmbedding().getMaxOverlapSize()))
                .build();
    }

    /**
     * 检索器：把用户查询向量化后在 Qdrant 中做相似度检索，供 RAG 流程复用。
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel,
                                             RagProperties properties) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(properties.getSearch().getMaxResults())
                .minScore(properties.getSearch().getMinScore())
                .build();
    }

    /**
     * 集合不存在时创建；维度与距离需与 embedding 模型一致（Cosine）。
     *
     * <p>维度取自 {@code shike.rag.embedding.dimension}，必须与所选外部模型输出维度一致。</p>
     */
    private void ensureCollection(QdrantClient client, RagProperties properties) {
        if (!properties.isInitCollectionOnStartup()) {
            log.info("shike.rag.init-collection-on-startup=false，跳过 Qdrant 集合初始化");
            return;
        }
        RagProperties.Qdrant qdrant = properties.getQdrant();
        String collection = qdrant.getCollectionName();
        try {
            Boolean exists = client.collectionExistsAsync(collection)
                    .get(qdrant.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(exists)) {
                log.info("Qdrant 集合已存在: {}", collection);
                return;
            }

            Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                    .setSize(properties.getEmbedding().getDimension())
                    .setDistance(Collections.Distance.Cosine)
                    .build();
            client.createCollectionAsync(collection, vectorParams)
                    .get(qdrant.getTimeoutSeconds(), TimeUnit.SECONDS);
            log.info("已创建 Qdrant 集合: {} (dimension={}, distance=Cosine)",
                    collection, properties.getEmbedding().getDimension());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("初始化 Qdrant 集合被中断: " + collection, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException(
                    "初始化 Qdrant 集合失败，请确认 Qdrant 已启动且 gRPC 端口可达: "
                            + qdrant.getHost() + ":" + qdrant.getPort(), e);
        }
    }
}
