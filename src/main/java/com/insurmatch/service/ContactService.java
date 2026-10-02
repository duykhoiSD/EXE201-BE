package com.insurmatch.service;

import com.insurmatch.dto.ContactDTO;
import com.insurmatch.dto.ContactDetailDTO;
import com.insurmatch.entity.Activity;
import com.insurmatch.entity.Contact;
import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.Deal;
import com.insurmatch.entity.Note;
import com.insurmatch.entity.Task;
import com.insurmatch.entity.Ticket;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;
    private final NoteRepository noteRepository;
    private final TaskRepository taskRepository;
    private final ActivityRepository activityRepository;
    private final DealRepository dealRepository;
    private final TicketRepository ticketRepository;
    private final CustomerDocumentRepository customerDocumentRepository;
    private final UserRepository userRepository;

    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    // ===================== LIST & SEARCH =====================

    public List<Contact> getAllContacts(String search, String owner) {
        User currentUser = getCurrentAuthenticatedUser();
        Long ownerId = null;

        // RBAC: Agent can only view their own contacts
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            ownerId = currentUser.getId();
            log.info("RBAC: Scoping contacts strictly for agent: {} (id: {})", currentUser.getEmail(), currentUser.getId());
        } else if (owner != null && !owner.isBlank() && !owner.equalsIgnoreCase("all")) {
            try {
                ownerId = Long.parseLong(owner);
            } catch (NumberFormatException e) {
                List<User> found = userRepository.searchByNameOrEmail(owner);
                if (!found.isEmpty()) {
                    ownerId = found.get(0).getId();
                }
            }
        }

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasOwner = ownerId != null;

        if (hasSearch && hasOwner) {
            return contactRepository.searchContactsByOwner(search, ownerId);
        } else if (hasSearch) {
            return contactRepository.searchContacts(search);
        } else if (hasOwner) {
            return contactRepository.findByContactOwnerId(ownerId);
        }
        return contactRepository.findAll();
    }

    // ===================== GET BY ID =====================

    public Contact getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));

        // RBAC: Agent can only view their own contact
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (contact.getContactOwner() == null || !contact.getContactOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Bạn chỉ có quyền xem hồ sơ khách hàng do mình phụ trách!");
            }
        }

        return contact;
    }

    public Contact getContactByIdOrCode(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Contact identifier cannot be empty");
        }
        // 1. Check exact code column
        java.util.Optional<Contact> byCode = contactRepository.findByCode(identifier);
        if (byCode.isPresent()) {
            Contact c = byCode.get();
            validateAgentAccess(c);
            return c;
        }

        // 2. Direct numeric ID
        try {
            Long id = Long.parseLong(identifier);
            return getContactById(id);
        } catch (NumberFormatException ignored) {}

        // 3. Fallback: Parse digits (e.g. CT26002001 -> 2001 -> 1)
        String digits = identifier.replaceAll("^[^0-9]+", "");
        if (digits.startsWith("2600") && digits.length() > 4) {
            digits = digits.substring(4);
        }
        try {
            long num = Long.parseLong(digits);
            if (num > 2000) {
                try {
                    return getContactById(num - 2000);
                } catch (Exception ignored) {}
            }
            return getContactById(num);
        } catch (NumberFormatException | ResourceNotFoundException ex) {
            throw new ResourceNotFoundException("Contact not found with identifier: " + identifier);
        }
    }

    private void validateAgentAccess(Contact contact) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (contact.getContactOwner() == null || !contact.getContactOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Bạn chỉ có quyền xem hồ sơ khách hàng do mình phụ trách!");
            }
        }
    }

    // ===================== GET DETAIL 360° =====================

    /**
     * Lấy toàn bộ thông tin Contact 360° cho trang Contact Detail.
     * Gộp notes, tasks, activities, deals, tickets, documents trong 1 response.
     */
    public ContactDetailDTO getContactDetail(Long id) {
        Contact contact = getContactById(id);
        ContactDTO contactDTO = toDTO(contact);

        List<Note> notes = noteRepository.findByContactIdOrderByCreatedAtDesc(id);
        List<Task> tasks = taskRepository.findByContactId(id);
        List<Activity> activities = activityRepository.findByContactIdOrderByCreatedAtDesc(id);
        List<Deal> deals = dealRepository.findByContactId(id);
        List<Ticket> tickets = ticketRepository.findByContactId(id);
        List<CustomerDocument> documents = customerDocumentRepository.findByContactId(id);

        return ContactDetailDTO.builder()
                .contact(contactDTO)
                .notes(notes)
                .tasks(tasks)
                .activities(activities)
                .deals(deals)
                .tickets(tickets)
                .documents(documents)
                .build();
    }

    // ===================== CREATE =====================

    @Transactional
    public Contact createContact(Contact contact) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            contact.setContactOwner(currentUser);
        } else if (contact.getContactOwner() == null && currentUser != null) {
            contact.setContactOwner(currentUser);
        }
        return contactRepository.save(contact);
    }

    @Transactional
    public Contact createContactFromDTO(ContactDTO dto) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            dto.setContactOwnerId(currentUser.getId());
            dto.setContactOwnerName(currentUser.getName());
        }
        Contact contact = new Contact();
        applyDTOToEntity(dto, contact);
        return contactRepository.save(contact);
    }

    // ===================== UPDATE =====================

    @Transactional
    public Contact updateContact(Long id, Contact contactData) {
        Contact existing = getContactById(id);
        User currentUser = getCurrentAuthenticatedUser();

        // RBAC: Check edit permission & Reassignment rights
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            if (existing.getContactOwner() == null || !existing.getContactOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Bạn chỉ có quyền cập nhật hồ sơ khách hàng do mình phụ trách!");
            }
            // Agent CANNOT reassign contact owner
        } else {
            // ADMIN / STAFF can assign & reassign contact owner
            if (contactData.getContactOwner() != null) {
                existing.setContactOwner(contactData.getContactOwner());
            }
        }

        // Update fields that are not null
        if (contactData.getFirstName() != null) existing.setFirstName(contactData.getFirstName());
        if (contactData.getLastName() != null) existing.setLastName(contactData.getLastName());
        if (contactData.getMiddleName() != null) existing.setMiddleName(contactData.getMiddleName());
        if (contactData.getEmail() != null) existing.setEmail(contactData.getEmail());
        if (contactData.getPhone() != null) existing.setPhone(contactData.getPhone());
        if (contactData.getDateOfBirth() != null) existing.setDateOfBirth(contactData.getDateOfBirth());
        if (contactData.getSsn() != null) existing.setSsn(contactData.getSsn());
        if (contactData.getGender() != null) existing.setGender(contactData.getGender());
        // ---- Immigration ----
        if (contactData.getImmigrationStatus() != null) existing.setImmigrationStatus(contactData.getImmigrationStatus());
        if (contactData.getAlienNumber() != null) existing.setAlienNumber(contactData.getAlienNumber());
        if (contactData.getCertificateNumber() != null) existing.setCertificateNumber(contactData.getCertificateNumber());
        if (contactData.getDateExpired() != null) existing.setDateExpired(contactData.getDateExpired());
        // ---- Address ----
        if (contactData.getAddress() != null) existing.setAddress(contactData.getAddress());
        if (contactData.getMailingAddress() != null) existing.setMailingAddress(contactData.getMailingAddress());
        if (contactData.getStreetAddress() != null) existing.setStreetAddress(contactData.getStreetAddress());
        if (contactData.getCounty() != null) existing.setCounty(contactData.getCounty());
        if (contactData.getCity() != null) existing.setCity(contactData.getCity());
        if (contactData.getState() != null) existing.setState(contactData.getState());
        if (contactData.getZipCode() != null) existing.setZipCode(contactData.getZipCode());
        // ---- Relationship ----
        if (contactData.getRelationship() != null) existing.setRelationship(contactData.getRelationship());
        // ---- Point of Contact ----
        if (contactData.getPocName() != null) existing.setPocName(contactData.getPocName());
        if (contactData.getPocPhone() != null) existing.setPocPhone(contactData.getPocPhone());
        if (contactData.getPocRelationship() != null) existing.setPocRelationship(contactData.getPocRelationship());
        // ---- How do you know us ----
        if (contactData.getSourceChannel() != null) existing.setSourceChannel(contactData.getSourceChannel());
        if (contactData.getSourceDetail() != null) existing.setSourceDetail(contactData.getSourceDetail());
        // ---- Household info ----
        if (contactData.getHouseholdSize() != null) existing.setHouseholdSize(contactData.getHouseholdSize());
        if (contactData.getEstimatedIncome() != null) existing.setEstimatedIncome(contactData.getEstimatedIncome());
        // ---- ACA Account ----
        if (contactData.getAcaUsername() != null) existing.setAcaUsername(contactData.getAcaUsername());
        if (contactData.getAcaPassword() != null) existing.setAcaPassword(contactData.getAcaPassword());
        if (contactData.getAcaStatus() != null) existing.setAcaStatus(contactData.getAcaStatus());
        // ---- Ownership ----
        if (contactData.getObShareOwner() != null) existing.setObShareOwner(contactData.getObShareOwner());
        if (contactData.getMedicareShareOwner() != null) existing.setMedicareShareOwner(contactData.getMedicareShareOwner());
        if (contactData.getLifeShareOwner() != null) existing.setLifeShareOwner(contactData.getLifeShareOwner());
        if (contactData.getSupportAgent() != null) existing.setSupportAgent(contactData.getSupportAgent());

        return contactRepository.save(existing);
    }

    @Transactional
    public Contact updateContactFromDTO(Long id, ContactDTO dto) {
        Contact existing = getContactById(id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() == User.Role.AGENT) {
            // Agent cannot reassign contact owner
            dto.setContactOwnerId(null);
            dto.setContactOwnerName(null);
        }
        applyDTOToEntity(dto, existing);
        return contactRepository.save(existing);
    }

    // ===================== DELETE =====================

    @Transactional
    public void deleteContact(Long id) {
        Contact contact = getContactById(id);
        contactRepository.delete(contact);
    }

    // ===================== COUNT =====================

    public long count() {
        return contactRepository.count();
    }

    // ===================== SUB-RESOURCES =====================

    public List<Note> getNotesByContactId(Long contactId) {
        getContactById(contactId); // validate existence
        return noteRepository.findByContactIdOrderByCreatedAtDesc(contactId);
    }

    public List<Task> getTasksByContactId(Long contactId) {
        getContactById(contactId);
        return taskRepository.findByContactId(contactId);
    }

    public List<Activity> getActivitiesByContactId(Long contactId) {
        getContactById(contactId);
        return activityRepository.findByContactIdOrderByCreatedAtDesc(contactId);
    }

    public List<Deal> getDealsByContactId(Long contactId) {
        getContactById(contactId);
        return dealRepository.findByContactId(contactId);
    }

    public List<Ticket> getTicketsByContactId(Long contactId) {
        getContactById(contactId);
        return ticketRepository.findByContactId(contactId);
    }

    public List<CustomerDocument> getDocumentsByContactId(Long contactId) {
        getContactById(contactId);
        return customerDocumentRepository.findByContactId(contactId);
    }

    // ===================== DTO MAPPING HELPERS =====================

    public ContactDTO toDTO(Contact c) {
        String fullName = java.util.stream.Stream.of(c.getFirstName(), c.getMiddleName(), c.getLastName())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
        if (fullName.isEmpty()) fullName = "Unknown Contact";

        String code = c.getCode();

        ContactDTO.UserSummary ownerSummary = null;
        if (c.getContactOwner() != null) {
            ownerSummary = ContactDTO.UserSummary.builder()
                    .id(c.getContactOwner().getId())
                    .name(c.getContactOwner().getName())
                    .email(c.getContactOwner().getEmail())
                    .avatar(c.getContactOwner().getAvatar())
                    .build();
        }

        ContactDTO.UserSummary agentSummary = null;
        if (c.getSupportAgent() != null) {
            agentSummary = ContactDTO.UserSummary.builder()
                    .id(c.getSupportAgent().getId())
                    .name(c.getSupportAgent().getName())
                    .email(c.getSupportAgent().getEmail())
                    .avatar(c.getSupportAgent().getAvatar())
                    .build();
        }

        return ContactDTO.builder()
                .id(c.getId())
                .code(code)
                .fullName(fullName)
                .firstName(c.getFirstName())
                .middleName(c.getMiddleName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .dateOfBirth(c.getDateOfBirth())
                .ssn(c.getSsn())
                .gender(c.getGender())
                .language("Vietnamese")
                .status("Active")
                .immigrationStatus(c.getImmigrationStatus())
                .alienNumber(c.getAlienNumber())
                .certificateNumber(c.getCertificateNumber())
                .dateExpired(c.getDateExpired())
                .address(c.getAddress())
                .mailingAddress(c.getMailingAddress())
                .streetAddress(c.getStreetAddress())
                .county(c.getCounty())
                .city(c.getCity())
                .state(c.getState())
                .zipCode(c.getZipCode())
                .relationship(c.getRelationship())
                .pocName(c.getPocName())
                .pocPhone(c.getPocPhone())
                .pocRelationship(c.getPocRelationship())
                .sourceChannel(c.getSourceChannel())
                .sourceDetail(c.getSourceDetail())
                .householdSize(c.getHouseholdSize())
                .estimatedIncome(c.getEstimatedIncome())
                .acaUsername(c.getAcaUsername())
                .acaPassword(c.getAcaPassword())
                .acaStatus(c.getAcaStatus())
                .contactOwnerId(c.getContactOwner() != null ? c.getContactOwner().getId() : null)
                .contactOwnerName(c.getContactOwner() != null ? c.getContactOwner().getName() : null)
                .contactOwner(ownerSummary)
                .supportAgentId(c.getSupportAgent() != null ? c.getSupportAgent().getId() : null)
                .supportAgentName(c.getSupportAgent() != null ? c.getSupportAgent().getName() : null)
                .supportAgent(agentSummary)
                .obShareOwner(c.getObShareOwner())
                .medicareShareOwner(c.getMedicareShareOwner())
                .lifeShareOwner(c.getLifeShareOwner())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public List<ContactDTO> toDTOList(List<Contact> contacts) {
        return contacts.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private void applyDTOToEntity(ContactDTO dto, Contact entity) {
        if (dto.getCode() != null && !dto.getCode().isBlank()) entity.setCode(dto.getCode());
        if (dto.getFirstName() != null) entity.setFirstName(dto.getFirstName());
        if (dto.getMiddleName() != null) entity.setMiddleName(dto.getMiddleName());
        if (dto.getLastName() != null) entity.setLastName(dto.getLastName());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getDateOfBirth() != null) entity.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getSsn() != null) entity.setSsn(dto.getSsn());
        if (dto.getGender() != null) entity.setGender(dto.getGender());
        if (dto.getImmigrationStatus() != null) entity.setImmigrationStatus(dto.getImmigrationStatus());
        if (dto.getAlienNumber() != null) entity.setAlienNumber(dto.getAlienNumber());
        if (dto.getCertificateNumber() != null) entity.setCertificateNumber(dto.getCertificateNumber());
        if (dto.getDateExpired() != null) entity.setDateExpired(dto.getDateExpired());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getMailingAddress() != null) entity.setMailingAddress(dto.getMailingAddress());
        if (dto.getStreetAddress() != null) entity.setStreetAddress(dto.getStreetAddress());
        if (dto.getCounty() != null) entity.setCounty(dto.getCounty());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getZipCode() != null) entity.setZipCode(dto.getZipCode());
        if (dto.getRelationship() != null) entity.setRelationship(dto.getRelationship());
        if (dto.getPocName() != null) entity.setPocName(dto.getPocName());
        if (dto.getPocPhone() != null) entity.setPocPhone(dto.getPocPhone());
        if (dto.getPocRelationship() != null) entity.setPocRelationship(dto.getPocRelationship());
        if (dto.getSourceChannel() != null) entity.setSourceChannel(dto.getSourceChannel());
        if (dto.getSourceDetail() != null) entity.setSourceDetail(dto.getSourceDetail());
        if (dto.getHouseholdSize() != null) entity.setHouseholdSize(dto.getHouseholdSize());
        if (dto.getEstimatedIncome() != null) entity.setEstimatedIncome(dto.getEstimatedIncome());
        if (dto.getAcaUsername() != null) entity.setAcaUsername(dto.getAcaUsername());
        if (dto.getAcaPassword() != null) entity.setAcaPassword(dto.getAcaPassword());
        if (dto.getAcaStatus() != null) entity.setAcaStatus(dto.getAcaStatus());
        if (dto.getObShareOwner() != null) entity.setObShareOwner(dto.getObShareOwner());
        if (dto.getMedicareShareOwner() != null) entity.setMedicareShareOwner(dto.getMedicareShareOwner());
        if (dto.getLifeShareOwner() != null) entity.setLifeShareOwner(dto.getLifeShareOwner());

        // Owner lookup: by ID first, then by Name if ID is null
        if (dto.getContactOwnerId() != null) {
            User owner = userRepository.findById(dto.getContactOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getContactOwnerId()));
            entity.setContactOwner(owner);
        } else if (dto.getContactOwnerName() != null && !dto.getContactOwnerName().isBlank()) {
            List<User> found = userRepository.searchByName(dto.getContactOwnerName());
            if (!found.isEmpty()) {
                entity.setContactOwner(found.get(0));
            }
        }

        // Support Agent lookup: by ID first, then by Name
        if (dto.getSupportAgentId() != null) {
            User agent = userRepository.findById(dto.getSupportAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getSupportAgentId()));
            entity.setSupportAgent(agent);
        } else if (dto.getSupportAgentName() != null && !dto.getSupportAgentName().isBlank()) {
            List<User> found = userRepository.searchByName(dto.getSupportAgentName());
            if (!found.isEmpty()) {
                entity.setSupportAgent(found.get(0));
            }
        }
    }
}
