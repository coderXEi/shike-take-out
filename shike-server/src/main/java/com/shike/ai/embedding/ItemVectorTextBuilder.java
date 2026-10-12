package com.shike.ai.embedding;

import com.alibaba.fastjson.JSON;
import com.shike.constant.VectorMetadataConstant;
import com.shike.dto.ItemVectorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品（菜品 / 套餐）向量语料与元信息构建器。
 *
 * <p><b>纯函数组件</b>：输入 {@link ItemVectorDTO}，输出「待 embedding 的文本 + Qdrant 元信息」。
 * 不查库、不入库、无状态。</p>
 *
 * <p>之所以单独抽出本类：批量重建（{@link ItemVectorIngestor}）与后续「商家新增商品时实时向量化」
 * 必须使用<b>完全一致</b>的拼接逻辑。若两边措辞不同，生成的向量不在同一语义分布上，
 * 检索时会出现同款商品互相召不回的问题。</p>
 *
 * <p>语料模板（字段顺序固定）：<br>
 * 菜品：{@code 菜品：名称；分类：xx；口味：xxx；描述：xxx；价格档位：中档(28元)}<br>
 * 套餐：{@code 套餐：名称；分类：xx；含：xxx；描述：xxx；价格档位：中档(38元)}</p>
 *
 * <p>元信息字段见 {@link VectorMetadataConstant}，其中 {@code type} 是单 collection
 * 内区分数据种类的关键过滤字段。</p>
 */
@Slf4j
@Component
public class ItemVectorTextBuilder {

    /**
     * 构建结果：embedding 文本 + Qdrant 元信息 + 向量分类（dish / setmeal）。
     */
    public static class VectorPayload {

        private final String text;
        private final Map<String, Object> metadata;

        public VectorPayload(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata;
        }

        public String getText() {
            return text;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * 菜品：构建语料与元信息。
     *
     * @param item 菜品宽表数据（type 应为 {@code dish}），其 {@code flavors} 来自 dish_flavor 解析
     * @return 文本 + 元信息
     */
    public VectorPayload buildForDish(ItemVectorDTO item) {
        return build(item, VectorMetadataConstant.TYPE_DISH);
    }

    /**
     * 套餐：构建语料与元信息。
     *
     * @param item 套餐宽表数据（type 应为 {@code setmeal}），其 {@code setmealItems} 为成分描述
     * @return 文本 + 元信息
     */
    public VectorPayload buildForSetmeal(ItemVectorDTO item) {
        return build(item, VectorMetadataConstant.TYPE_SETMEAL);
    }

    /**
     * 统一的语料与元信息构建入口。
     *
     * @param item 商品宽表数据
     * @param type 数据类型（dish / setmeal）；传入值优先于 {@code item.type}
     */
    public VectorPayload build(ItemVectorDTO item, String type) {
        if (item == null) {
            throw new IllegalArgumentException("商品向量数据不能为空");
        }
        String validType = type != null ? type : item.getType();
        String text = VectorMetadataConstant.TYPE_SETMEAL.equals(validType)
                ? buildSetmealText(item)
                : buildDishText(item);
        Map<String, Object> metadata = buildMetadata(item, validType);
        return new VectorPayload(text, metadata);
    }

    /**
     * 菜品语料：名称 + 分类 + 口味 + 描述 + 价格档位
     */
    private String buildDishText(ItemVectorDTO item) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("菜品：").append(safe(item.getName()));
        sb.append("；分类：").append(safe(item.getCategoryName()));
        if (item.getFlavors() != null && !item.getFlavors().isEmpty()) {
            sb.append("；口味：").append(String.join("、", item.getFlavors()));
        }
        sb.append("；描述：").append(safe(item.getDescription()));
        sb.append("；价格档位：").append(priceLevelText(item.getPrice()));
        return sb.toString();
    }

    /**
     * 套餐语料：名称 + 分类 + 含（成分） + 描述 + 价格档位
     *
     * <p>套餐没有口味，用 {@code setmeal_dish} 的成分列表替代「口味」位置，
     * 语义上更贴近「这个套餐是什么」。</p>
     */
    private String buildSetmealText(ItemVectorDTO item) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("套餐：").append(safe(item.getName()));
        sb.append("；分类：").append(safe(item.getCategoryName()));
        if (item.getSetmealItems() != null && !item.getSetmealItems().isEmpty()) {
            sb.append("；含：").append(String.join("、", item.getSetmealItems()));
        }
        sb.append("；描述：").append(safe(item.getDescription()));
        sb.append("；价格档位：").append(priceLevelText(item.getPrice()));
        return sb.toString();
    }

    /**
     * 元信息：定位回表 + 硬过滤 + 增量判断三组字段。
     */
    private Map<String, Object> buildMetadata(ItemVectorDTO item, String type) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        // 定位原信息用
        metadata.put(VectorMetadataConstant.KEY_TYPE, type);
        metadata.put(VectorMetadataConstant.KEY_BIZ_ID, item.getBizId());
        metadata.put(VectorMetadataConstant.KEY_NAME, item.getName());
        // 过滤用
        metadata.put(VectorMetadataConstant.KEY_CATEGORY_ID, item.getCategoryId());
        metadata.put(VectorMetadataConstant.KEY_CATEGORY_NAME, item.getCategoryName());
        if (item.getPrice() != null) {
            metadata.put(VectorMetadataConstant.KEY_PRICE, item.getPrice().doubleValue());
            metadata.put(VectorMetadataConstant.KEY_PRICE_LEVEL, priceLevel(item.getPrice()));
        }
        // 增强召回与排序
        if (item.getFlavors() != null && !item.getFlavors().isEmpty()) {
            // 注意：langchain4j-qdrant 的 ValueMapFactory 只支持标量类型
            // (String/UUID/Integer/Long/Double/Float/Boolean)，不支持 List，
            // 因此口味必须拼成单一字符串存入。
            metadata.put(VectorMetadataConstant.KEY_FLAVORS, String.join("/", item.getFlavors()));
        }
        if (item.getSetmealItems() != null && !item.getSetmealItems().isEmpty()) {
            // 同上：套餐成分也必须是标量字符串
            metadata.put(VectorMetadataConstant.KEY_SETMEAL_ITEMS, String.join("/", item.getSetmealItems()));
        }
        if (item.getUpdateTime() != null) {
            metadata.put(VectorMetadataConstant.KEY_UPDATED_AT, item.getUpdateTime().toString());
        }
        metadata.put(VectorMetadataConstant.KEY_SOURCE, VectorMetadataConstant.SOURCE_MYSQL);
        // Map<String,Object>
        return metadata;
    }

    /**
     * 计算价格档位：&lt;20 平价 / 20~60 中档 / &gt;60 高档。
     *
     * @param price 价格，可为空（为空按中档处理）
     */
    public String priceLevel(BigDecimal price) {
        if (price == null) {
            return VectorMetadataConstant.PRICE_LEVEL_MID;
        }
        if (price.compareTo(VectorMetadataConstant.PRICE_HIGH_LOWER) > 0) {
            return VectorMetadataConstant.PRICE_LEVEL_HIGH;
        }
        if (price.compareTo(VectorMetadataConstant.PRICE_MID_LOWER) >= 0) {
            return VectorMetadataConstant.PRICE_LEVEL_MID;
        }
        return VectorMetadataConstant.PRICE_LEVEL_LOW;
    }

    /**
     * 价格档位的中文展示，如 {@code 中档(28元)}，用于拼入语料。
     */
    private String priceLevelText(BigDecimal price) {
        String level = priceLevel(price);
        String levelText;
        if (VectorMetadataConstant.PRICE_LEVEL_LOW.equals(level)) {
            levelText = VectorMetadataConstant.PRICE_LEVEL_LOW_TEXT;
        } else if (VectorMetadataConstant.PRICE_LEVEL_HIGH.equals(level)) {
            levelText = VectorMetadataConstant.PRICE_LEVEL_HIGH_TEXT;
        } else {
            levelText = VectorMetadataConstant.PRICE_LEVEL_MID_TEXT;
        }
        return price == null ? levelText : levelText + "(" + price.stripTrailingZeros().toPlainString() + "元)";
    }

    /**
     * 解析 {@code dish_flavor.value}（形如 {@code ["微辣","中辣","特辣"]}）为口味词列表。
     *
     * <p>解析失败时不抛异常，退化为「按逗号切分并去掉引号括号」，保证批量入库不因单条脏数据中断。</p>
     *
     * @param value dish_flavor.value 原始字符串
     * @return 口味词列表，无法解析时返回空列表（不会返回 null）
     */
    public List<String> parseFlavorValues(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return result;
        }
        try {
            List<String> parsed = JSON.parseArray(value.trim(), String.class);
            if (parsed != null) {
                for (String v : parsed) {
                    if (v != null && !v.trim().isEmpty()) {
                        result.add(v.trim());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析 dish_flavor.value 失败，退化为字符串切分: value={}, err={}", value, e.getMessage());
            String cleaned = value.replaceAll("[\\[\\]\"']", "");
            for (String v : cleaned.split(",")) {
                if (!v.trim().isEmpty()) {
                    result.add(v.trim());
                }
            }
            return result;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
