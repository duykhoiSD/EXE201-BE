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
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final TicketRepository ticketRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public List<Task> getAllTasks(String status, String priority, Long assignedTo, Long contactId, Long dealId, Long ticketId) {
        User currentUser = getCurrentAuthenticatedUser();

        List<Task> tasks;
        // RBAC: Agent can only view tasks assigned to them or created by them
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            log.info("RBAC: Scoping tasks strictly for agent: {} (id: {})", currentUser.getEmail(), currentUser.getId());
            tasks = taskRepository.findByAgentId(currentUser.getId());
        } else {
            tasks = taskRepository.findAll();
        }

        return tasks.stream()
                .filter(t -> status == null || status.isBlank() || status.equalsIgnoreCase(t.getStatus()))
                .filter(t -> priority == null || priority.isBlank() || priority.equalsIgnoreCase(t.getPriority()))
                .filter(t -> assignedTo == null || (t.getAssignedTo() != null && t.getAssignedTo().getId().equals(assignedTo)))
                .filter(t -> contactId == null || (t.getContact() != null && t.getContact().getId().equals(contactId)))
                .filter(t -> dealId == null || (t.getDeal() != null && t.getDeal().getId().equals(dealId)))
                .filter(t -> ticketId == null || (t.getTicket() != null && t.getTicket().getId().equals(ticketId)))
                .toList();
    }

    public Task getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            boolean isOwner = (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUser.getId()))
                    || (task.getCreatedBy() != null && task.getCreatedBy().getId().equals(currentUser.getId()));
            if (!isOwner) {
                throw new AccessDeniedException("Bạn chỉ có quyền xem công việc do mình phụ trách!");
            }
        }
        return task;
    }

    public Task createTask(Task task) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null) {
            task.setCreatedBy(currentUser);
            if (currentUser.getRole() == User.Role.AGENT) {
                // Agent can only assign task to themselves
                task.setAssignedTo(currentUser);
            }
        }

        // SOP 4: Default due date = +3 business days if not set
        if (task.getDueDate() == null) {
            task.setDueDate(LocalDate.now().plusDays(3));
        }

        // Relational RBAC: Validate Ticket linkage
        if (task.getTicket() != null && task.getTicket().getId() != null) {
            Ticket ticket = ticketRepository.findById(task.getTicket().getId()).orElse(null);
            if (ticket != null) {
                if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
                    boolean isOwner = (ticket.getTicketOwner() != null && ticket.getTicketOwner().getId().equals(currentUser.getId()))
                            || (ticket.getServiceAgent() != null && ticket.getServiceAgent().getId().equals(currentUser.getId()));
                    if (!isOwner) {
                        throw new AccessDeniedException("Bạn không thể tạo task gắn với Ticket của người khác!");
                    }
                }
                task.setTicket(ticket);
            }
        }

        // Relational RBAC: Validate Contact linkage
        if (task.getContact() != null && task.getContact().getId() != null) {
            Contact contact = contactRepository.findById(task.getContact().getId()).orElse(null);
            if (contact != null) {
                if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
                    boolean isOwner = contact.getContactOwner() != null && contact.getContactOwner().getId().equals(currentUser.getId());
                    if (!isOwner) {
                        throw new AccessDeniedException("Bạn không thể tạo task gắn với Contact của người khác!");
                    }
                }
                task.setContact(contact);
            }
        }

        // Relational RBAC: Validate Deal linkage
        if (task.getDeal() != null && task.getDeal().getId() != null) {
            Deal deal = dealRepository.findById(task.getDeal().getId()).orElse(null);
            if (deal != null) {
                if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
                    boolean isOwner = deal.getDealOwner() != null && deal.getDealOwner().getId().equals(currentUser.getId());
                    if (!isOwner) {
                        throw new AccessDeniedException("Bạn không thể tạo task gắn với Deal của người khác!");
                    }
                }
                task.setDeal(deal);
            }
        }

        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task taskData) {
        Task existing = getTaskById(id);
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Check edit permission & Reassignment rights
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            boolean isOwner = (existing.getAssignedTo() != null && existing.getAssignedTo().getId().equals(currentUser.getId()))
                    || (existing.getCreatedBy() != null && existing.getCreatedBy().getId().equals(currentUser.getId()));
            if (!isOwner) {
                throw new AccessDeniedException("Bạn chỉ có quyền cập nhật công việc do mình phụ trách!");
            }
            // Agent CANNOT reassign assignedTo
        } else {
            // ADMIN / STAFF can reassign assignedTo
            if (taskData.getAssignedTo() != null) {
                existing.setAssignedTo(taskData.getAssignedTo());
            }
        }

        if (taskData.getTitle() != null) existing.setTitle(taskData.getTitle());
        if (taskData.getDescription() != null) existing.setDescription(taskData.getDescription());
        if (taskData.getPriority() != null) existing.setPriority(taskData.getPriority());
        if (taskData.getStatus() != null) existing.setStatus(taskData.getStatus());
        if (taskData.getTaskType() != null) existing.setTaskType(taskData.getTaskType());
        if (taskData.getDueDate() != null) existing.setDueDate(taskData.getDueDate());
        return taskRepository.save(existing);
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Agent can only delete tasks created by themselves
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (task.getCreatedBy() == null || !task.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Đại lý chỉ có thể xóa các công việc do chính mình tạo ra!");
            }
        }
        taskRepository.delete(task);
    }

    public long count() {
        return taskRepository.count();
    }
}
