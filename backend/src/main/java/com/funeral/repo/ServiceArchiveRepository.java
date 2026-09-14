package com.funeral.repo;

import com.funeral.entity.ServiceArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceArchiveRepository extends JpaRepository<ServiceArchive, Long> {
    Optional<ServiceArchive> findByOrderId(Long orderId);
    List<ServiceArchive> findAllByOrderByArchivedAtDesc();
}
