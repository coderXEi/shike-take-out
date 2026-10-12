package com.shike.controller.admin;

import com.shike.anno.NoAuth;
import com.shike.result.Result;
import com.shike.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
