package com.funeral.repo;

import com.funeral.entity.CommunicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {
    List<CommunicationLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
