package com.funeral.repo;

import com.funeral.entity.CrossRegionTransport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrossRegionTransportRepository extends JpaRepository<CrossRegionTransport, Long> {
    Optional<CrossRegionTransport> findByOrderId(Long orderId);
}
