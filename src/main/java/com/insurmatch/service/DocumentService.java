package com.insurmatch.service;

import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.DocumentFile;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.CustomerDocumentRepository;
import com.insurmatch.repository.DocumentFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final CustomerDocumentRepository documentRepository;
    private final DocumentFileRepository fileRepository;

    public CustomerDocument getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
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
