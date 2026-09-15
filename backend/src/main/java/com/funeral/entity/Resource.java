package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 馆内资源主数据：VEHICLE 接运车辆 / COLD 冷藏位 / HALL 告别厅 / FURNACE 火化设备。
 * hallSpec：告别厅规格 SMALL/MEDIUM/LARGE/GRAND；available=false 用于车辆/火化设备检修停用。
 */
@Entity
@Table(name = "resource", indexes = {
        @Index(name = "idx_resource_type", columnList = "type")
})
@Getter
@Setter
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 16)
    private String hallSpec;

    /** 车辆资质是否有效（跨区域接运核验使用，仅车辆使用） */
    @Column(nullable = false)
    private Boolean qualified = true;

    /** 跨县接运许可编号 / 车辆营运证号 */
    @Column(length = 64)
    private String permitNo;

    /** 容量（冷藏位=1，告别厅=容纳人数），用于馆内接收能力核验 */
    private Integer capacity;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(length = 500)
    private String note;

    /** 检修/停用至何时（火化设备检修等） */
    private LocalDateTime unavailableUntil;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (available == null) available = true;
    }
}
