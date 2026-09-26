package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.Task;
import com.insurmatch.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TaskController — Khớp với FE:
 *   GET    /api/tasks?status=&priority=&assignedTo=&contactId=&dealId=
 *   GET    /api/tasks/:id
 *   POST   /api/tasks
 *   PUT    /api/tasks/:id
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) Long dealId) {
        List<Task> tasks = taskService.getAllTasks(status, priority, assignedTo, contactId, dealId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTask(@PathVariable Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@RequestBody Task task) {
        Task created = taskService.createTask(task);
        return ResponseEntity.ok(ApiResponse.success("Task created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable Long id, @RequestBody Task taskData) {
        Task updated = taskService.updateTask(id, taskData);
        return ResponseEntity.ok(ApiResponse.success("Task updated", updated));
    }
}
