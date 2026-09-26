package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.Activity;
import com.insurmatch.entity.Contact;
import com.insurmatch.entity.Note;
import com.insurmatch.entity.Task;
import com.insurmatch.repository.ActivityRepository;
import com.insurmatch.repository.NoteRepository;
import com.insurmatch.repository.TaskRepository;
import com.insurmatch.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ContactController — Khớp với FE:
 *   GET    /api/contacts?search=&owner=
 *   GET    /api/contacts/:id
 *   POST   /api/contacts
 *   POST   /api/contacts/:id/notes
 *   POST   /api/contacts/:id/tasks
 *   POST   /api/contacts/:id/activities
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final NoteRepository noteRepository;
    private final TaskRepository taskRepository;
    private final ActivityRepository activityRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Contact>>> getContacts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String owner) {
        List<Contact> contacts = contactService.getAllContacts(search, owner);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Contact>> getContact(@PathVariable Long id) {
        Contact contact = contactService.getContactById(id);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Contact>> createContact(@RequestBody Contact contact) {
        Contact created = contactService.createContact(contact);
        return ResponseEntity.ok(ApiResponse.success("Contact created", created));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<Note>> addNote(@PathVariable Long id, @RequestBody Note note) {
        Contact contact = contactService.getContactById(id);
        note.setContact(contact);
        Note saved = noteRepository.save(note);
        return ResponseEntity.ok(ApiResponse.success("Note added", saved));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<Task>> addTask(@PathVariable Long id, @RequestBody Task task) {
        Contact contact = contactService.getContactById(id);
        task.setContact(contact);
        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(ApiResponse.success("Task added", saved));
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<Activity>> addActivity(@PathVariable Long id, @RequestBody Activity activity) {
        Contact contact = contactService.getContactById(id);
        activity.setContact(contact);
        Activity saved = activityRepository.save(activity);
        return ResponseEntity.ok(ApiResponse.success("Activity added", saved));
    }
}
