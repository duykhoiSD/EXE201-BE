package com.insurmatch.service;

import com.insurmatch.entity.Ticket;
import com.insurmatch.entity.TicketComment;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.TicketCommentRepository;
import com.insurmatch.repository.TicketRepository;
import com.insurmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public List<Ticket> getAllTickets(String pipeline, String status, String priority, Long contactId, Long dealId) {
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Agent can only see tickets where they are ticketOwner or serviceAgent
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            log.info("RBAC: Scoping tickets strictly for agent: {} (id: {})", currentUser.getEmail(), currentUser.getId());
            return ticketRepository.findByAgentId(currentUser.getId());
        }

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
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            boolean isOwner = (ticket.getTicketOwner() != null && ticket.getTicketOwner().getId().equals(currentUser.getId()))
                    || (ticket.getServiceAgent() != null && ticket.getServiceAgent().getId().equals(currentUser.getId()));
            if (!isOwner) {
                throw new AccessDeniedException("Bạn chỉ có quyền xem ticket do mình phụ trách!");
            }
        }
        return ticket;
    }

    public Ticket createTicket(Ticket ticket) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            ticket.setTicketOwner(currentUser);
            if (ticket.getServiceAgent() == null) {
                ticket.setServiceAgent(currentUser);
            }
        }
        return ticketRepository.save(ticket);
    }

    public Ticket updateTicket(Long id, Ticket ticketData) {
        Ticket existing = getTicketById(id);
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Check edit permission & Reassignment rights
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            boolean isOwner = (existing.getTicketOwner() != null && existing.getTicketOwner().getId().equals(currentUser.getId()))
                    || (existing.getServiceAgent() != null && existing.getServiceAgent().getId().equals(currentUser.getId()));
            if (!isOwner) {
                throw new AccessDeniedException("Bạn chỉ có quyền cập nhật ticket do mình phụ trách!");
            }
            // Agent CANNOT reassign ticketOwner or serviceAgent
        } else {
            // ADMIN / STAFF can reassign ticketOwner and serviceAgent
            if (ticketData.getTicketOwner() != null) existing.setTicketOwner(ticketData.getTicketOwner());
            if (ticketData.getServiceAgent() != null) existing.setServiceAgent(ticketData.getServiceAgent());
        }

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
        if (comment.getAuthorName() == null || comment.getAuthorName().isBlank()) {
            User currentUser = getCurrentAuthenticatedUser();
            String name = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "System";
            comment.setAuthorName(name);
        }
        return ticketCommentRepository.save(comment);
    }

    public List<TicketComment> getComments(Long ticketId) {
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public long count() {
        return ticketRepository.count();
    }
}
