package com.insurmatch.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.insurmatch.dto.upload.UploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    /**
     * Kiểm tra xem cấu hình Cloudinary đã được điền đầy đủ chưa.
     */
    public boolean isConfigured() {
        return cloudName != null && !cloudName.trim().isEmpty() && !"your-cloud-name".equalsIgnoreCase(cloudName.trim())
                && apiKey != null && !apiKey.trim().isEmpty() && !"your-api-key".equalsIgnoreCase(apiKey.trim());
    }

    /**
     * Tải ảnh / tài liệu lên Cloudinary.
     * @param file File tải lên (ảnh hoặc PDF)
     * @param folder Thư mục đích trên Cloudinary (vd: "avatars", "documents", "policies")
     * @return UploadResponse chuẩn chứa URL, publicId, kích thước
     */
    public UploadResponse uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống");
        }

        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary chưa được cấu hình. Vui lòng cập nhật CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET trong file .env");
        }

        String targetFolder = (folder != null && !folder.trim().isEmpty()) ? folder.trim() : "insurmatch";

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", targetFolder,
                "resource_type", "auto"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

        String secureUrl = (String) uploadResult.get("secure_url");
        String url = (String) uploadResult.get("url");
        String publicId = (String) uploadResult.get("public_id");
        String format = (String) uploadResult.get("format");
        Number bytes = (Number) uploadResult.get("bytes");
        Number width = (Number) uploadResult.get("width");
        Number height = (Number) uploadResult.get("height");

        log.info("Cloudinary upload success: public_id={}, url={}", publicId, secureUrl);

        return UploadResponse.builder()
                .url(url)
                .secureUrl(secureUrl)
                .publicId(publicId)
                .format(format)
                .bytes(bytes != null ? bytes.longValue() : null)
                .width(width != null ? width.intValue() : null)
                .height(height != null ? height.intValue() : null)
                .originalFilename(file.getOriginalFilename())
                .build();
    }

    /**
     * Xóa ảnh trên Cloudinary theo publicId.
     */
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new IllegalArgumentException("publicId không được để trống");
        }

        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary chưa được cấu hình.");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId.trim(), ObjectUtils.emptyMap());
            String status = (String) result.get("result");
            log.info("Cloudinary delete result for public_id {}: {}", publicId, status);
            return "ok".equalsIgnoreCase(status);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", e.getMessage(), e);
            return false;
        }
    }
}
