package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.DocumentFile;
import com.insurmatch.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DocumentController — Khớp với FE:
 *   GET    /api/documents/:id
 *   POST   /api/documents/:docId/files
 *   DELETE /api/documents/:docId/files/:fileId
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDocument>>> getDocuments(
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) String owner) {
        List<CustomerDocument> docs = documentService.getAllDocuments(contactId, owner);
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDocument>> getDocument(@PathVariable Long id) {
        CustomerDocument doc = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDocument>> createDocument(
            @RequestBody CustomerDocument doc,
            @RequestParam(required = false) Long contactId) {
        CustomerDocument created = documentService.createDocument(doc, contactId);
        return ResponseEntity.ok(ApiResponse.success("Document created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDocument>> updateDocument(
            @PathVariable Long id,
            @RequestBody CustomerDocument docData) {
        CustomerDocument updated = documentService.updateDocument(id, docData);
        return ResponseEntity.ok(ApiResponse.success("Document updated", updated));
    }

    @PostMapping("/{docId}/files")
    public ResponseEntity<ApiResponse<DocumentFile>> addFile(@PathVariable Long docId, @RequestBody DocumentFile file) {
        DocumentFile saved = documentService.addFile(docId, file);
        return ResponseEntity.ok(ApiResponse.success("File added", saved));
    }

    @DeleteMapping("/{docId}/files/{fileId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(@PathVariable Long docId, @PathVariable Long fileId) {
        documentService.deleteFile(docId, fileId);
        return ResponseEntity.ok(ApiResponse.success("File deleted", null));
    }
}
