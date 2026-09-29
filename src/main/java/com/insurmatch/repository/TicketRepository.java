package com.insurmatch.repository;

import com.insurmatch.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByPipeline(String pipeline);
    List<Ticket> findByTicketStatus(String ticketStatus);
    List<Ticket> findByPriority(String priority);
    List<Ticket> findByContactId(Long contactId);
    List<Ticket> findByDealId(Long dealId);
    List<Ticket> findByPipelineAndTicketStatus(String pipeline, String ticketStatus);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.ticketOwner.id = :agentId OR t.serviceAgent.id = :agentId")
    List<Ticket> findByAgentId(@org.springframework.data.repository.query.Param("agentId") Long agentId);
}
