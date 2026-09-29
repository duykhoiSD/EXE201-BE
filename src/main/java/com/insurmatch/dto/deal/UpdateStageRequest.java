package com.insurmatch.dto.deal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStageRequest {
    private String stage;
    private String user;
}
