package com.kopi.kopi.controller;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.config.PayOSConfig;
import com.kopi.kopi.dto.PayOSPaymentResponse;
import com.kopi.kopi.service.impl.PayOSService;
import com.kopi.kopi.util.PayOSUtil;
import com.kopi.kopi.dto.PaymentDTO;
import com.kopi.kopi.service.impl.PaymentService;
import com.kopi.kopi.config.VNPAYConfig;
import com.kopi.kopi.util.VNPayUtil;
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
	private final VNPAYConfig vnpayConfig;

	@Value("${app.frontend.url:https://kopi-coffee-fe.vercel.app}")
	private String frontendUrl;

	public PaymentController(PaymentService paymentService, OrderRepository orderRepository, PaymentRepository paymentRepository, PayOSService payOSService, PayOSConfig payOSConfig, VNPAYConfig vnpayConfig) {
		this.paymentService = paymentService;
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
		this.payOSService = payOSService;
		this.payOSConfig = payOSConfig;
		this.vnpayConfig = vnpayConfig;
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
		try {
			// Verify checksum
			String receivedHash = params.get("vnp_SecureHash");
			if (receivedHash == null || receivedHash.isBlank()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Missing vnp_SecureHash"));
			}
			java.util.Map<String, String> filtered = new java.util.HashMap<>();
			for (Map.Entry<String, String> e : params.entrySet()) {
				String k = e.getKey();
				if (!"vnp_SecureHash".equals(k) && !"vnp_SecureHashType".equals(k)) {
					filtered.put(k, e.getValue());
				}
			}
			String hashData = VNPayUtil.getPaymentURL(filtered, false);
			String calcHash = VNPayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData);
			if (calcHash == null || !calcHash.equalsIgnoreCase(receivedHash)) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid checksum"));
			}

			// Extract and process
			String responseCode = params.get("vnp_ResponseCode");
			String transactionStatus = params.getOrDefault("vnp_TransactionStatus", responseCode);
			String orderInfo = params.get("vnp_OrderInfo");
			String txnRef = params.get("vnp_TxnRef");

			Integer orderId = null;
			if (orderInfo != null && orderInfo.startsWith("ORDER:")) {
				try {
					orderId = Integer.valueOf(orderInfo.substring("ORDER:".length()));
				} catch (Exception ignored) {}
			}

			boolean isSuccess = "00".equals(responseCode) && "00".equals(transactionStatus);

			String redirect;
			if (isSuccess && orderId != null) {
				OrderEntity order = orderRepository.findById(orderId).orElse(null);
				if (order != null && order.getPayments() != null && !order.getPayments().isEmpty()) {
					Payment pay = order.getPayments().get(0);
					pay.setStatus(PaymentStatus.PAID);
					pay.setPaidAt(LocalDateTime.now());
					if (txnRef != null && (pay.getTxnRef() == null || !Objects.equals(pay.getTxnRef(), txnRef))) {
						pay.setTxnRef(txnRef);
					}
					paymentRepository.save(pay);
				}
				redirect = frontendUrl + "/history/" + orderId;
			} else {
				redirect = frontendUrl + "/history?payment=" + (isSuccess ? "success" : "failed");
				if (orderId != null) {
					redirect += "&orderId=" + orderId;
					// Delete pre-created order if payment failed and no successful payment exists
					try {
						OrderEntity orderToDelete = orderRepository.findById(orderId).orElse(null);
						if (orderToDelete != null) {
							boolean hasPaid = orderToDelete.getPayments() != null
									&& orderToDelete.getPayments().stream().anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
							if (!hasPaid) {
								orderRepository.delete(orderToDelete);
							}
						}
					} catch (Exception ignored) {}
				}
			}

			return ResponseEntity.status(302).header("Location", redirect).build();
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
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

			System.out.println("PayOS Webhook - Found payment: " + payment.getPaymentId() + ", current status: " + payment.getStatus());

			// Cập nhật trạng thái thanh toán
			if (code != null && code == 0) {
				// Thanh toán thành công
				payment.setStatus(PaymentStatus.PAID);
				payment.setPaidAt(LocalDateTime.now());
				paymentRepository.save(payment);

				System.out.println("PayOS Webhook - Payment updated to PAID");
			} else {
				// Thanh toán thất bại
				payment.setStatus(PaymentStatus.CANCELLED);
				paymentRepository.save(payment);
				System.out.println("PayOS Webhook - Payment updated to CANCELLED");
				// Delete pre-created order if no successful payment exists
				try {
					OrderEntity orderToDelete = payment.getOrder();
					if (orderToDelete != null) {
						boolean hasPaid = orderToDelete.getPayments() != null
								&& orderToDelete.getPayments().stream().anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
						if (!hasPaid) {
							orderRepository.delete(orderToDelete);
						}
					}
				} catch (Exception ignored) {}
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
			Optional<Payment> paymentOpt = Optional.empty();
			
			// Tìm payment từ orderCode và kiểm tra status từ database
			if (orderCodeStr != null) {
				try {
					Integer orderCode = Integer.parseInt(orderCodeStr);
					String txnRefPrefix = "ORD-" + orderCode + "-";
					paymentOpt = paymentRepository.findAll().stream()
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

			// Get orderId from payment for redirect
			Integer orderIdForRedirect = null;
			if (paymentOpt.isPresent()) {
				Payment payment = paymentOpt.get();
				if (payment.getOrder() != null) {
					orderIdForRedirect = payment.getOrder().getOrderId();
					System.out.println("PayOS Return: Found orderId from payment.order: " + orderIdForRedirect);
				} else {
					// Fallback: extract orderId from txnRef format "ORD-{orderCode}-{orderId}"
					String txnRef = payment.getTxnRef();
					if (txnRef != null && txnRef.contains("-")) {
						try {
							String[] parts = txnRef.split("-");
							if (parts.length >= 3) {
								orderIdForRedirect = Integer.parseInt(parts[2]);
								System.out.println("PayOS Return: Found orderId from txnRef: " + orderIdForRedirect);
							}
						} catch (Exception ignored) {}
					}
				}
			}

			if (isSuccess) {
				// Thanh toán thành công → redirect đến trang order detail
				if (orderIdForRedirect != null) {
					return ResponseEntity.status(302)
							.header("Location", frontendUrl + "/history/" + orderIdForRedirect)
							.build();
				} else {
					// Fallback: redirect về trang chủ nếu không tìm thấy orderId
					return ResponseEntity.status(302)
							.header("Location", frontendUrl + "/")
							.build();
				}
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


