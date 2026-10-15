package com.shike.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品（菜品 / 套餐）向量化数据模型。
 *
 * <p>作为 {@code ItemVectorTextBuilder} 的输入，承载「拼装 embedding 语料 + Qdrant 元信息」
 * 所需的全部字段。数据来源有两类：</p>
 * <ul>
 *     <li>批量重建：由 {@code ItemVectorIngestor} 查库（dish / setmeal / dish_flavor / setmeal_dish）
 *         组装后填充；</li>
 *     <li>实时向量化：商家新增 / 修改商品时按同一模型组装，保证与批量重建的语料口径一致。</li>
 * </ul>
 *
 * <p>{@link #type} 取值见 {@code VectorMetadataConstant.TYPE_DISH} / {@code TYPE_SETMEAL}；
 * 当调用 {@code build(item, type)} 显式传入 type 时，以入参为准。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVectorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据类型：dish（菜品）/ setmeal（套餐）
     */
    private String type;

    /**
     * 业务主键：dish.id 或 setmeal.id
     */
    private Long bizId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类 id
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 状态：0 停售/停用，1 起售/启用
     */
    private Integer status;

    /**
     * 更新时间（用于增量判断）
     */
    private LocalDateTime updateTime;

    /**
     * 口味词列表（菜品专用），由 dish_flavor.value 解析得到
     */
    private List<String> flavors;

    /**
     * 套餐成分描述列表（套餐专用），形如「扬州炒饭×1」
     */
    private List<String> setmealItems;
}
