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
    List<Task> findByTicketId(Long ticketId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.assignedTo.id = :agentId OR (t.createdBy IS NOT NULL AND t.createdBy.id = :agentId)")
    List<Task> findByAgentId(@org.springframework.data.repository.query.Param("agentId") Long agentId);
}
