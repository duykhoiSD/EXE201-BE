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

    public List<Ticket> getAllTickets(String pipeline, String status, String priority, Long contactId, Long dealId, String search, String owner) {
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
                .filter(t -> pipeline == null || pipeline.isBlank() || pipeline.equalsIgnoreCase("all") || pipeline.equalsIgnoreCase(t.getPipeline()))
                .filter(t -> status == null || status.isBlank() || status.equalsIgnoreCase("all") || status.equalsIgnoreCase(t.getTicketStatus()))
                .filter(t -> priority == null || priority.isBlank() || priority.equalsIgnoreCase("all") || priority.equalsIgnoreCase(t.getPriority()))
                .filter(t -> contactId == null || (t.getContact() != null && t.getContact().getId().equals(contactId)))
                .filter(t -> dealId == null || (t.getDeal() != null && t.getDeal().getId().equals(dealId)))
                .filter(t -> {
                    if (search == null || search.isBlank()) return true;
                    String q = search.toLowerCase().trim();
                    return (t.getTicketName() != null && t.getTicketName().toLowerCase().contains(q))
                            || (t.getCode() != null && t.getCode().toLowerCase().contains(q))
                            || (t.getTicketDescription() != null && t.getTicketDescription().toLowerCase().contains(q))
                            || (t.getContact() != null && t.getContact().getFullName() != null && t.getContact().getFullName().toLowerCase().contains(q));
                })
                .filter(t -> {
                    if (owner == null || owner.isBlank() || owner.equalsIgnoreCase("all")) return true;
                    String o = owner.toLowerCase().trim();
                    return (t.getTicketOwner() != null && t.getTicketOwner().getFullName() != null && t.getTicketOwner().getFullName().toLowerCase().contains(o))
                            || (t.getServiceAgent() != null && t.getServiceAgent().getFullName() != null && t.getServiceAgent().getFullName().toLowerCase().contains(o));
                })
                .toList();
    }

    public List<Ticket> getAllTickets(String pipeline, String status, String priority, Long contactId, Long dealId) {
        return getAllTickets(pipeline, status, priority, contactId, dealId, null, null);
    }

    public Ticket getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        validateAgentAccess(ticket);
        return ticket;
    }

    public Ticket getTicketByIdOrCode(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Ticket identifier cannot be empty");
        }
        java.util.Optional<Ticket> byCode = ticketRepository.findByCode(identifier);
        if (byCode.isPresent()) {
            Ticket t = byCode.get();
            validateAgentAccess(t);
            return t;
        }
        try {
            Long id = Long.parseLong(identifier);
            return getTicketById(id);
        } catch (NumberFormatException ignored) {}

        String digits = identifier.replaceAll("^[^0-9]+", "");
        if (digits.startsWith("2600") && digits.length() > 4) {
            digits = digits.substring(4);
        }
        try {
            long num = Long.parseLong(digits);
            if (num > 1000) {
                try {
                    return getTicketById(num - 1000);
                } catch (Exception ignored) {}
            }
            return getTicketById(num);
        } catch (NumberFormatException | ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Ticket not found with identifier: " + identifier);
        }
    }

    private void validateAgentAccess(Ticket ticket) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            boolean isOwner = (ticket.getTicketOwner() != null && ticket.getTicketOwner().getId().equals(currentUser.getId()))
                    || (ticket.getServiceAgent() != null && ticket.getServiceAgent().getId().equals(currentUser.getId()));
            if (!isOwner) {
                throw new AccessDeniedException("Bạn chỉ có quyền xem ticket do mình phụ trách!");
            }
        }
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

    public Ticket updateTicket(String identifier, Ticket ticketData) {
        return updateTicket(getTicketByIdOrCode(identifier).getId(), ticketData);
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

    public TicketComment addComment(String identifier, TicketComment comment) {
        return addComment(getTicketByIdOrCode(identifier).getId(), comment);
    }

    public List<TicketComment> getComments(Long ticketId) {
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public List<TicketComment> getComments(String identifier) {
        return getComments(getTicketByIdOrCode(identifier).getId());
    }

    public void deleteTicket(Long id) {
        Ticket existing = getTicketById(id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            throw new AccessDeniedException("Đại lý không có quyền xóa Ticket!");
        }
        ticketRepository.delete(existing);
    }

    public void deleteTicket(String identifier) {
        deleteTicket(getTicketByIdOrCode(identifier).getId());
    }

    public long count() {
        return ticketRepository.count();
    }
}
