package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资源占用记录：接运车辆 VEHICLE / 馆内冷藏位 COLD / 告别厅 HALL / 火化炉 FURNACE。
 * 通过时间段重叠判断冲突；状态 HELD 预占 -> CONFIRMED 确认 / RELEASED 释放。
 */
@Entity
@Table(name = "resource_booking", indexes = {
        @Index(name = "idx_booking_resource", columnList = "resourceType,resourceId"),
        @Index(name = "idx_booking_order", columnList = "orderId")
})
@Getter
@Setter
public class ResourceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    /** VEHICLE / COLD / HALL / FURNACE */
    @Column(nullable = false, length = 16)
    private String resourceType;

    @Column(nullable = false)
    private Long resourceId;

    @Column(nullable = false, length = 128)
    private String resourceName;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    /** HELD 预占 / CONFIRMED 已确认占用 / SUSPENDED 异常暂停锁定 / RELEASED 已释放 */
    @Column(nullable = false, length = 16)
    private String status = "HELD";

    // ---------------- 跨区域接运信息 ----------------
    @Column(length = 64)
    private String driverName;
    @Column(length = 32)
    private String driverPhone;
    /** 跨县接运许可编号 */
    @Column(length = 64)
    private String permitNo;
    /** 预计到馆时间 */
    private LocalDateTime estimatedArrivalAt;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "HELD";
    }
}
