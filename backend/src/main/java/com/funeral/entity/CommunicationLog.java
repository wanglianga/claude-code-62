package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 沟通记录：业务员与家属沟通（含亲属情绪激动、现场临时增减仪式等）。
 * 关键约束：先保存沟通记录，再变更礼厅、车辆和物品库存，避免口头承诺争议。
 */
@Entity
@Table(name = "communication_log", indexes = {
        @Index(name = "idx_commlog_order", columnList = "orderId")
})
@Getter
@Setter
public class CommunicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    /** CHAT 普通沟通 / DISPUTE 意见不一致 / EMOTIONAL 情绪激动 / TEMP_CHANGE 现场临时变更 / OTHER */
    @Column(nullable = false, length = 24)
    private String type = "CHAT";

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 500)
    private String participants;

    private Long authorId;
    @Column(nullable = false, length = 64)
    private String authorName;

    /** 是否锁定：锁定后沟通记录不可修改，作为后续变更/结算依据 */
    @Column(nullable = false)
    private Boolean locked = false;

    /** 该沟通记录是否已据此执行资源/物品变更 */
    @Column(nullable = false)
    private Boolean changeApplied = false;

    @Column(length = 1000)
    private String changeNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (locked == null) locked = false;
        if (changeApplied == null) changeApplied = false;
    }
}
