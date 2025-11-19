package com.kopi.kopi.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PayOSConfig {

    @Value("${payment.payos.client-id}")
    private String clientId;

    @Value("${payment.payos.api-key}")
    private String apiKey;

    @Value("${payment.payos.checksum-key}")
    private String checksumKey;

    @Value("${payment.payos.api-url:https://api-merchant.payos.vn/v2/payment-requests}")
    private String apiUrl;

    @Value("${payment.payos.webhook-url}")
    private String webhookUrl;
}
