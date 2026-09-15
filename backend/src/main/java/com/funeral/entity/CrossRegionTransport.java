package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 外地/跨县接运登记与核验。围绕同一治丧单处理：
 * 死亡地、当地医院/公安/殡仪馆联系人、死亡证明出具机构、接运许可、
 * 车辆资质与司机、预计到达、防腐冷藏需求、宗教习俗、随行亲属；
 * 核验死亡证明/接运许可/车辆资质/冷藏条件/馆内接收能力，并关联火化排期与礼厅。
 * 异常（证明缺失、车辆延误、冷藏位不足、排期冲突）暂停资源锁定并通知主要联系人改期补料。
 * 到达后回写车辆交接、冷藏入库、证明复核、火化排期。
 */
@Entity
@Table(name = "cross_region_transport")
@Getter
@Setter
public class CrossRegionTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String orderNo;

    // ---------------- 异地信息登记 ----------------
    /** 死亡地（外地医院/地点） */
    @Column(nullable = false, length = 255)
    private String deathPlace;
    /** 跨县接运地点 */
    @Column(length = 255)
    private String pickupAddress;
    /** 当地对接机构（医院/公安/殡仪馆） */
    @Column(length = 128)
    private String localOrgName;
    @Column(length = 64)
    private String localContactName;
    @Column(length = 32)
    private String localContactPhone;
    /** 死亡证明出具机构 */
    @Column(length = 128)
    private String certIssuingOrg;

    /** 接运许可编号与核验状态 PENDING/VERIFIED/MISSING */
    @Column(length = 64)
    private String transportPermitNo;
    @Column(length = 16)
    private String permitStatus = "PENDING";

    // ---------------- 车辆 / 司机 / 到达 ----------------
    private Long vehicleId;
    @Column(length = 128)
    private String vehicleName;
    @Column(length = 64)
    private String driverName;
    @Column(length = 32)
    private String driverPhone;
    private LocalDateTime estimatedArrivalAt;
    private LocalDateTime departedAt;

    // ---------------- 冷藏 / 习俗 / 随行 ----------------
    @Column(nullable = false)
    private Boolean embalmingRequired = false;
    @Column(length = 500)
    private String coldConditionNote;
    @Column(length = 500)
    private String religiousCustom;
    @Column(length = 500)
    private String accompanyingRelatives;

    // ---------------- 五项核验 + 排期 ----------------
    @Column(nullable = false)
    private Boolean certVerified = false;
    @Column(nullable = false)
    private Boolean permitVerified = false;
    @Column(nullable = false)
    private Boolean vehicleVerified = false;
    @Column(nullable = false)
    private Boolean coldConditionVerified = false;
    @Column(nullable = false)
    private Boolean receptionCapacityVerified = false;
    @Column(nullable = false)
    private Boolean scheduleVerified = false;

    /** PLANNED 已登记 / IN_TRANSIT 在途 / ARRIVED 已到馆 / SUSPENDED 异常暂停 */
    @Column(nullable = false, length = 16)
    private String status = "PLANNED";

    @Column(length = 1000)
    private String suspendReason;

    private Long coldBookingId;
    private Long hallBookingId;
    private Long furnaceBookingId;

    // ---------------- 到达回写 ----------------
    private LocalDateTime arrivedAt;
    /** 车辆交接情况 */
    @Column(length = 1000)
    private String handoverNote;
    private LocalDateTime coldStoredAt;
    @Column(length = 500)
    private String coldStorageNote;
    private LocalDateTime certReverifiedAt;
    @Column(length = 500)
    private String certReverifyNote;
    private LocalDateTime scheduleConfirmedAt;
    @Column(length = 500)
    private String scheduleNote;
    @Column(length = 64)
    private String receiverName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (embalmingRequired == null) embalmingRequired = false;
        if (certVerified == null) certVerified = false;
        if (permitVerified == null) permitVerified = false;
        if (vehicleVerified == null) vehicleVerified = false;
        if (coldConditionVerified == null) coldConditionVerified = false;
        if (receptionCapacityVerified == null) receptionCapacityVerified = false;
        if (scheduleVerified == null) scheduleVerified = false;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
