package com.insurmatch.service;

import com.insurmatch.entity.Contact;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    public List<Contact> getAllContacts(String search, String owner) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasOwner = owner != null && !owner.isBlank() && !owner.equalsIgnoreCase("all");

        if (hasSearch && hasOwner) {
            try {
                Long ownerId = Long.parseLong(owner);
                return contactRepository.searchContactsByOwner(search, ownerId);
            } catch (NumberFormatException e) {
                return contactRepository.searchContacts(search);
            }
        } else if (hasSearch) {
            return contactRepository.searchContacts(search);
        } else if (hasOwner) {
            try {
                Long ownerId = Long.parseLong(owner);
                return contactRepository.findByContactOwnerId(ownerId);
            } catch (NumberFormatException e) {
                return contactRepository.findAll();
            }
        }
        return contactRepository.findAll();
    }

    public Contact getContactById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
    }

    public Contact createContact(Contact contact) {
        return contactRepository.save(contact);
    }

    public Contact updateContact(Long id, Contact contactData) {
        Contact existing = getContactById(id);
        // Update fields that are not null
        if (contactData.getFirstName() != null) existing.setFirstName(contactData.getFirstName());
        if (contactData.getLastName() != null) existing.setLastName(contactData.getLastName());
        if (contactData.getMiddleName() != null) existing.setMiddleName(contactData.getMiddleName());
        if (contactData.getEmail() != null) existing.setEmail(contactData.getEmail());
        if (contactData.getPhone() != null) existing.setPhone(contactData.getPhone());
        if (contactData.getDateOfBirth() != null) existing.setDateOfBirth(contactData.getDateOfBirth());
        if (contactData.getSsn() != null) existing.setSsn(contactData.getSsn());
        if (contactData.getGender() != null) existing.setGender(contactData.getGender());
        if (contactData.getImmigrationStatus() != null) existing.setImmigrationStatus(contactData.getImmigrationStatus());
        if (contactData.getAddress() != null) existing.setAddress(contactData.getAddress());
        if (contactData.getCity() != null) existing.setCity(contactData.getCity());
        if (contactData.getState() != null) existing.setState(contactData.getState());
        if (contactData.getZipCode() != null) existing.setZipCode(contactData.getZipCode());
        if (contactData.getHouseholdSize() != null) existing.setHouseholdSize(contactData.getHouseholdSize());
        if (contactData.getEstimatedIncome() != null) existing.setEstimatedIncome(contactData.getEstimatedIncome());
        if (contactData.getAcaUsername() != null) existing.setAcaUsername(contactData.getAcaUsername());
        if (contactData.getAcaPassword() != null) existing.setAcaPassword(contactData.getAcaPassword());
        if (contactData.getAcaStatus() != null) existing.setAcaStatus(contactData.getAcaStatus());
        if (contactData.getSourceChannel() != null) existing.setSourceChannel(contactData.getSourceChannel());
        if (contactData.getSourceDetail() != null) existing.setSourceDetail(contactData.getSourceDetail());
        return contactRepository.save(existing);
    }

    public long count() {
        return contactRepository.count();
    }
}
