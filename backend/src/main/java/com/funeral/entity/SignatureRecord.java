package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 家属签字记录：每次确认或删减项目都保留签名（PNG dataURL 存库）。
 * actionType：CONFIRM_ITEM 确认项目 / REMOVE_ITEM 删减项目 /
 *             CONFIRM_PLAN 确认整体方案 / CONFIRM_BILL 确认费用明细 /
 *             CONFIRM_CHANGE 确认临时变更 / URN_CLAIM 骨灰领取 / FEEDBACK 服务反馈
 */
@Entity
@Table(name = "signature_record", indexes = {
        @Index(name = "idx_sign_order", columnList = "orderId")
})
@Getter
@Setter
public class SignatureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    private Long orderItemId;

    @Column(nullable = false, length = 32)
    private String actionType;

    @Column(length = 1000)
    private String contentSummary;

    /** 家属签名人姓名（与逝者关系） */
    @Column(nullable = false, length = 64)
    private String signerName;

    @Column(length = 32)
    private String signerPhone;

    @Column(length = 32)
    private String signerRelation;

    @Column(nullable = false, columnDefinition = "text")
    private String signatureData;

    /** 在场经办人 */
    @Column(length = 64)
    private String witnessName;
    private Long witnessId;

    @Column(nullable = false)
    private LocalDateTime signedAt;

    @PrePersist
    void prePersist() {
        if (signedAt == null) signedAt = LocalDateTime.now();
    }
}
