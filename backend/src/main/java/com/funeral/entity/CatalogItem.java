package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 服务/物品目录（公益基本服务、自选增值服务、政府补助项目统一目录） */
@Entity
@Table(name = "catalog_item")
@Getter
@Setter
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 24)
    private String category;

    @Column(nullable = false, length = 24)
    private String serviceClass;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(length = 16)
    private String unit = "项";

    /** 库存（寿衣、骨灰盒、花圈等物品）；服务类可为空 */
    private Integer stock;

    @Column(nullable = false)
    private Boolean refundable = true;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 500)
    private String description;

    /** 费用来源/定价依据，费用单中向家属展示 */
    @Column(length = 255)
    private String sourceNote;
}
