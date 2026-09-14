package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 治丧订单条目：同一订单内同时包含 遗体接运/化妆/告别仪式/花圈挽联/寿衣/骨灰盒/餐饮/休息室/减免。
 * serviceClass 区分：PUBLIC_BASIC 公益基本服务 / OPTIONAL 自选增值服务 / SUBSIDY 政府补助项目。
 */
@Entity
@Table(name = "order_item", indexes = {
        @Index(name = "idx_order_item_order", columnList = "orderId")
})
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    private Long catalogId;

    @Column(nullable = false, length = 128)
    private String name;

    /** TRANSPORT 接运 / EMBALM 化妆整容 / FAREWELL 告别仪式 / WREATH 花圈挽联 /
     *  BURIAL_CLOTHES 寿衣 / URN 骨灰盒 / CATERING 餐饮 / REST_ROOM 休息室 / OTHER 其他 */
    @Column(nullable = false, length = 24)
    private String category;

    /** PUBLIC_BASIC 公益基本 / OPTIONAL 自选增值 / SUBSIDY 政府补助 */
    @Column(nullable = false, length = 24)
    private String serviceClass;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(length = 16)
    private String unit = "项";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** PENDING 待确认 / CONFIRMED 已确认 / REMOVED 已删减 */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(nullable = false)
    private Boolean refundable = true;

    /** CATALOG 服务目录 / CUSTOM 临时约定 / POLICY 政策减免 */
    @Column(length = 16)
    private String source = "CATALOG";

    /** 来源与定价依据（如：县物价局2024公示价 / 治丧政策第3条） */
    @Column(length = 255)
    private String sourceNote;

    private Long confirmedById;
    @Column(length = 64)
    private String confirmedByName;
    private LocalDateTime confirmedAt;

    /** 最近一次家属签字记录 id */
    private Long signatureId;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (quantity == null) quantity = 1;
        if (status == null) status = "PENDING";
        if (refundable == null) refundable = true;
    }
}
