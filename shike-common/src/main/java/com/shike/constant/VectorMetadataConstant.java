package com.shike.constant;

import java.math.BigDecimal;

/**
 * 商品向量（Qdrant）元信息常量。
 *
 * <p>商品向量与 RAG 知识库文档向量共用同一个 collection，
 * 依靠元信息中的 {@link #KEY_TYPE} 区分数据种类：<br>
 * {@code dish}（菜品）/ {@code setmeal}（套餐）/ RAG 文档（不含 type 或非上述取值）。</p>
 *
 * <p><b>注意</b>：langchain4j-qdrant 的元信息只支持标量类型
 * （String / UUID / Integer / Long / Double / Float / Boolean），
 * 不支持 List，因此口味、套餐成分等集合字段需拼成单一字符串后存入。</p>
 */
public class VectorMetadataConstant {

    // ==================== type 取值 ====================

    /**
     * 数据类型：菜品
     */
    public static final String TYPE_DISH = "dish";

    /**
     * 数据类型：套餐
     */
    public static final String TYPE_SETMEAL = "setmeal";

    // ==================== 元信息 key ====================

    /**
     * 数据类型（dish / setmeal），单 collection 内区分数据种类的关键过滤字段
     */
    public static final String KEY_TYPE = "type";

    /**
     * 业务主键（dish.id / setmeal.id），用于检索结果回表定位原记录
     */
    public static final String KEY_BIZ_ID = "biz_id";

    /**
     * 商品名称
     */
    public static final String KEY_NAME = "name";

    /**
     * 分类 id，用于按分类硬过滤
     */
    public static final String KEY_CATEGORY_ID = "category_id";

    /**
     * 分类名称，用于增强召回与展示
     */
    public static final String KEY_CATEGORY_NAME = "category_name";

    /**
     * 商品价格（double，标量）
     */
    public static final String KEY_PRICE = "price";

    /**
     * 价格档位（low / mid / high）
     */
    public static final String KEY_PRICE_LEVEL = "price_level";

    /**
     * 口味（多个以 {@code /} 拼接为单一字符串）
     */
    public static final String KEY_FLAVORS = "flavors";

    /**
     * 套餐成分（多个以 {@code /} 拼接为单一字符串）
     */
    public static final String KEY_SETMEAL_ITEMS = "setmeal_items";

    /**
     * 更新时间，用于增量判断（ISO 字符串）
     */
    public static final String KEY_UPDATED_AT = "updated_at";

    /**
     * 数据来源
     */
    public static final String KEY_SOURCE = "source";

    /**
     * 数据来源：MySQL 业务库
     */
    public static final String SOURCE_MYSQL = "mysql";

    // ==================== 价格档位 ====================

    /**
     * 价格档位：平价（&lt; 20 元）
     */
    public static final String PRICE_LEVEL_LOW = "low";

    /**
     * 价格档位：中档（20 ~ 60 元）
     */
    public static final String PRICE_LEVEL_MID = "mid";

    /**
     * 价格档位：高档（&gt; 60 元）
     */
    public static final String PRICE_LEVEL_HIGH = "high";

    /**
     * 价格档位中文展示：平价
     */
    public static final String PRICE_LEVEL_LOW_TEXT = "平价";

    /**
     * 价格档位中文展示：中档
     */
    public static final String PRICE_LEVEL_MID_TEXT = "中档";

    /**
     * 价格档位中文展示：高档
     */
    public static final String PRICE_LEVEL_HIGH_TEXT = "高档";

    /**
     * 中档价格下限（含）：20 元，{@code price >= 此值} 视为中档
     */
    public static final BigDecimal PRICE_MID_LOWER = new BigDecimal("20");

    /**
     * 高档价格下限（不含）：60 元，{@code price > 此值} 视为高档
     */
    public static final BigDecimal PRICE_HIGH_LOWER = new BigDecimal("60");

    private VectorMetadataConstant() {
    }
}
