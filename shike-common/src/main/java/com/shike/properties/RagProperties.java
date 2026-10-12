package com.shike.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG（检索增强生成）相关配置。
 *
 * <p>对应 application.yml 中 {@code shike.rag.*}。默认值面向“本地 Docker + Ollama 开箱即用”，
 * 即：外部 Embedding 服务（本地 Ollama 或远程 OpenAI 兼容 API）+ 本地 Qdrant（gRPC 6334）。</p>
 */
@Component
@ConfigurationProperties(prefix = "shike.rag")
@Data
public class RagProperties {

    /** 是否启用 RAG 装配。关闭后不会创建任何 Qdrant / Embedding Bean，避免拖慢启动。 */
    private boolean enabled = true;

    /** 是否在应用启动时就创建 Qdrant 集合（不存在时）。 */
    private boolean initCollectionOnStartup = true;

    private Qdrant qdrant = new Qdrant();

    /**
     * Embedding 模型配置。
     *
     * <p>Embedding 模型不在 JVM 进程内运行，而是调用外部服务：
     * 本地 Ollama 或 OpenAI 兼容的远程 API。</p>
     */
    private Embedding embedding = new Embedding();

    private Search search = new Search();

    /**
     * Qdrant 向量库连接参数。
     *
     * <p>注意：langchain4j-qdrant 通过 gRPC 通信，port 应填 gRPC 端口（默认 6334），
     * 而不是 REST 端口 6333。</p>
     */
    @Data
    public static class Qdrant {

        /** Qdrant 主机。 */
        private String host = "localhost";

        /** Qdrant gRPC 端口。 */
        private int port = 6334;

        /** 是否使用 TLS。本地开发为 false。 */
        private boolean useTls = false;

        /** 可选的 API Key，本地未开启鉴权时留空。 */
        private String apiKey;

        /** 集合名称。 */
        private String collectionName = "shike_knowledge";

        /** 文本在 Qdrant payload 中的字段名。 */
        private String payloadTextKey = "text";

        /** 向量维度。默认值应与 {@code shike.rag.embedding.dimension} 保持一致。 */
        private int dimension = 768;

        /** 连接/请求超时（秒）。 */
        private long timeoutSeconds = 10;
    }

    /**
     * Embedding 模型配置。
     *
     * <p>支持两种提供方（{@link #provider}）：</p>
     * <ul>
     *     <li>{@code ollama}：本地部署的 Ollama，走 HTTP（默认端点 http://localhost:11434）；</li>
     *     <li>{@code openai}：OpenAI 兼容的远程 Embedding API（可指向任意兼容网关）。</li>
     * </ul>
     *
     * <p>同时提供文本切分参数，用于把长文档切成若干 TextSegment 再向量化。</p>
     */
    @Data
    public static class Embedding {

        /**
         * Embedding 提供方，可选值：{@code ollama}、{@code openai}。
         */
        private String provider = "ollama";

        /**
         * 模型服务地址（base url）。
         *
         * <ul>
         *     <li>ollama：默认 {@code http://localhost:11434}</li>
         *     <li>openai：默认 {@code https://api.openai.com/v1}，远程网关请填写其兼容地址</li>
         * </ul>
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * Embedding 模型名称。
         *
         * <ul>
         *     <li>ollama 常用：{@code nomic-embed-text}（768 维）、{@code bge-m3}（1024 维）</li>
         *     <li>openai 常用：{@code text-embedding-3-small}（1536 维）</li>
         * </ul>
         */
        private String modelName = "nomic-embed-text";

        /** API Key。ollama 本地部署留空；openai / 远程网关按需填写。 */
        private String apiKey;

        /**
         * 向量维度。
         *
         * <p>必须与所选模型实际输出维度一致，且与 Qdrant 集合维度匹配。
         * 常用值：nomic-embed-text=768、bge-m3=1024、text-embedding-3-small=1536。</p>
         */
        private int dimension = 768;

        /** 单个分段最大字符数。 */
        private int maxSegmentSize = 500;

        /** 相邻分段之间的重叠字符数，避免语义在边界被切断。 */
        private int maxOverlapSize = 50;

        /** 请求外部 Embedding 服务的超时（秒）。 */
        private long timeoutSeconds = 60;

        /** 是否记录 Embedding 请求/响应日志（调试用）。 */
        private boolean logRequests = false;

        /** 是否记录 Embedding 响应内容（调试用）。 */
        private boolean logResponses = false;
    }

    /**
     * 检索参数。
     */
    @Data
    public static class Search {

        /** 默认返回的最大结果数。 */
        private int maxResults = 5;

        /** 最小相似度阈值（0~1，越大越严格）。 */
        private double minScore = 0.6;
    }
}
