package com.insurmatch.service;

import com.insurmatch.entity.Contact;
import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.DocumentFile;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.ContactRepository;
import com.insurmatch.repository.CustomerDocumentRepository;
import com.insurmatch.repository.DocumentFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final CustomerDocumentRepository documentRepository;
    private final DocumentFileRepository fileRepository;
    private final ContactRepository contactRepository;

    public List<CustomerDocument> getAllDocuments(Long contactId, String owner) {
        if (contactId != null) {
            return documentRepository.findByContactId(contactId);
        }
        if (owner != null && !owner.trim().isEmpty()) {
            return documentRepository.findByContactOwnerContainingIgnoreCase(owner.trim());
        }
        return documentRepository.findAll();
    }

    public CustomerDocument getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    public CustomerDocument createDocument(CustomerDocument doc, Long contactId) {
        if (contactId != null) {
            Contact contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + contactId));
            doc.setContact(contact);
        } else {
            List<Contact> allContacts = contactRepository.findAll();
            if (!allContacts.isEmpty()) {
                doc.setContact(allContacts.get(0));
            } else {
                throw new IllegalArgumentException("Cannot create CustomerDocument without an associated Contact");
            }
        }
        if (doc.getInitials() == null || doc.getInitials().trim().isEmpty()) {
            if (doc.getName() != null && !doc.getName().isBlank()) {
                String[] parts = doc.getName().trim().split("\\s+");
                if (parts.length >= 2) {
                    doc.setInitials((parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase());
                } else if (parts.length == 1 && !parts[0].isEmpty()) {
                    doc.setInitials(parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase());
                }
            } else {
                doc.setInitials("CD");
            }
        }
        return documentRepository.save(doc);
    }

    public CustomerDocument updateDocument(Long id, CustomerDocument docData) {
        CustomerDocument existing = getDocumentById(id);
        if (docData.getName() != null && !docData.getName().isBlank()) {
            existing.setName(docData.getName().trim());
        }
        if (docData.getContactOwner() != null) {
            existing.setContactOwner(docData.getContactOwner());
        }
        if (docData.getLastModifiedBy() != null) {
            existing.setLastModifiedBy(docData.getLastModifiedBy());
        }
        if (docData.getInitials() != null && !docData.getInitials().isBlank()) {
            existing.setInitials(docData.getInitials());
        }
        return documentRepository.save(existing);
    }

    public DocumentFile addFile(Long documentId, DocumentFile file) {
        CustomerDocument doc = getDocumentById(documentId);
        file.setDocument(doc);
        return fileRepository.save(file);
    }

    public void deleteFile(Long documentId, Long fileId) {
        // Verify document exists
        getDocumentById(documentId);
        DocumentFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        fileRepository.delete(file);
    }
}
