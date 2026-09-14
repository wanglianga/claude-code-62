package com.funeral.repo;

import com.funeral.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {
    List<OrderTimeline> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
