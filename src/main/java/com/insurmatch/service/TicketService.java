package com.insurmatch.service;

import com.insurmatch.entity.*;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public List<Ticket> getAllTickets(String pipeline, String status, String priority, Long contactId, Long dealId) {
        User currentUser = getCurrentAuthenticatedUser();

        List<Ticket> tickets;
        // RBAC: Agent can only see tickets where they are ticketOwner or serviceAgent
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            log.info("RBAC: Scoping tickets strictly for agent: {} (id: {})", currentUser.getEmail(), currentUser.getId());
            tickets = ticketRepository.findByAgentId(currentUser.getId());
        } else {
            tickets = ticketRepository.findAll();
        }

        return tickets.stream()
                .filter(t -> pipeline == null || pipeline.isBlank() || pipeline.equalsIgnoreCase(t.getPipeline()))
                .filter(t -> status == null || status.isBlank() || status.equalsIgnoreCase(t.getTicketStatus()))
                .filter(t -> priority == null || priority.isBlank() || priority.equalsIgnoreCase(t.getPriority()))
                .filter(t -> contactId == null || (t.getContact() != null && t.getContact().getId().equals(contactId)))
                .filter(t -> dealId == null || (t.getDeal() != null && t.getDeal().getId().equals(dealId)))
                .toList();
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

        // SOP 4: Default due date = +3 business days if not set
        if (ticket.getDueDate() == null) {
            ticket.setDueDate(LocalDate.now().plusDays(3));
        }

        // Relational RBAC: Validate Contact linkage
        if (ticket.getContact() != null && ticket.getContact().getId() != null) {
            Contact contact = contactRepository.findById(ticket.getContact().getId()).orElse(null);
            if (contact != null) {
                if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
                    boolean isOwner = contact.getContactOwner() != null && contact.getContactOwner().getId().equals(currentUser.getId());
                    if (!isOwner) {
                        throw new AccessDeniedException("Bạn không thể tạo ticket cho Contact của người khác!");
                    }
                }
                ticket.setContact(contact);
            }
        }

        // Relational RBAC: Validate Deal linkage
        if (ticket.getDeal() != null && ticket.getDeal().getId() != null) {
            Deal deal = dealRepository.findById(ticket.getDeal().getId()).orElse(null);
            if (deal != null) {
                if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
                    boolean isOwner = deal.getDealOwner() != null && deal.getDealOwner().getId().equals(currentUser.getId());
                    if (!isOwner) {
                        throw new AccessDeniedException("Bạn không thể tạo ticket cho Deal của người khác!");
                    }
                }
                ticket.setDeal(deal);
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

        // SOP 4 Rule: Require changeDueDateReason when due date is updated
        if (ticketData.getDueDate() != null && existing.getDueDate() != null 
                && !ticketData.getDueDate().equals(existing.getDueDate())) {
            if (ticketData.getChangeDueDateReason() == null || ticketData.getChangeDueDateReason().isBlank()) {
                throw new IllegalArgumentException("Quy định SOP 4: Bắt buộc phải cung cấp lý do (changeDueDateReason) khi thay đổi hạn chót Due Date!");
            }
        }

        // SOP 4 Rule: Require ticketResult before closing/completing ticket
        if (ticketData.getTicketStatus() != null && 
                (ticketData.getTicketStatus().equalsIgnoreCase("DONE") || ticketData.getTicketStatus().equalsIgnoreCase("Closed"))) {
            String result = ticketData.getTicketResult() != null ? ticketData.getTicketResult() : existing.getTicketResult();
            if (result == null || result.isBlank()) {
                throw new IllegalArgumentException("Quy định SOP 4: Bắt buộc phải nhập kết quả xử lý (ticketResult) trước khi đóng hoặc hoàn tất Ticket!");
            }
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

    public void deleteTicket(Long id) {
        Ticket existing = getTicketById(id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            throw new AccessDeniedException("Đại lý không có quyền xóa Ticket!");
        }
        ticketRepository.delete(existing);
    }

    public long count() {
        return ticketRepository.count();
    }
}
