package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.DocumentFile;
import com.insurmatch.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDocument>> getDocument(@PathVariable Long id) {
        CustomerDocument doc = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(doc));
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
