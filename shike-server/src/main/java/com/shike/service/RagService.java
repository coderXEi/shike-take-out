package com.shike.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;
import java.util.Map;

/**
 * RAG 检索服务。
 *
 * <p>提供最小可用的「入库 + 检索」能力，作为后续知识库问答、菜品语义搜索、
 * 相似店铺推荐等场景的基础设施。</p>
 */
public interface RagService {

    /**
     * 把一段纯文本写入向量库（自动切分、向量化、持久化到 Qdrant）。
     *
     * @param text 原始文本，可为长文本（会按配置切分）
     * @return 本次实际写入的向量条数
     */
    int ingest(String text);

    /**
     * 把一段纯文本连同元数据写入向量库。
     *
     * @param text     原始文本
     * @param metadata 元数据（如 source、dishId、categoryName），会一并存入 Qdrant payload，
     *                 检索时可通过返回结果的 metadata 读取
     * @return 本次实际写入的向量条数
     */
    int ingest(String text, Map<String, Object> metadata);

    /**
     * 语义检索：返回与 query 最相似的片段。
     *
     * @param query 查询语句
     * @return 匹配结果（按相似度降序）
     */
    List<RagSearchResult> search(String query);

    /**
     * 语义检索，并指定返回条数。
     *
     * @param query      查询语句
     * @param maxResults 最大返回条数；{@code <=0} 时使用配置默认值
     * @return 匹配结果（按相似度降序）
     */
    List<RagSearchResult> search(String query, int maxResults);

    /**
     * 清空向量库中当前集合的全部数据（谨慎使用）。
     */
    void clear();

    /**
     * 检索结果，屏蔽 langchain4j 内部类型，便于上层调用。
     */
    class RagSearchResult {

        private final double score;
        private final String text;
        private final Map<String, Object> metadata;

        public RagSearchResult(double score, String text, Map<String, Object> metadata) {
            this.score = score;
            this.text = text;
            this.metadata = metadata;
        }

        public static RagSearchResult from(EmbeddingMatch<TextSegment> match) {
            TextSegment segment = match.embedded();
            return new RagSearchResult(
                    match.score(),
                    segment == null ? null : segment.text(),
                    segment == null ? null : segment.metadata().toMap());
        }

        public double getScore() {
            return score;
        }

        public String getText() {
            return text;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        @Override
        public String toString() {
            return "RagSearchResult{score=" + score + ", text='" + text + "', metadata=" + metadata + '}';
        }
    }
}
