package com.insurmatch.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String url;
    private String secureUrl;
    private String publicId;
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;
    private String originalFilename;
}
