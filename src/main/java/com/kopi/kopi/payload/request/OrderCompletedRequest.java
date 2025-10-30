package com.kopi.kopi.payload.request;

import lombok.Data;

@Data
public class OrderCompletedRequest {
    private Long orderId;
    private Integer customerId;
    private String orderCode;
}
