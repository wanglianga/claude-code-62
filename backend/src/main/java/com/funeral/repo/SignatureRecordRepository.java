package com.funeral.repo;

import com.funeral.entity.SignatureRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignatureRecordRepository extends JpaRepository<SignatureRecord, Long> {
    List<SignatureRecord> findByOrderIdOrderBySignedAtDesc(Long orderId);
}
