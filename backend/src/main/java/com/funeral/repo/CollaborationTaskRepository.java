package com.funeral.repo;

import com.funeral.entity.CollaborationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollaborationTaskRepository extends JpaRepository<CollaborationTask, Long> {
    List<CollaborationTask> findByOrderIdOrderByPriorityAscCreatedAtDesc(Long orderId);
    List<CollaborationTask> findByStatusNotOrderByPriorityAscCreatedAtDesc(String status);
    List<CollaborationTask> findByAssigneeRoleAndStatusNotOrderByPriorityAscCreatedAtDesc(String role, String status);
}
