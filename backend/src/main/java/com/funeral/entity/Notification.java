package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 主要联系人通知（补材料/改期等），异常暂停资源锁定时生成 */
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notify_order", columnList = "orderId")
})
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    /** DOC_MISSING 补材料 / RESCHEDULE 改期 / VEHICLE_DELAY 车辆延误 / COLD_SHORTAGE 冷藏不足 / 通用 */
    @Column(nullable = false, length = 32)
    private String type;

    /** PHONE 电话 / SMS 短信 / ONSITE 现场告知 */
    @Column(nullable = false, length = 16)
    private String channel = "PHONE";

    @Column(nullable = false, length = 64)
    private String targetName;
    @Column(length = 32)
    private String targetPhone;

    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, length = 16)
    private String status = "SENT";

    private Long sentById;
    @Column(length = 64)
    private String sentByName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "SENT";
        if (channel == null) channel = "PHONE";
    }
}
