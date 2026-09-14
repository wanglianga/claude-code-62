package com.funeral.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 系统用户：家属 / 接运组 / 业务员 / 财务 / 礼厅管理员 / 馆领导 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 32)
    private String displayName;

    /** FAMILY 家属 / TRANSPORT 接运组 / CLERK 业务员 / FINANCE 财务 / HALL_ADMIN 礼厅管理员 / LEADER 馆领导 */
    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false)
    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = true;
    }
}
