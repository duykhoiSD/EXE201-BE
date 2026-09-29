package com.insurmatch.service;

import com.insurmatch.entity.Task;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.TaskRepository;
import com.insurmatch.repository.UserRepository;
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

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    public List<Task> getAllTasks(String status, String priority, Long assignedTo, Long contactId, Long dealId, Long ticketId) {
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Agent can only view tasks assigned to them or created by them
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            log.info("RBAC: Scoping tasks strictly for agent: {} (id: {})", currentUser.getEmail(), currentUser.getId());
            return taskRepository.findByAgentId(currentUser.getId());
        }

        if (contactId != null) return taskRepository.findByContactId(contactId);
        if (dealId != null) return taskRepository.findByDealId(dealId);
        if (ticketId != null) return taskRepository.findByTicketId(ticketId);
        if (assignedTo != null) return taskRepository.findByAssignedToId(assignedTo);
        if (status != null && !status.isBlank()) return taskRepository.findByStatus(status);
        if (priority != null && !priority.isBlank()) return taskRepository.findByPriority(priority);
        return taskRepository.findAll();
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
            if (currentUser.getRole() == User.Role.AGENT && task.getAssignedTo() == null) {
                task.setAssignedTo(currentUser);
            }
        }

        // SOP 4: Default due date = +3 days if not set
        if (task.getDueDate() == null) {
            task.setDueDate(LocalDate.now().plusDays(3));
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
        Task task = getTaskById(id); // RBAC validated
        taskRepository.delete(task);
    }

    public long count() {
        return taskRepository.count();
    }
}
