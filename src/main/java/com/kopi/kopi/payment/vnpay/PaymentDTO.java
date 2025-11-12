package com.kopi.kopi.payment.vnpay;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDTO {
	private String code;
	private String message;
	private String paymentUrl;
}


