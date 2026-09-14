package com.funeral.repo;

import com.funeral.entity.FuneralOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuneralOrderRepository extends JpaRepository<FuneralOrder, Long> {
    List<FuneralOrder> findAllByOrderByCreatedAtDesc();
    boolean existsByOrderNo(String orderNo);
}
