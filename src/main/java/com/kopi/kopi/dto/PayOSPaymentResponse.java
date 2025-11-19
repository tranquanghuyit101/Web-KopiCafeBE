package com.kopi.kopi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayOSPaymentResponse {
    private boolean success;
    private String paymentLink;
    private String qrCode;
    private String orderCode;
    private String message;
    private Long expiredAt; // timestamp in seconds
}

