package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.dto.ContactDTO;
import com.insurmatch.dto.ContactDetailDTO;
import com.insurmatch.entity.Activity;
import com.insurmatch.entity.Contact;
import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.Deal;
import com.insurmatch.entity.Note;
import com.insurmatch.entity.Task;
import com.insurmatch.entity.Ticket;
import com.insurmatch.repository.ActivityRepository;
import com.insurmatch.repository.NoteRepository;
import com.insurmatch.repository.TaskRepository;
import com.insurmatch.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ContactController — CRUD hoàn chỉnh + sub-resources cho module Contact.
 *
 *   GET    /api/contacts?search=&owner=         — Danh sách contacts (search + filter)
 *   GET    /api/contacts/:id                    — Chi tiết contact (entity gốc)
 *   GET    /api/contacts/:id/detail             — Chi tiết contact 360° (contact + notes + tasks + activities + deals + tickets + documents)
 *   POST   /api/contacts                        — Tạo mới contact
 *   PUT    /api/contacts/:id                    — Cập nhật contact
 *   DELETE /api/contacts/:id                    — Xóa contact
 *   GET    /api/contacts/:id/notes              — Lấy notes của contact
 *   POST   /api/contacts/:id/notes              — Thêm note cho contact
 *   GET    /api/contacts/:id/tasks              — Lấy tasks của contact
 *   POST   /api/contacts/:id/tasks              — Thêm task cho contact
 *   GET    /api/contacts/:id/activities          — Lấy activities của contact
 *   POST   /api/contacts/:id/activities          — Thêm activity cho contact
 *   GET    /api/contacts/:id/deals              — Lấy deals của contact
 *   GET    /api/contacts/:id/tickets            — Lấy tickets của contact
 *   GET    /api/contacts/:id/documents          — Lấy documents của contact
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final NoteRepository noteRepository;
    private final TaskRepository taskRepository;
    private final ActivityRepository activityRepository;

    // ===================== LIST =====================

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactDTO>>> getContacts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String owner) {
        List<Contact> contacts = contactService.getAllContacts(search, owner);
        List<ContactDTO> dtos = contactService.toDTOList(contacts);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    // ===================== GET BY ID / CODE =====================

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactDTO>> getContact(@PathVariable String id) {
        Contact contact = contactService.getContactByIdOrCode(id);
        ContactDTO dto = contactService.toDTO(contact);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // ===================== GET DETAIL 360° =====================

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<ContactDetailDTO>> getContactDetail(@PathVariable String id) {
        Contact contact = contactService.getContactByIdOrCode(id);
        ContactDetailDTO detail = contactService.getContactDetail(contact.getId());
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    // ===================== CREATE =====================

    @PostMapping
    public ResponseEntity<ApiResponse<ContactDTO>> createContact(@RequestBody ContactDTO contactDTO) {
        Contact created = contactService.createContactFromDTO(contactDTO);
        ContactDTO dto = contactService.toDTO(created);
        return ResponseEntity.ok(ApiResponse.success("Contact created", dto));
    }

    // ===================== UPDATE =====================

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactDTO>> updateContact(
            @PathVariable Long id,
            @RequestBody ContactDTO contactDTO) {
        Contact updated = contactService.updateContactFromDTO(id, contactDTO);
        ContactDTO dto = contactService.toDTO(updated);
        return ResponseEntity.ok(ApiResponse.success("Contact updated", dto));
    }

    // ===================== DELETE =====================

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok(ApiResponse.success("Contact deleted", null));
    }

    // ===================== SUB-RESOURCES: NOTES =====================

    @GetMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<List<Note>>> getContactNotes(@PathVariable Long id) {
        List<Note> notes = contactService.getNotesByContactId(id);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<Note>> addNote(@PathVariable Long id, @RequestBody Note note) {
        Contact contact = contactService.getContactById(id);
        note.setContact(contact);
        Note saved = noteRepository.save(note);
        return ResponseEntity.ok(ApiResponse.success("Note added", saved));
    }

    // ===================== SUB-RESOURCES: TASKS =====================

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<Task>>> getContactTasks(@PathVariable Long id) {
        List<Task> tasks = contactService.getTasksByContactId(id);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<Task>> addTask(@PathVariable Long id, @RequestBody Task task) {
        Contact contact = contactService.getContactById(id);
        task.setContact(contact);
        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(ApiResponse.success("Task added", saved));
    }

    // ===================== SUB-RESOURCES: ACTIVITIES =====================

    @GetMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<List<Activity>>> getContactActivities(@PathVariable Long id) {
        List<Activity> activities = contactService.getActivitiesByContactId(id);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ApiResponse<Activity>> addActivity(@PathVariable Long id, @RequestBody Activity activity) {
        Contact contact = contactService.getContactById(id);
        activity.setContact(contact);
        Activity saved = activityRepository.save(activity);
        return ResponseEntity.ok(ApiResponse.success("Activity added", saved));
    }

    // ===================== SUB-RESOURCES: DEALS =====================

    @GetMapping("/{id}/deals")
    public ResponseEntity<ApiResponse<List<Deal>>> getContactDeals(@PathVariable Long id) {
        List<Deal> deals = contactService.getDealsByContactId(id);
        return ResponseEntity.ok(ApiResponse.success(deals));
    }

    // ===================== SUB-RESOURCES: TICKETS =====================

    @GetMapping("/{id}/tickets")
    public ResponseEntity<ApiResponse<List<Ticket>>> getContactTickets(@PathVariable Long id) {
        List<Ticket> tickets = contactService.getTicketsByContactId(id);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    // ===================== SUB-RESOURCES: DOCUMENTS =====================

    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<CustomerDocument>>> getContactDocuments(@PathVariable Long id) {
        List<CustomerDocument> documents = contactService.getDocumentsByContactId(id);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }
}
