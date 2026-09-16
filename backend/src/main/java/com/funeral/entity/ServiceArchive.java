package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 服务档案：服务结束后归档。包含费用明细快照、减免依据、火化证明编号、
 * 骨灰领取人、未结项目、家属反馈；后续费用争议/证件补办可回到原始服务过程。
 */
@Entity
@Table(name = "service_archive")
@Getter
@Setter
public class ServiceArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long orderId;

    @Column(unique = true, nullable = false, length = 32)
    private String orderNo;

    // ---------------- 费用归档 ----------------
    @Column(columnDefinition = "text")
    private String feeSnapshot;
    @Column(length = 500)
    private String reductionBasis;

    // ---------------- 火化证明 ----------------
    @Column(length = 64)
    private String cremationCertNo;
    private LocalDateTime cremationTime;
    @Column(length = 64)
    private String furnaceName;

    // ---------------- 骨灰领取 ----------------
    @Column(length = 64)
    private String urnClaimantName;
    @Column(length = 32)
    private String urnClaimantPhone;
    @Column(length = 32)
    private String urnClaimantRelation;
    @Column(length = 64)
    private String urnClaimantIdNo;
    private LocalDateTime urnClaimTime;

    /** 未结项目（逗号分隔或说明） */
    @Column(length = 1000)
    private String unresolvedItems;

    /** 家属反馈 */
    private Integer feedbackRating;
    @Column(length = 1000)
    private String feedbackContent;

    private Long archivedById;
    @Column(length = 64)
    private String archivedByName;
    private LocalDateTime archivedAt;

    @PrePersist
    void prePersist() {
        if (archivedAt == null) archivedAt = LocalDateTime.now();
    }
}
