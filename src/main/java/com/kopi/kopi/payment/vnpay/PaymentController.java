package com.kopi.kopi.payment.vnpay;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.payment.payos.PayOSConfig;
import com.kopi.kopi.payment.payos.PayOSPaymentResponse;
import com.kopi.kopi.payment.payos.PayOSService;
import com.kopi.kopi.payment.payos.PayOSUtil;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/apiv1/payment")
public class PaymentController {
	private final PaymentService paymentService;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final PayOSService payOSService;
	private final PayOSConfig payOSConfig;

	@Value("${app.frontend.url:http://localhost:3000}")
	private String frontendUrl;

	public PaymentController(PaymentService paymentService, OrderRepository orderRepository, PaymentRepository paymentRepository, PayOSService payOSService, PayOSConfig payOSConfig) {
		this.paymentService = paymentService;
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
		this.payOSService = payOSService;
		this.payOSConfig = payOSConfig;
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

	// PayOS endpoints
	@PostMapping("/payos")
	public ResponseEntity<?> createPayOSUrl(@RequestParam("orderId") Integer orderId) {
		try {
		PayOSPaymentResponse response = payOSService.createPayOSPayment(orderId);
		return ResponseEntity.ok(Map.of(
			"message", "OK",
			"data", Map.of(
				"payment_url", response.getPaymentLink(),
				"qr_code", response.getQrCode() != null ? response.getQrCode() : "",
				"order_code", response.getOrderCode(),
				"expired_at", response.getExpiredAt() != null ? response.getExpiredAt() : 0
			)
		));
		} catch (Exception e) {
			String errorMessage = e.getMessage();
			System.err.println("PayOS Error: " + errorMessage);
			if (e.getCause() != null) {
				System.err.println("PayOS Error Cause: " + e.getCause().getMessage());
			}
			e.printStackTrace();
			return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"error", errorMessage != null ? errorMessage : "Unknown error",
				"message", "Không thể tạo link thanh toán PayOS: " + (errorMessage != null ? errorMessage : "Lỗi không xác định")
			));
		}
	}

	@PostMapping("/payos/webhook")
	@Transactional
	public ResponseEntity<?> payOSWebhook(@RequestBody Map<String, Object> body) {
		try {
			System.out.println("=== PayOS Webhook Received ===");
			System.out.println("Body: " + body);
			
			// Verify checksum từ PayOS
			String receivedChecksum = (String) body.get("checksum");
			String checksumKey = payOSConfig.getChecksumKey();

			if (!PayOSUtil.verifyChecksum(body, receivedChecksum, checksumKey)) {
				System.err.println("PayOS Webhook: Invalid checksum");
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid checksum"));
			}

			// Lấy thông tin từ webhook
			Integer orderCode = (Integer) body.get("orderCode");
			Integer code = (Integer) body.get("code");
			String desc = (String) body.get("desc");
			
			System.out.println("PayOS Webhook - orderCode: " + orderCode + ", code: " + code + ", desc: " + desc);

			// Tìm payment theo txnRef (format: "ORD-{orderCode}-{orderId}")
			// Tìm payment có txnRef bắt đầu bằng "ORD-" + orderCode + "-"
			String txnRefPrefix = "ORD-" + orderCode + "-";
			Payment payment = paymentRepository.findAll().stream()
					.filter(p -> p.getTxnRef() != null && p.getTxnRef().startsWith(txnRefPrefix))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Payment not found for orderCode: " + orderCode));

			OrderEntity order = payment.getOrder();
			System.out.println("PayOS Webhook - Found payment: " + payment.getPaymentId() + ", current status: " + payment.getStatus());

			// Cập nhật trạng thái thanh toán
			if (code != null && code == 0) {
				// Thanh toán thành công
				payment.setStatus(PaymentStatus.PAID);
				payment.setPaidAt(LocalDateTime.now());
				paymentRepository.save(payment);

				order.setStatus("COMPLETED");
				orderRepository.save(order);
				System.out.println("PayOS Webhook - Payment updated to PAID, Order updated to COMPLETED");
			} else {
				// Thanh toán thất bại
				payment.setStatus(PaymentStatus.CANCELLED);
				paymentRepository.save(payment);
				System.out.println("PayOS Webhook - Payment updated to CANCELLED");
			}

			// PayOS yêu cầu trả về format này
			return ResponseEntity.ok(Map.of(
					"error", 0,
					"message", "Success",
					"data", Map.of()
			));
		} catch (Exception e) {
			System.err.println("PayOS Webhook Error: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/payos/return")
	public ResponseEntity<?> payOSReturn(@RequestParam Map<String, String> params) {
		try {
			System.out.println("PayOS Return Params: " + params);
			
			String orderCodeStr = params.get("orderCode");
			boolean isSuccess = false;
			
			// Tìm payment từ orderCode và kiểm tra status từ database
			if (orderCodeStr != null) {
				try {
					Integer orderCode = Integer.parseInt(orderCodeStr);
					String txnRefPrefix = "ORD-" + orderCode + "-";
					Optional<Payment> paymentOpt = paymentRepository.findAll().stream()
							.filter(p -> p.getTxnRef() != null && p.getTxnRef().startsWith(txnRefPrefix))
							.findFirst();
					
					if (paymentOpt.isPresent()) {
						Payment payment = paymentOpt.get();
						// Kiểm tra payment status từ database (webhook đã cập nhật)
						isSuccess = (payment.getStatus() == PaymentStatus.PAID);
						System.out.println("PayOS Payment Status from DB: " + payment.getStatus() + " (isSuccess: " + isSuccess + ")");
					}
				} catch (Exception e) {
					System.err.println("Error finding payment: " + e.getMessage());
				}
			}
			
			// Nếu không tìm thấy payment, kiểm tra code từ params
			if (orderCodeStr == null) {
				String code = params.get("code");
				if (code != null) {
					try {
						Integer codeInt = Integer.parseInt(code);
						isSuccess = (codeInt == 0);
					} catch (Exception ignored) {}
				}
			}
			
			// Default: coi như success (webhook đã xử lý)
			if (orderCodeStr == null && params.get("code") == null) {
				isSuccess = true;
			}

			if (isSuccess) {
				// Thanh toán thành công → redirect về trang chủ
				return ResponseEntity.status(302)
						.header("Location", frontendUrl + "/")
						.build();
			} else {
				// Thanh toán thất bại → redirect về trang chủ với thông báo lỗi
				return ResponseEntity.status(302)
						.header("Location", frontendUrl + "/?error=payment_failed")
						.build();
			}
		} catch (Exception e) {
			System.err.println("PayOS Return Error: " + e.getMessage());
			e.printStackTrace();
			// Default: redirect về trang chủ
			return ResponseEntity.status(302)
					.header("Location", frontendUrl + "/")
					.build();
		}
	}

	@GetMapping("/payos/cancel")
	public ResponseEntity<?> payOSCancel() {
		return ResponseEntity.status(302)
				.header("Location", frontendUrl + "/cart?payment=cancelled")
				.build();
	}
}


