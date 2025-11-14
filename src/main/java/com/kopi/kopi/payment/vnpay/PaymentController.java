package com.kopi.kopi.payment.vnpay;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/apiv1/payment")
public class PaymentController {
	private final PaymentService paymentService;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;

	@Value("${app.frontend.url:https://kopi-coffee-fe.vercel.app}")
	private String frontendUrl;

	public PaymentController(PaymentService paymentService, OrderRepository orderRepository, PaymentRepository paymentRepository) {
		this.paymentService = paymentService;
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
	}

	@GetMapping("/vn-pay")
	public ResponseEntity<?> createVNPayUrl(
			@RequestParam("orderId") Integer orderId,
			@RequestParam(value = "clientIp", required = false) String clientIp,
			@RequestHeader(value = "X-FORWARDED-FOR", required = false) String forwardedIp) {
		String ip = clientIp != null && !clientIp.isBlank() ? clientIp : (forwardedIp != null ? forwardedIp : null);
		PaymentDTO dto = paymentService.createVnPayPayment(orderId, ip);
		return ResponseEntity.ok(Map.of("message", "OK", "data", Map.of("payment_url", dto.getPaymentUrl())));
	}

	@GetMapping("/vn-pay-callback")
	@Transactional
	public ResponseEntity<?> vnPayCallback(@RequestParam Map<String, String> params) {
		String status = params.get("vnp_ResponseCode");
		String orderInfo = params.get("vnp_OrderInfo");
		String txnRef = params.get("vnp_TxnRef");
		Integer orderId = null;
		if (orderInfo != null && orderInfo.startsWith("ORDER:")) {
			try {
				orderId = Integer.valueOf(orderInfo.substring("ORDER:".length()));
			} catch (Exception ignored) {}
		}
		String redirect = frontendUrl + "/history?payment=" + ("00".equals(status) ? "success" : "failed");
		if (orderId != null) {
			redirect += "&orderId=" + orderId;
		}
		if ("00".equals(status) && orderId != null) {
			OrderEntity order = orderRepository.findById(orderId).orElse(null);
			if (order != null && order.getPayments() != null && !order.getPayments().isEmpty()) {
				Payment pay = order.getPayments().get(0);
				pay.setStatus(PaymentStatus.PAID);
				pay.setPaidAt(LocalDateTime.now());
				if (txnRef != null && (pay.getTxnRef() == null || !Objects.equals(pay.getTxnRef(), txnRef))) {
					pay.setTxnRef(txnRef);
				}
				paymentRepository.save(pay);
				order.setStatus("PAID");
				order.setUpdatedAt(LocalDateTime.now());
				orderRepository.save(order);
			}
		}
		return ResponseEntity.status(302).header("Location", redirect).build();
	}
}


