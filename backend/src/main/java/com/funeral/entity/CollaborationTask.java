package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 协同待办：围绕同一治丧单跨岗位处理异常。
 * type：DOC_MISSING 证明材料缺失 / FAMILY_DISAGREE 亲属意见不一致 / REDUCTION_REVIEW 低保减免待审核 /
 *       HALL_CONFLICT 告别厅临时冲突 / FURNACE_MAINT 火化设备检修 / NONLOCAL 外地逝者 / GENERAL 其他
 * assigneeRole：TRANSPORT 接运组 / CLERK 业务员 / FINANCE 财务 / HALL_ADMIN 礼厅管理员 / LEADER 馆领导
 * status：OPEN 待处理 / PROCESSING 处理中 / RESOLVED 已解决
 */
@Entity
@Table(name = "collaboration_task", indexes = {
        @Index(name = "idx_collab_order", columnList = "orderId"),
        @Index(name = "idx_collab_role", columnList = "assigneeRole,status")
})
@Getter
@Setter
public class CollaborationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 24)
    private String assigneeRole;

    private Long assigneeId;
    @Column(length = 64)
    private String assigneeName;

    @Column(nullable = false, length = 16)
    private String status = "OPEN";

    /** 优先级：1 高 / 2 中 / 3 低 */
    @Column(nullable = false)
    private Integer priority = 2;

    @Column(length = 1000)
    private String resolution;

    private Long createdById;
    @Column(nullable = false, length = 64)
    private String createdByName;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "OPEN";
        if (priority == null) priority = 2;
    }
}
