package com.funeral.repo;

import com.funeral.entity.ReductionApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReductionApplicationRepository extends JpaRepository<ReductionApplication, Long> {
    List<ReductionApplication> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<ReductionApplication> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, String status);
}
