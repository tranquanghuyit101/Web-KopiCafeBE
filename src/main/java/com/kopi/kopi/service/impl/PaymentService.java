package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.dto.PaymentDTO;
import com.kopi.kopi.config.VNPAYConfig;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.PaymentRepository;
import com.kopi.kopi.util.VNPayUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {
	private final VNPAYConfig vnPayConfig;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;

	public PaymentService(VNPAYConfig vnPayConfig, OrderRepository orderRepository, PaymentRepository paymentRepository) {
		this.vnPayConfig = vnPayConfig;
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
	}

	@Transactional
	public PaymentDTO createVnPayPayment(Integer orderId, String clientIp) {
		OrderEntity order = orderRepository.findById(orderId).orElseThrow();
		if (order.getPayments() == null || order.getPayments().isEmpty()) {
			throw new IllegalStateException("Order has no payment record");
		}
		Payment pay = order.getPayments().get(0);
		BigDecimal amount = pay.getAmount() == null ? BigDecimal.ZERO : pay.getAmount();
		long vnpAmount = amount.multiply(new BigDecimal("100")).longValue();

		Map<String, String> vnpParamsMap = new HashMap<>(vnPayConfig.getVNPayConfigBase());
		vnpParamsMap.put("vnp_TxnRef", VNPayUtil.getRandomNumber(8));
		vnpParamsMap.put("vnp_Amount", String.valueOf(vnpAmount));
		vnpParamsMap.put("vnp_OrderType", "other");
		vnpParamsMap.put("vnp_OrderInfo", "ORDER:" + order.getOrderId());
		vnpParamsMap.put("vnp_IpAddr", (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp);

		// build query url
		String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
		String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
		String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
		queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
		String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

		// persist txn ref for reconciliation
		pay.setTxnRef(vnpParamsMap.get("vnp_TxnRef"));
		paymentRepository.save(pay);

		return PaymentDTO.builder()
				.code("ok")
				.message("success")
				.paymentUrl(paymentUrl)
				.build();
	}
}


