package com.insurmatch.service;

import com.insurmatch.entity.Ticket;
import com.insurmatch.entity.TicketComment;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.TicketCommentRepository;
import com.insurmatch.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;

    public List<Ticket> getAllTickets(String pipeline, String status, String priority, Long contactId, Long dealId) {
        if (contactId != null) return ticketRepository.findByContactId(contactId);
        if (dealId != null) return ticketRepository.findByDealId(dealId);
        if (pipeline != null && !pipeline.isBlank() && status != null && !status.isBlank()) {
            return ticketRepository.findByPipelineAndTicketStatus(pipeline, status);
        }
        if (pipeline != null && !pipeline.isBlank()) return ticketRepository.findByPipeline(pipeline);
        if (status != null && !status.isBlank()) return ticketRepository.findByTicketStatus(status);
        if (priority != null && !priority.isBlank()) return ticketRepository.findByPriority(priority);
        return ticketRepository.findAll();
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public Ticket updateTicket(Long id, Ticket ticketData) {
        Ticket existing = getTicketById(id);
        if (ticketData.getTicketName() != null) existing.setTicketName(ticketData.getTicketName());
        if (ticketData.getPipeline() != null) existing.setPipeline(ticketData.getPipeline());
        if (ticketData.getTicketStatus() != null) existing.setTicketStatus(ticketData.getTicketStatus());
        if (ticketData.getTicketDescription() != null) existing.setTicketDescription(ticketData.getTicketDescription());
        if (ticketData.getTicketResult() != null) existing.setTicketResult(ticketData.getTicketResult());
        if (ticketData.getPriority() != null) existing.setPriority(ticketData.getPriority());
        if (ticketData.getDueDate() != null) existing.setDueDate(ticketData.getDueDate());
        if (ticketData.getChangeDueDateReason() != null) existing.setChangeDueDateReason(ticketData.getChangeDueDateReason());
        return ticketRepository.save(existing);
    }

    public TicketComment addComment(Long ticketId, TicketComment comment) {
        Ticket ticket = getTicketById(ticketId);
        comment.setTicket(ticket);
        return ticketCommentRepository.save(comment);
    }

    public List<TicketComment> getComments(Long ticketId) {
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public long count() {
        return ticketRepository.count();
    }
}
