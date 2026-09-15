package com.funeral.repo;

import com.funeral.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
