package com.insurmatch.controller;

import com.insurmatch.dto.ApiResponse;
import com.insurmatch.dto.upload.UploadResponse;
import com.insurmatch.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * UploadController — Quản lý tải lên tệp & ảnh qua Cloudinary:
 *   POST   /api/upload/image    : Tải ảnh/tài liệu lên (tùy chọn folder)
 *   POST   /api/upload/avatar   : Tải ảnh đại diện người dùng
 *   POST   /api/upload/document : Tải tài liệu khách hàng / hợp đồng bảo hiểm
 *   DELETE /api/upload          : Xóa ảnh theo publicId
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    /**
     * 1. Tải ảnh tổng quát (cho phép truyền tên folder)
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "insurmatch") String folder
    ) {
        try {
            UploadResponse response = cloudinaryService.uploadImage(file, folder);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tải ảnh lên Cloudinary thành công!", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi tải ảnh lên Cloudinary: " + e.getMessage()));
        }
    }

    /**
     * 2. Tải ảnh đại diện (Avatar)
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            UploadResponse response = cloudinaryService.uploadImage(file, "insurmatch/avatars");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tải ảnh đại diện thành công!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi tải ảnh đại diện: " + e.getMessage()));
        }
    }

    /**
     * 3. Tải tài liệu / hợp đồng khách hàng (Document)
     */
    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            UploadResponse response = cloudinaryService.uploadImage(file, "insurmatch/documents");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tải tài liệu thành công!", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi tải tài liệu: " + e.getMessage()));
        }
    }

    /**
     * 4. Xóa ảnh khỏi Cloudinary
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam("publicId") String publicId) {
        try {
            boolean deleted = cloudinaryService.deleteImage(publicId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Xóa ảnh khỏi Cloudinary thành công!", null));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Không thể xóa ảnh. Vui lòng kiểm tra lại publicId."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa ảnh: " + e.getMessage()));
        }
    }
}
