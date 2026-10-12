package com.shike.ai.embedding;

import com.shike.constant.VectorMetadataConstant;
import com.shike.dto.ItemVectorDTO;
import com.shike.entity.DishFlavor;
import com.shike.mapper.DishFlavorMapper;
import com.shike.mapper.DishMapper;
import com.shike.mapper.SetmealMapper;
import com.shike.service.RagService;
import com.shike.vo.DishItemVO;
import com.shike.vo.DishVO;
import com.shike.vo.SetmealVO;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品向量批量重建服务。
 *
 * <p>把「在售菜品 + 启用套餐」全量向量化后写入 Qdrant（与 RAG 知识库共用同一个 collection，
 * 依靠 {@code metadata.type} 区分）。</p>
 *
 * <p><b>幂等策略</b>：重建前按 {@code type} 维度删除旧向量（{@code removeAll(Filter)}），
 * 只清理 {@code dish} / {@code setmeal} 两类数据，<b>不会</b>动到 RAG 知识库的文档向量。
 * 这样重复执行不会产生重复向量（Qdrant 不按业务 id 去重，必须显式清理）。</p>
 *
 * <p><b>入库范围</b>：只入库在售商品（dish.status = 1 / setmeal.status = 1），
 * 停售商品既不占向量也不参与检索。</p>
 *
 * <p>本类只负责「查库 → 拼语料 → 写入」的批处理编排；语料与元信息的拼接规则
 * 全部委托给 {@link ItemVectorTextBuilder}，保证与后续实时向量化逻辑一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shike.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ItemVectorIngestor {

    private final DishMapper dishMapper;

    private final SetmealMapper setmealMapper;

    private final DishFlavorMapper dishFlavorMapper;

    private final ItemVectorTextBuilder textBuilder;

    private final RagService ragService;

    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 全量重建：菜品 + 套餐。
     *
     * @param reset 是否先按 type 清理旧向量；首次导入或数据大改时传 true
     * @return 执行结果统计
     */
    public RebuildResult rebuildAll(boolean reset) {
        if (reset) {
            clearByType(VectorMetadataConstant.TYPE_DISH);
            clearByType(VectorMetadataConstant.TYPE_SETMEAL);
        }
        long dishCount = rebuildDishes();
        long setmealCount = rebuildSetmeals();

        RebuildResult result = new RebuildResult(dishCount, setmealCount);
        log.info("商品向量全量重建完成: reset={}, 菜品={}, 套餐={}", reset, dishCount, setmealCount);
        return result;
    }

    /**
     * 全量重建菜品向量。
     *
     * @return 成功入库的菜品条数
     */
    public long rebuildDishes() {
        List<DishVO> dishes = dishMapper.listOnSaleWithCategory();
        if (dishes == null || dishes.isEmpty()) {
            log.warn("没有在售菜品，跳过菜品向量重建");
            return 0;
        }

        // 一次性取全口味，避免逐条查库（N+1）
        List<Long> dishIds = dishes.stream().map(DishVO::getId).collect(Collectors.toList());
        Map<Long, List<String>> flavorMap = loadFlavorMap(dishIds);

        long success = 0;
        for (DishVO dish : dishes) {
            try {
                // 构造元信息
                ItemVectorDTO item = ItemVectorDTO.builder()
                        .type(VectorMetadataConstant.TYPE_DISH)
                        .bizId(dish.getId())
                        .name(dish.getName())
                        .categoryId(dish.getCategoryId())
                        .categoryName(dish.getCategoryName())
                        .price(dish.getPrice())
                        .description(dish.getDescription())
                        .status(dish.getStatus())
                        .updateTime(dish.getUpdateTime())
                        .flavors(flavorMap.getOrDefault(dish.getId(), Collections.emptyList()))
                        .build();

                // VectorPayload  = text + metadata
                ItemVectorTextBuilder.VectorPayload payload = textBuilder.buildForDish(item);
                // 向量化并存入  向量化是在向量数据库内执行的
                ragService.ingest(payload.getText(), payload.getMetadata());
                success++;
            } catch (Exception e) {
                // 单条失败不影响整体，记录后继续
                log.error("菜品向量入库失败: dishId={}, name={}", dish.getId(), dish.getName(), e);
            }
        }
        log.info("菜品向量重建完成: 总数={}, 成功={}", dishes.size(), success);
        return success;
    }

    /**
     * 全量重建套餐向量。
     *
     * @return 成功入库的套餐条数
     */
    public long rebuildSetmeals() {
        List<SetmealVO> setmeals = setmealMapper.listOnSaleWithCategory();
        if (setmeals == null || setmeals.isEmpty()) {
            log.warn("没有启用套餐，跳过套餐向量重建");
            return 0;
        }

        long success = 0;
        for (SetmealVO setmeal : setmeals) {
            try {
                ItemVectorDTO item = ItemVectorDTO.builder()
                        .type(VectorMetadataConstant.TYPE_SETMEAL)
                        .bizId(setmeal.getId())
                        .name(setmeal.getName())
                        .categoryId(setmeal.getCategoryId())
                        .categoryName(setmeal.getCategoryName())
                        .price(setmeal.getPrice())
                        .description(setmeal.getDescription())
                        .status(setmeal.getStatus())
                        .updateTime(setmeal.getUpdateTime())
                        .setmealItems(loadSetmealItems(setmeal.getId()))
                        .build();

                ItemVectorTextBuilder.VectorPayload payload = textBuilder.buildForSetmeal(item);
                ragService.ingest(payload.getText(), payload.getMetadata());
                success++;
            } catch (Exception e) {
                log.error("套餐向量入库失败: setmealId={}, name={}", setmeal.getId(), setmeal.getName(), e);
            }
        }
        log.info("套餐向量重建完成: 总数={}, 成功={}", setmeals.size(), success);
        return success;
    }

    /**
     * 按 type 删除向量（只清理商品类数据，不影响 RAG 知识库文档）。
     *
     * @param type {@code dish} 或 {@code setmeal}
     */
    public void clearByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            log.warn("clearByType 的 type 为空，跳过清理");
            return;
        }
        try {
            Filter filter = new IsEqualTo(VectorMetadataConstant.KEY_TYPE, type);
            embeddingStore.removeAll(filter);
            log.info("已清理向量: type={}", type);
        } catch (Exception e) {
            // 集合为空或过滤无匹配时可能抛异常，不影响后续写入
            log.warn("清理向量失败(可能集合为空): type={}, err={}", type, e.getMessage());
        }
    }

    /**
     * 批量查口味并按 dishId 分组，解析 value 的 JSON 数组为口味词。
     */
    private Map<Long, List<String>> loadFlavorMap(List<Long> dishIds) {
        Map<Long, List<String>> map = new HashMap<>();
        if (dishIds.isEmpty()) {
            return map;
        }
        List<DishFlavor> flavors = dishFlavorMapper.getByDishIds(dishIds);
        if (flavors == null || flavors.isEmpty()) {
            return map;
        }
        for (DishFlavor flavor : flavors) {
            List<String> values = textBuilder.parseFlavorValues(flavor.getValue());
            if (values.isEmpty()) {
                continue;
            }
            map.computeIfAbsent(flavor.getDishId(), k -> new ArrayList<>()).addAll(values);
        }
        return map;
    }

    /**
     * 取套餐成分描述，格式为「菜名×份数」，如 {@code 扬州炒饭×1}。
     */
    private List<String> loadSetmealItems(Long setmealId) {
        List<DishItemVO> items = setmealMapper.getDishItemBySetmealId(setmealId);
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(items.size());
        for (DishItemVO item : items) {
            if (item.getName() == null) {
                continue;
            }
            int copies = item.getCopies() == null ? 1 : item.getCopies();
            result.add(item.getName() + "×" + copies);
        }
        return result;
    }

    /**
     * 重建结果统计。
     */
    public static class RebuildResult {

        private final long dishCount;
        private final long setmealCount;

        public RebuildResult(long dishCount, long setmealCount) {
            this.dishCount = dishCount;
            this.setmealCount = setmealCount;
        }

        public long getDishCount() {
            return dishCount;
        }

        public long getSetmealCount() {
            return setmealCount;
        }
    }
}
