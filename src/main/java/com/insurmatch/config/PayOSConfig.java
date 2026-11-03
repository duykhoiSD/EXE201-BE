package com.insurmatch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter @Setter
public class PayOSConfig {

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payos.endpoint:https://api-merchant.payos.vn}")
    private String endpoint;

    @Value("${app.frontend.url:https://thebestrateins-exe201.vercel.app}")
    private String frontendUrl;

    public boolean isConfigured() {
        return clientId != null && !clientId.trim().isEmpty()
                && apiKey != null && !apiKey.trim().isEmpty()
                && checksumKey != null && !checksumKey.trim().isEmpty();
    }
}
