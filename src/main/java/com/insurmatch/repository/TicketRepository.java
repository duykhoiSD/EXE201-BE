package com.insurmatch.repository;

import com.insurmatch.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByCode(String code);
    List<Ticket> findByPipeline(String pipeline);
    List<Ticket> findByTicketStatus(String ticketStatus);
    List<Ticket> findByPriority(String priority);
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.contact.id = :contactId")
    List<Ticket> findByContactId(@org.springframework.data.repository.query.Param("contactId") Long contactId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.deal.id = :dealId")
    List<Ticket> findByDealId(@org.springframework.data.repository.query.Param("dealId") Long dealId);

    List<Ticket> findByPipelineAndTicketStatus(String pipeline, String ticketStatus);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.ticketOwner.id = :agentId OR t.serviceAgent.id = :agentId")
    List<Ticket> findByAgentId(@org.springframework.data.repository.query.Param("agentId") Long agentId);
}
