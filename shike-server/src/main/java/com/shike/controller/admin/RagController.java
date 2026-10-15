package com.shike.controller.admin;

import com.shike.anno.NoAuth;
import com.shike.ai.embedding.ItemVectorIngestor;
import com.shike.constant.VectorMetadataConstant;
import com.shike.result.Result;
import com.shike.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * RAG 调试接口：文档入库 + 语义检索 + 问答。
 *
 * <p>仅在 {@code shike.rag.enabled=true} 时装配（此时 {@link RagService} 才存在）。</p>
 *
 * <p>用法（服务端口 8080）：</p>
 * <ol>
 *     <li>导入内置文档：{@code POST /admin/rag/import?path=classpath*:docs/*.md}</li>
 *     <li>语义检索：{@code GET /admin/rag/search?query=费曼技巧是什么}</li>
 *     <li>RAG 问答：{@code GET /admin/rag/chat?msg=费曼技巧是什么}</li>
 *     <li>清空向量库：{@code POST /admin/rag/clear}</li>
 *     <li>商品向量全量重建：{@code POST /admin/rag/rebuild-items?reset=true}</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/admin/rag")
@RequiredArgsConstructor
@Tag(name = "RAG 调试接口")
@ConditionalOnProperty(prefix = "shike.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagController {

    private final RagService ragService;

    private final ItemVectorIngestor itemVectorIngestor;

    /**
     * 把 classpath / 文件系统下的文档批量入库。
     *
     * @param path Spring Resource 通配路径，默认 {@code classpath*:docs/*.md}
     */
    @NoAuth
    @PostMapping("/import")
    @Operation(summary = "导入文档到向量库")
    public Result<Map<String, Object>> importDocs(
            @RequestParam(defaultValue = "classpath*:docs/*.md") String path) {
        log.info("RAG 文档导入开始: path={}", path);

        List<String> importedFiles = new ArrayList<>();
        int totalSegments = 0;

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(path);
            if (resources.length == 0) {
                return Result.error("未匹配到任何文档: " + path);
            }

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String text = readUtf8(resource);
                if (text == null || text.trim().isEmpty()) {
                    log.warn("文档内容为空，已跳过: {}", filename);
                    continue;
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("file_name", filename);
                metadata.put("source", "classpath:" + path);

                totalSegments += ragService.ingest(text, metadata);
                importedFiles.add(filename);
                log.info("文档已入库: {}, 字符数={}", filename, text.length());
            }
        } catch (Exception e) {
            log.error("RAG 文档导入失败: path={}", path, e);
            return Result.error("文档导入失败: " + e.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("files", importedFiles);
        data.put("fileCount", importedFiles.size());
        data.put("estimatedSegments", totalSegments);
        log.info("RAG 文档导入完成: 文件数={}, 预估片段数={}", importedFiles.size(), totalSegments);
        return Result.success(data);
    }

    /**
     * 语义检索：只走向量库，不调用大模型，用于验证 embedding + 检索是否正常。
     */
    @NoAuth
    @GetMapping("/search")
    @Operation(summary = "语义检索向量库")
    public Result<List<RagService.RagSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int maxResults) {
        log.info("RAG 检索请求: query={}, maxResults={}", query, maxResults);
        List<RagService.RagSearchResult> results = ragService.search(query, maxResults);
        return Result.success(results);
    }

    /**
     * 清空当前集合的全部向量数据。
     */
    @NoAuth
    @PostMapping("/clear")
    @Operation(summary = "清空向量库")
    public Result<String> clear() {
        log.warn("RAG 向量库清空请求");
        ragService.clear();
        return Result.success("已清空");
    }

    /**
     * 商品向量全量重建：把「在售菜品 + 启用套餐」重新向量化写入 Qdrant。
     *
     * <p>与 RAG 知识库文档共用同一个 collection，依靠 {@code metadata.type}
     * （{@code dish} / {@code setmeal}）区分，重建不会影响知识库文档向量。</p>
     *
     * @param reset 是否先按 type 清理旧向量；首次导入或商品数据大改时传 true。
     *              传 false 时直接追加写入（Qdrant 不按业务 id 去重，可能产生重复向量）
     */
    @NoAuth
    @PostMapping("/rebuild-items")
    @Operation(summary = "商品向量全量重建")
    public Result<Map<String, Object>> rebuildItems(
            @RequestParam(defaultValue = "true") boolean reset) {
        log.info("商品向量全量重建开始: reset={}", reset);
        try {
            ItemVectorIngestor.RebuildResult result = itemVectorIngestor.rebuildAll(reset);
            Map<String, Object> data = new HashMap<>();
            data.put("dishCount", result.getDishCount());
            data.put("setmealCount", result.getSetmealCount());
            log.info("商品向量全量重建完成: 菜品={}, 套餐={}", result.getDishCount(), result.getSetmealCount());
            return Result.success(data);
        } catch (Exception e) {
            log.error("商品向量全量重建失败", e);
            return Result.error("商品向量重建失败: " + e.getMessage());
        }
    }

    /**
     * 仅重建菜品向量。
     */
    @NoAuth
    @PostMapping("/rebuild-dishes")
    @Operation(summary = "重建菜品向量")
    public Result<Map<String, Object>> rebuildDishes(
            @RequestParam(defaultValue = "true") boolean reset) {
        log.info("菜品向量重建开始: reset={}", reset);
        try {
            if (reset) {
                itemVectorIngestor.clearByType(VectorMetadataConstant.TYPE_DISH);
            }
            long count = itemVectorIngestor.rebuildDishes();
            Map<String, Object> data = new HashMap<>();
            data.put("dishCount", count);
            return Result.success(data);
        } catch (Exception e) {
            log.error("菜品向量重建失败", e);
            return Result.error("菜品向量重建失败: " + e.getMessage());
        }
    }

    /**
     * 仅重建套餐向量。
     */
    @NoAuth
    @PostMapping("/rebuild-setmeals")
    @Operation(summary = "重建套餐向量")
    public Result<Map<String, Object>> rebuildSetmeals(
            @RequestParam(defaultValue = "true") boolean reset) {
        log.info("套餐向量重建开始: reset={}", reset);
        try {
            if (reset) {
                itemVectorIngestor.clearByType(VectorMetadataConstant.TYPE_SETMEAL);
            }
            long count = itemVectorIngestor.rebuildSetmeals();
            Map<String, Object> data = new HashMap<>();
            data.put("setmealCount", count);
            return Result.success(data);
        } catch (Exception e) {
            log.error("套餐向量重建失败", e);
            return Result.error("套餐向量重建失败: " + e.getMessage());
        }
    }

    /**
     * 批量入库：把多段文本连同元数据一次性写入向量库（用于脚本化导入商品/知识片段）。
     *
     * <p>请求体为 JSON 数组，每项形如：<br>
     * {@code [{"text":"宫保鸡丁...","metadata":{"type":"dish","biz_id":1}}]}</p>
     *
     * @param items 待入库文本及元数据
     */
    @NoAuth
    @PostMapping("/ingest-batch")
    @Operation(summary = "批量入库文本")
    public Result<Map<String, Object>> ingestBatch(@RequestBody List<IngestItem> items) {
        if (items == null || items.isEmpty()) {
            return Result.error("待入库文本为空");
        }
        log.info("批量入库开始: 条数={}", items.size());
        int success = 0;
        int failed = 0;
        for (IngestItem item : items) {
            if (item == null || item.getText() == null || item.getText().trim().isEmpty()) {
                failed++;
                continue;
            }
            try {
                ragService.ingest(item.getText(), item.getMetadata());
                success++;
            } catch (Exception e) {
                failed++;
                log.error("批量入库单条失败: text={}", item.getText(), e);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", items.size());
        data.put("success", success);
        data.put("failed", failed);
        log.info("批量入库完成: 总数={}, 成功={}, 失败={}", items.size(), success, failed);
        return Result.success(data);
    }

    /**
     * 批量入库的单条入参。
     */
    @Data
    public static class IngestItem {

        /**
         * 原始文本
         */
        private String text;

        /**
         * 元数据（可选），键值均为标量
         */
        private Map<String, Object> metadata;
    }

    /**
     * 以 UTF-8 读取资源内容。
     *
     * <p>注意：不能直接用 {@code Scanner(InputStream)}，在 Windows 下会按
     * {@code file.encoding}(GBK) 解码，导致中文乱码。</p>
     */
    private String readUtf8(Resource resource) throws Exception {
        try (InputStream in = resource.getInputStream();
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
