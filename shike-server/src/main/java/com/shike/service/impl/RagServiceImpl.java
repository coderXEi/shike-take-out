package com.shike.service.impl;

import com.shike.properties.RagProperties;
import com.shike.service.RagService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 检索服务默认实现：外部 Embedding 服务（本地 Ollama / 远程 API）+ Qdrant 向量库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shike.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagServiceImpl implements RagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingStoreIngestor embeddingStoreIngestor;
    private final RagProperties ragProperties;

    @Override
    public int ingest(String text) {
        return ingest(text, Collections.emptyMap());
    }

    @Override
    public int ingest(String text, Map<String, Object> metadata) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("RAG 入库文本为空，已跳过");
            return 0;
        }

        Metadata documentMetadata = metadata == null || metadata.isEmpty()
                ? new Metadata()
                : new Metadata(metadata);
        Document document = Document.from(text, documentMetadata);

        // 切分 + 向量化 + 写入 Qdrant
        embeddingStoreIngestor.ingest(document);

        // EmbeddingStoreIngestor 未回传条数，这里按切分结果估算用于日志/返回值
        int estimated = estimateSegments(text);
        log.info("RAG 文本已入库, 预估片段数={}, metadata={}", estimated, metadata);
        return estimated;
    }

    @Override
    public List<RagSearchResult> search(String query) {
        return search(query, ragProperties.getSearch().getMaxResults());
    }

    @Override
    public List<RagSearchResult> search(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("RAG 查询语句为空，返回空结果");
            return Collections.emptyList();
        }

        int topK = maxResults > 0 ? maxResults : ragProperties.getSearch().getMaxResults();
        double minScore = ragProperties.getSearch().getMinScore();

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                        EmbeddingSearchRequest.builder()
                                .queryEmbedding(queryEmbedding)
                                .maxResults(topK)
                                .minScore(minScore)
                                .build())
                .matches();

        log.info("RAG 检索完成, query='{}', topK={}, minScore={}, 命中={}",
                query, topK, minScore, matches.size());

        return matches.stream()
                .map(RagSearchResult::from)
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        if (embeddingStore instanceof dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore) {
            ((dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore) embeddingStore).clearStore();
            log.warn("RAG 向量库已清空");
        } else {
            embeddingStore.removeAll();
            log.warn("RAG 向量库已清空(removeAll)");
        }
    }

    /**
     * 粗略估算切分后的片段数量，仅用于日志与返回值展示。
     */
    private int estimateSegments(String text) {
        int maxSegmentSize = ragProperties.getEmbedding().getMaxSegmentSize();
        if (maxSegmentSize <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) text.length() / maxSegmentSize));
    }
}
