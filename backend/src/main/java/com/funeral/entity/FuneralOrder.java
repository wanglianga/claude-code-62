package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 治丧单（预约 + 治丧全过程主单）。围绕同一单据：核验证件/车辆/冷藏位/火化排期/礼厅、
 * 方案沟通、家属签字确认、费用结算、减免审核、异常协同、服务归档。
 */
@Entity
@Table(name = "funeral_order")
@Getter
@Setter
public class FuneralOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String orderNo;

    // ---------------- 逝者信息 ----------------
    @Column(nullable = false, length = 64)
    private String deceasedName;
    private String gender;
    private Integer age;
    @Column(length = 32)
    private String idCardNo;
    private LocalDateTime deathTime;
    @Column(length = 255)
    private String deathCause;
    /** 是否外地来此（异地逝者） */
    private Boolean fromOtherCity = false;

    /** 死亡证明核验状态：PENDING 待核验 / VERIFIED 已核验 / MISSING 材料缺失 */
    @Column(length = 16)
    private String certificateStatus = "PENDING";
    @Column(length = 64)
    private String certificateNo;
    @Column(length = 500)
    private String certificateNote;

    // ---------------- 接运 / 习俗 / 冷藏 ----------------
    @Column(length = 255)
    private String pickupAddress;
    private LocalDateTime pickupTime;
    @Column(length = 255)
    private String religiousCustom;
    private Boolean needRefrigeration = false;

    /** 期望告别厅规格：SMALL / MEDIUM / LARGE / GRAND */
    @Column(length = 16)
    private String hallSpec;

    /** 预约告别时间（核验礼厅/火化排期使用） */
    private LocalDateTime farewellTime;

    // ---------------- 亲属联系人 ----------------
    @Column(length = 64)
    private String contactName;
    @Column(length = 32)
    private String contactPhone;
    @Column(length = 32)
    private String contactRelation;

    // ---------------- 流程状态 ----------------
    /**
     * DRAFT 待提交 / RESOURCE_VERIFYING 资源核验中 / VERIFIED 核验通过 /
     * NEGOTIATING 方案沟通中 / CONFIRMED 方案已确认 / IN_SERVICE 服务进行中 /
     * COMPLETED 服务完成待结算 / SETTLED 已结算 / ARCHIVED 已归档 / CANCELLED 已取消
     */
    @Column(nullable = false, length = 24)
    private String status = "DRAFT";

    @Column(length = 16)
    private String verifyStatus = "PENDING";   // PENDING/PASS/FAIL
    @Column(length = 1000)
    private String verifyNote;

    private Long createdById;
    @Column(length = 64)
    private String createdByName;

    private Long clerkId;
    @Column(length = 64)
    private String clerkName;

    // ---------------- 金额 / 减免 ----------------
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal reductionAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payableAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    /** UNPAID / PARTIAL / PAID */
    @Column(length = 16)
    private String paymentStatus = "UNPAID";
    /** NONE / PENDING / APPROVED / REJECTED */
    @Column(length = 16)
    private String reductionStatus = "NONE";

    private Boolean familyConfirmed = false;
    private LocalDateTime settleTime;
    private Long settledById;
    @Column(length = 64)
    private String settledByName;

    // ---------------- 归档/反馈 ----------------
    private Integer feedbackRating;
    @Column(length = 1000)
    private String feedbackContent;
    private LocalDateTime feedbackTime;
    /** 未结项目说明 */
    @Column(length = 1000)
    private String unresolvedNote;

    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (fromOtherCity == null) fromOtherCity = false;
        if (needRefrigeration == null) needRefrigeration = false;
        if (familyConfirmed == null) familyConfirmed = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
