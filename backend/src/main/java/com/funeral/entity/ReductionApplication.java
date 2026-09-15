package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 困难家庭减免申请（两级审核：财务初审 → 馆领导确认）。
 * 支持审核通过后家属补选用品时重新提交（每次申请一条记录，以最近一次 APPROVED 为有效减免范围）。
 */
@Entity
@Table(name = "reduction_application", indexes = {
        @Index(name = "idx_redapp_order", columnList = "orderId"),
        @Index(name = "idx_redapp_status", columnList = "status")
})
@Getter
@Setter
public class ReductionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String orderNo;

    /** SUBSISTENCE 低保 / EXTREME_POVERTY 特困供养 / TEMP_RELIEF 临时救助 */
    @Column(nullable = false, length = 24)
    private String assistanceType;

    @Column(nullable = false, length = 64)
    private String applicantName;
    @Column(length = 32)
    private String applicantPhone;

    /** 证明材料清单说明（低保证/特困证/临时救助认定书编号、复印件已收等） */
    @Column(nullable = false, length = 1000)
    private String materialsNote;

    /** 社区（村/居委会）联系人及电话，用于核实困难情况 */
    @Column(length = 64)
    private String communityContactName;
    @Column(length = 32)
    private String communityContactPhone;

    /** 家属申请减免的目录项目 id（逗号分隔） */
    @Column(length = 500)
    private String requestedCatalogIds;

    /**
     * SUBMITTED 待财务初审 / FINANCE_PRE_APPROVED 初审通过待馆领导确认 /
     * APPROVED 终审通过 / REJECTED 审核未通过
     */
    @Column(nullable = false, length = 24)
    private String status = "SUBMITTED";

    // ---------------- 财务初审 ----------------
    private Long financeReviewerId;
    @Column(length = 64)
    private String financeReviewerName;
    @Column(length = 1000)
    private String financeOpinion;
    private LocalDateTime financeReviewedAt;

    // ---------------- 馆领导终审 ----------------
    private Long leaderReviewerId;
    @Column(length = 64)
    private String leaderReviewerName;
    @Column(length = 1000)
    private String leaderOpinion;
    private LocalDateTime leaderReviewedAt;

    /** 终审批准的政府补助目录 id（逗号分隔，实际减免以生成的补助条目为准） */
    @Column(length = 500)
    private String approvedSubsidyIds;

    /** 终审确认的可减免项目目录 id（逗号分隔，用于减免范围留痕，不直接冲减费用） */
    @Column(length = 500)
    private String approvedCatalogIds;

    /** 批准时减免依据 */
    @Column(length = 1000)
    private String approvedBasis;

    /** 批准时家属仍需自费合计（用于补选时差额对比） */
    @Column(precision = 12, scale = 2)
    private BigDecimal approvedSelfPayTotal;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "SUBMITTED";
    }
}
