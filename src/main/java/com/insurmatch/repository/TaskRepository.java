package com.insurmatch.repository;

import com.insurmatch.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByCode(String code);
    List<Task> findByStatus(String status);
    List<Task> findByPriority(String priority);
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.assignedTo.id = :assignedToId")
    List<Task> findByAssignedToId(@org.springframework.data.repository.query.Param("assignedToId") Long assignedToId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.contact.id = :contactId")
    List<Task> findByContactId(@org.springframework.data.repository.query.Param("contactId") Long contactId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.deal.id = :dealId")
    List<Task> findByDealId(@org.springframework.data.repository.query.Param("dealId") Long dealId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.ticket.id = :ticketId")
    List<Task> findByTicketId(@org.springframework.data.repository.query.Param("ticketId") Long ticketId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.assignedTo.id = :agentId OR (t.createdBy IS NOT NULL AND t.createdBy.id = :agentId)")
    List<Task> findByAgentId(@org.springframework.data.repository.query.Param("agentId") Long agentId);
}
