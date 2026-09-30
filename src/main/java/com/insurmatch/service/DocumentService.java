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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final CustomerDocumentRepository documentRepository;
    private final DocumentFileRepository fileRepository;
    private final ContactRepository contactRepository;
    private final ContactService contactService;

    public List<CustomerDocument> getAllDocuments(Long contactId, String owner) {
        return getAllDocuments(contactId != null ? String.valueOf(contactId) : null, owner, null);
    }

    public List<CustomerDocument> getAllDocuments(String contactIdentifier, String owner) {
        return getAllDocuments(contactIdentifier, owner, null);
    }

    public List<CustomerDocument> getAllDocuments(String contactIdentifier, String owner, String search) {
        List<CustomerDocument> docs;
        if (contactIdentifier != null && !contactIdentifier.isBlank()) {
            try {
                Contact c = contactService.getContactByIdOrCode(contactIdentifier);
                docs = documentRepository.findByContactId(c.getId());
            } catch (Exception ignored) {
                docs = documentRepository.findAll();
            }
        } else if (search != null && !search.trim().isEmpty()) {
            docs = documentRepository.searchDocuments(search.trim());
        } else if (owner != null && !owner.trim().isEmpty() && !owner.equalsIgnoreCase("all")) {
            docs = documentRepository.findByContactOwnerContainingIgnoreCase(owner.trim());
        } else {
            docs = documentRepository.findAll();
        }

        if (owner != null && !owner.trim().isEmpty() && !owner.equalsIgnoreCase("all") && search != null && !search.trim().isEmpty()) {
            String o = owner.trim().toLowerCase();
            docs = docs.stream().filter(d -> d.getContactOwner() != null && d.getContactOwner().toLowerCase().contains(o)).toList();
        }
        return docs;
    }

    public CustomerDocument getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    public CustomerDocument getDocumentByIdOrCode(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Document identifier cannot be empty");
        }
        Optional<CustomerDocument> byCode = documentRepository.findByCode(identifier.trim());
        if (byCode.isPresent()) return byCode.get();

        try {
            Long id = Long.parseLong(identifier.trim());
            return getDocumentById(id);
        } catch (NumberFormatException ignored) {}

        String digits = identifier.replaceAll("^[^0-9]+", "");
        if (!digits.isEmpty()) {
            try {
                long num = Long.parseLong(digits);
                return getDocumentById(num);
            } catch (Exception ignored) {}
        }
        throw new ResourceNotFoundException("Document not found with identifier: " + identifier);
    }

    public CustomerDocument createDocument(CustomerDocument doc, String contactIdentifier) {
        if (contactIdentifier != null && !contactIdentifier.isBlank()) {
            Contact contact = contactService.getContactByIdOrCode(contactIdentifier);
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

    public CustomerDocument updateDocument(String identifier, CustomerDocument docData) {
        return updateDocument(getDocumentByIdOrCode(identifier).getId(), docData);
    }

    public DocumentFile addFile(Long documentId, DocumentFile file) {
        CustomerDocument doc = getDocumentById(documentId);
        file.setDocument(doc);
        return fileRepository.save(file);
    }

    public DocumentFile addFile(String docIdentifier, DocumentFile file) {
        return addFile(getDocumentByIdOrCode(docIdentifier).getId(), file);
    }

    public void deleteFile(Long documentId, Long fileId) {
        // Verify document exists
        getDocumentById(documentId);
        DocumentFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        fileRepository.delete(file);
    }

    public void deleteFile(String docIdentifier, Long fileId) {
        deleteFile(getDocumentByIdOrCode(docIdentifier).getId(), fileId);
    }
}
