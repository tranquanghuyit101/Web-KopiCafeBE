package com.kopi.kopi.payload.request;

import lombok.Data;

@Data
public class OrderPaidRequest {
    private Long orderId;
    private Integer customerId;
    private Integer staffId;
    private String orderCode;
}
