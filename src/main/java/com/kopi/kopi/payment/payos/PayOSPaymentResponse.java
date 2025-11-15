package com.kopi.kopi.payment.payos;

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

