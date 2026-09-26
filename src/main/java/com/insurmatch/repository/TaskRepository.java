package com.insurmatch.repository;

import com.insurmatch.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(String status);
    List<Task> findByPriority(String priority);
    List<Task> findByAssignedToId(Long assignedToId);
    List<Task> findByContactId(Long contactId);
    List<Task> findByDealId(Long dealId);
}
