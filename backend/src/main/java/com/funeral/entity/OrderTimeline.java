package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 治丧单全过程时间线（操作留痕，费用争议/证件补办时可回溯原始服务过程） */
@Entity
@Table(name = "order_timeline", indexes = {
        @Index(name = "idx_timeline_order", columnList = "orderId")
})
@Getter
@Setter
public class OrderTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    /** SUBMIT / VERIFY / NEGOTIATE / SIGN / ITEM_CHANGE / RESOURCE_CHANGE /
     *  COLLAB / REDUCTION / SETTLE / URN_CLAIM / FEEDBACK / ARCHIVE / SYSTEM */
    @Column(nullable = false, length = 32)
    private String eventType;

    @Column(nullable = false, length = 500)
    private String content;

    /** FAMILY / TRANSPORT / CLERK / FINANCE / HALL_ADMIN / LEADER / SYSTEM */
    @Column(length = 16)
    private String actorRole;

    private Long actorId;
    @Column(length = 64)
    private String actorName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
