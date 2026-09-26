package com.insurmatch.service;

import com.insurmatch.entity.Task;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<Task> getAllTasks(String status, String priority, Long assignedTo, Long contactId, Long dealId) {
        if (contactId != null) return taskRepository.findByContactId(contactId);
        if (dealId != null) return taskRepository.findByDealId(dealId);
        if (assignedTo != null) return taskRepository.findByAssignedToId(assignedTo);
        if (status != null && !status.isBlank()) return taskRepository.findByStatus(status);
        if (priority != null && !priority.isBlank()) return taskRepository.findByPriority(priority);
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    public Task createTask(Task task) {
        // SOP 4: Default due date = +3 days if not set
        if (task.getDueDate() == null) {
            task.setDueDate(LocalDate.now().plusDays(3));
        }
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task taskData) {
        Task existing = getTaskById(id);
        if (taskData.getTitle() != null) existing.setTitle(taskData.getTitle());
        if (taskData.getDescription() != null) existing.setDescription(taskData.getDescription());
        if (taskData.getPriority() != null) existing.setPriority(taskData.getPriority());
        if (taskData.getStatus() != null) existing.setStatus(taskData.getStatus());
        if (taskData.getDueDate() != null) existing.setDueDate(taskData.getDueDate());
        return taskRepository.save(existing);
    }

    public long count() {
        return taskRepository.count();
    }
}
