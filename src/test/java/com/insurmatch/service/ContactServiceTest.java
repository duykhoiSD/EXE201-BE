package com.insurmatch.service;

import com.insurmatch.dto.ContactDTO;
import com.insurmatch.dto.ContactDetailDTO;
import com.insurmatch.entity.*;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private DealRepository dealRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CustomerDocumentRepository customerDocumentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactService contactService;

    private User sampleOwner;
    private Contact sampleContact;

    @BeforeEach
    void setUp() {
        sampleOwner = User.builder()
                .id(10L)
                .firstName("Khanh")
                .lastName("Nguyen")
                .email("manager@insurmatch.us")
                .build();

        sampleContact = Contact.builder()
                .id(1L)
                .firstName("Minh")
                .lastName("Tran")
                .email("minhtran@gmail.com")
                .phone("832-123-4567")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .gender("Male")
                .contactOwner(sampleOwner)
                .build();
    }

    @Test
    @DisplayName("Should return all contacts when search and owner are empty")
    void testGetAllContactsEmptyFilter() {
        when(contactRepository.findAll()).thenReturn(List.of(sampleContact));

        List<Contact> list = contactService.getAllContacts(null, null);

        assertNotNull(list);
        assertEquals(1, list.size());
        verify(contactRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should search contacts by keyword")
    void testGetAllContactsWithSearch() {
        when(contactRepository.searchContacts("Minh")).thenReturn(List.of(sampleContact));

        List<Contact> list = contactService.getAllContacts("Minh", "all");

        assertEquals(1, list.size());
        verify(contactRepository).searchContacts("Minh");
    }

    @Test
    @DisplayName("Should filter contacts by owner ID")
    void testGetAllContactsWithOwnerId() {
        when(contactRepository.findByContactOwnerId(10L)).thenReturn(List.of(sampleContact));

        List<Contact> list = contactService.getAllContacts(null, "10");

        assertEquals(1, list.size());
        verify(contactRepository).findByContactOwnerId(10L);
    }

    @Test
    @DisplayName("Should get contact by ID")
    void testGetContactByIdSuccess() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(sampleContact));

        Contact found = contactService.getContactById(1L);

        assertNotNull(found);
        assertEquals("Minh", found.getFirstName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when contact not found")
    void testGetContactByIdNotFound() {
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contactService.getContactById(999L));
    }

    @Test
    @DisplayName("Should resolve contact by Code string (e.g. CT26000001)")
    void testGetContactByIdOrCode() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(sampleContact));

        Contact foundByCode = contactService.getContactByIdOrCode("CT26000001");
        assertNotNull(foundByCode);
        assertEquals(1L, foundByCode.getId());

        Contact foundByIdStr = contactService.getContactByIdOrCode("1");
        assertNotNull(foundByIdStr);
        assertEquals(1L, foundByIdStr.getId());
    }

    @Test
    @DisplayName("Should create contact and automatically map owner by name")
    void testCreateContactFromDTOWithOwnerNameLookup() {
        ContactDTO dto = ContactDTO.builder()
                .firstName("Lan")
                .lastName("Nguyen")
                .contactOwnerName("Khanh Nguyen")
                .build();

        when(userRepository.searchByName("Khanh Nguyen")).thenReturn(List.of(sampleOwner));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        Contact created = contactService.createContactFromDTO(dto);

        assertNotNull(created);
        assertEquals("Lan", created.getFirstName());
        assertEquals(sampleOwner, created.getContactOwner());
        verify(userRepository).searchByName("Khanh Nguyen");
    }

    @Test
    @DisplayName("Should update contact fields properly")
    void testUpdateContactFromDTO() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(sampleContact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactDTO updateDto = ContactDTO.builder()
                .phone("999-888-7777")
                .city("Houston")
                .state("TX")
                .build();

        Contact updated = contactService.updateContactFromDTO(1L, updateDto);

        assertEquals("999-888-7777", updated.getPhone());
        assertEquals("Houston", updated.getCity());
        assertEquals("TX", updated.getState());
        assertEquals("Minh", updated.getFirstName()); // Unchanged field preserved
    }

    @Test
    @DisplayName("Should delete contact by ID")
    void testDeleteContact() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(sampleContact));

        contactService.deleteContact(1L);

        verify(contactRepository).delete(sampleContact);
    }

    @Test
    @DisplayName("Should fetch complete 360-degree contact detail")
    void testGetContactDetail360() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(sampleContact));
        when(noteRepository.findByContactIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(taskRepository.findByContactId(1L)).thenReturn(Collections.emptyList());
        when(activityRepository.findByContactIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(dealRepository.findByContactId(1L)).thenReturn(Collections.emptyList());
        when(ticketRepository.findByContactId(1L)).thenReturn(Collections.emptyList());
        when(customerDocumentRepository.findByContactId(1L)).thenReturn(Collections.emptyList());

        ContactDetailDTO detail = contactService.getContactDetail(1L);

        assertNotNull(detail);
        assertNotNull(detail.getContact());
        assertEquals("Minh Tran", detail.getContact().getFullName());
        assertEquals("CT26000001", detail.getContact().getCode());
        assertNotNull(detail.getNotes());
        assertNotNull(detail.getTasks());
        assertNotNull(detail.getActivities());
        assertNotNull(detail.getDeals());
        assertNotNull(detail.getTickets());
        assertNotNull(detail.getDocuments());
    }

    @Test
    @DisplayName("toDTO should populate fullName, code, and contactOwner UserSummary")
    void testToDTO() {
        ContactDTO dto = contactService.toDTO(sampleContact);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("CT26000001", dto.getCode());
        assertEquals("Minh Tran", dto.getFullName());
        assertNotNull(dto.getContactOwner());
        assertEquals("Khanh Nguyen", dto.getContactOwner().getName());
        assertEquals("manager@insurmatch.us", dto.getContactOwner().getEmail());
    }
}
