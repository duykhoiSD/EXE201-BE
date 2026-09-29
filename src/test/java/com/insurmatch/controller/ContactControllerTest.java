package com.insurmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurmatch.dto.ContactDTO;
import com.insurmatch.dto.ContactDetailDTO;
import com.insurmatch.entity.*;
import com.insurmatch.repository.ActivityRepository;
import com.insurmatch.repository.NoteRepository;
import com.insurmatch.repository.TaskRepository;
import com.insurmatch.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ContactService contactService;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ContactController contactController;

    private Contact sampleContact;
    private ContactDTO sampleContactDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contactController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        sampleContact = Contact.builder()
                .id(1L)
                .firstName("Minh")
                .lastName("Tran")
                .email("minhtran@gmail.com")
                .phone("832-123-4567")
                .build();

        sampleContactDTO = ContactDTO.builder()
                .id(1L)
                .code("CT26000001")
                .fullName("Minh Tran")
                .firstName("Minh")
                .lastName("Tran")
                .email("minhtran@gmail.com")
                .phone("832-123-4567")
                .build();
    }

    @Test
    @DisplayName("GET /api/contacts - Should return contacts list")
    void testGetContacts() throws Exception {
        when(contactService.getAllContacts(any(), any())).thenReturn(List.of(sampleContact));
        when(contactService.toDTOList(any())).thenReturn(List.of(sampleContactDTO));

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].fullName").value("Minh Tran"));
    }

    @Test
    @DisplayName("GET /api/contacts/{id} - Should return single contact by ID or Code")
    void testGetContactById() throws Exception {
        when(contactService.getContactByIdOrCode("1")).thenReturn(sampleContact);
        when(contactService.toDTO(sampleContact)).thenReturn(sampleContactDTO);

        mockMvc.perform(get("/api/contacts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("CT26000001"));
    }

    @Test
    @DisplayName("GET /api/contacts/{id}/detail - Should return 360-degree contact detail")
    void testGetContactDetail() throws Exception {
        ContactDetailDTO detailDTO = ContactDetailDTO.builder()
                .contact(sampleContactDTO)
                .notes(Collections.emptyList())
                .tasks(Collections.emptyList())
                .activities(Collections.emptyList())
                .deals(Collections.emptyList())
                .tickets(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();

        when(contactService.getContactByIdOrCode("1")).thenReturn(sampleContact);
        when(contactService.getContactDetail(1L)).thenReturn(detailDTO);

        mockMvc.perform(get("/api/contacts/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contact.fullName").value("Minh Tran"));
    }

    @Test
    @DisplayName("POST /api/contacts - Should create contact and return 200 with DTO")
    void testCreateContact() throws Exception {
        when(contactService.createContactFromDTO(any(ContactDTO.class))).thenReturn(sampleContact);
        when(contactService.toDTO(sampleContact)).thenReturn(sampleContactDTO);

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleContactDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contact created"))
                .andExpect(jsonPath("$.data.fullName").value("Minh Tran"));
    }

    @Test
    @DisplayName("PUT /api/contacts/{id} - Should update contact")
    void testUpdateContact() throws Exception {
        when(contactService.updateContactFromDTO(eq(1L), any(ContactDTO.class))).thenReturn(sampleContact);
        when(contactService.toDTO(sampleContact)).thenReturn(sampleContactDTO);

        mockMvc.perform(put("/api/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleContactDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contact updated"));
    }

    @Test
    @DisplayName("DELETE /api/contacts/{id} - Should delete contact")
    void testDeleteContact() throws Exception {
        doNothing().when(contactService).deleteContact(1L);

        mockMvc.perform(delete("/api/contacts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contact deleted"));

        verify(contactService, times(1)).deleteContact(1L);
    }

    @Test
    @DisplayName("POST /api/contacts/{id}/notes - Should add note to contact")
    void testAddNote() throws Exception {
        Note note = Note.builder().text("Follow up next week").build();
        Note savedNote = Note.builder().id(101L).text("Follow up next week").contact(sampleContact).build();

        when(contactService.getContactById(1L)).thenReturn(sampleContact);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        mockMvc.perform(post("/api/contacts/1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Note added"))
                .andExpect(jsonPath("$.data.id").value(101));
    }

    @Test
    @DisplayName("POST /api/contacts/{id}/tasks - Should add task to contact")
    void testAddTask() throws Exception {
        Task task = Task.builder().title("Call client").build();
        Task savedTask = Task.builder().id(201L).title("Call client").contact(sampleContact).build();

        when(contactService.getContactById(1L)).thenReturn(sampleContact);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        mockMvc.perform(post("/api/contacts/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task added"))
                .andExpect(jsonPath("$.data.id").value(201));
    }

    @Test
    @DisplayName("POST /api/contacts/{id}/activities - Should add activity to contact")
    void testAddActivity() throws Exception {
        Activity activity = Activity.builder().summary("Spoke on phone for 10 mins").build();
        Activity savedActivity = Activity.builder().id(301L).summary("Spoke on phone for 10 mins").contact(sampleContact).build();

        when(contactService.getContactById(1L)).thenReturn(sampleContact);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        mockMvc.perform(post("/api/contacts/1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Activity added"))
                .andExpect(jsonPath("$.data.id").value(301));
    }
}
