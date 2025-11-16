package com.kopi.kopi.payment.payos;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.PaymentRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PayOSService {

    private final PayOSConfig config;
    private final OrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private final RestTemplate rest = new RestTemplate();

    public PayOSService(PayOSConfig config,
                        OrderRepository orderRepo,
                        PaymentRepository paymentRepo) {
        this.config = config;
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
    }

    @Transactional
    public PayOSPaymentResponse createPayOSPayment(Integer orderId) {

        // 1. Load order
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getPayments() == null || order.getPayments().isEmpty()) {
            throw new IllegalStateException("Order has no payment record");
        }

        Payment payment = order.getPayments().get(0);

        long amount = payment.getAmount().longValue();
        if (amount <= 0) {
            throw new IllegalStateException("Invalid amount");
        }

        // 2. orderCode phải là int dương, unique (PayOS requirement)
        long timestamp = System.currentTimeMillis();
        int orderCode = (int) (timestamp % 2147483647L); // Max int
        if (orderCode <= 0) {
            orderCode = (int) (Math.abs(timestamp) % 1000000000L) + 1;
        }

        // 3. URLs
        String webhookUrl = config.getWebhookUrl();
        String baseUrl;
        if (webhookUrl != null && webhookUrl.contains("/apiv1")) {
            baseUrl = webhookUrl.substring(0, webhookUrl.indexOf("/apiv1"));
        } else {
            // Fallback local
            baseUrl = "http://localhost:8080/Kopi";
        }

        // Đảm bảo context path /Kopi nếu chạy trong Tomcat context
        if (!baseUrl.endsWith("/Kopi") && !baseUrl.contains("localhost:8080/Kopi")) {
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/Kopi";
            } else {
                baseUrl = baseUrl + "Kopi";
            }
        }

        String returnUrl = baseUrl + "/apiv1/payment/payos/return";
        String cancelUrl = baseUrl + "/apiv1/payment/payos/cancel";

        System.out.println("PayOS URLs - Base: " + baseUrl + ", Return: " + returnUrl + ", Cancel: " + cancelUrl);

        // 4. LIST items
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "Order-" + order.getOrderCode());
        item.put("quantity", 1);
        item.put("price", amount);
        items.add(item);

        // 5. description - PayOS max 25 characters
        String orderCodeStr = order.getOrderCode();
        String description = orderCodeStr.length() <= 25 ? orderCodeStr : orderCodeStr.substring(0, 25);

        // 6. Tính expiration time: 15 phút từ bây giờ (timestamp in seconds)
        // PayOS có thể không hỗ trợ expiredAt trong signature, thử không dùng trước
        long expirationTime = (System.currentTimeMillis() / 1000) + (15 * 60); // 15 phút = 900 giây
        System.out.println("PayOS Expiration Time: " + expirationTime + " (15 minutes from now)");

        // 7. Tạo signature theo format PayOS (KHÔNG bao gồm expiredAt trong signature)
        // PayOS signature chỉ tính từ: amount, cancelUrl, description, orderCode, returnUrl
        String signature = PayOSUtil.createPaymentSignature(
                amount,
                cancelUrl,
                description,
                orderCode,
                returnUrl,
                config.getChecksumKey()
        );

        // 8. Request body (PayOS dùng 'signature', không phải 'checksum')
        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("orderCode", orderCode);
        requestData.put("amount", amount);
        requestData.put("description", description);
        requestData.put("items", items); // items là LIST
        requestData.put("cancelUrl", cancelUrl);
        requestData.put("returnUrl", returnUrl);
        // Thử thêm expiredAt vào request body (nhưng không tính vào signature)
        requestData.put("expiredAt", expirationTime); // 15 phút
        requestData.put("signature", signature);

        // 9. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", config.getClientId().trim());
        headers.set("x-api-key", config.getApiKey().trim());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        // 10. Call PayOS
        try {
            System.out.println("=== PayOS Request ===");
            System.out.println("URL: " + config.getApiUrl());
            System.out.println("Client ID: " + config.getClientId().trim());
            System.out.println("Request Body: " + mapper.writeValueAsString(requestData));
            System.out.println("Signature: " + signature);
            System.out.println("=====================");

            ResponseEntity<Map> resp = rest.postForEntity(config.getApiUrl(), request, Map.class);
            Map<String, Object> body = resp.getBody();

            if (body == null) {
                throw new RuntimeException("Empty response from PayOS");
            }

            System.out.println("=== PayOS Response ===");
            System.out.println("Response: " + mapper.writeValueAsString(body));
            System.out.println("=====================");
            
            // Log QR code để debug
            Object qrCodeObj = ((Map<String, Object>) body.get("data")).get("qrCode");
            if (qrCodeObj != null) {
                System.out.println("PayOS QR Code type: " + qrCodeObj.getClass().getSimpleName());
                String qrCodeStr = String.valueOf(qrCodeObj);
                System.out.println("PayOS QR Code length: " + qrCodeStr.length());
                System.out.println("PayOS QR Code preview (first 100 chars): " + (qrCodeStr.length() > 100 ? qrCodeStr.substring(0, 100) : qrCodeStr));
            }

            // code = 0 hoặc "0" hoặc "00" = success
            Object codeObj = body.get("code");
            boolean isSuccess = false;

            if (codeObj instanceof Integer) {
                isSuccess = ((Integer) codeObj) == 0;
            } else if (codeObj instanceof Number) {
                isSuccess = ((Number) codeObj).intValue() == 0;
            } else if (codeObj != null) {
                String codeStr = String.valueOf(codeObj).trim();
                isSuccess = "0".equals(codeStr) || "00".equals(codeStr);
            }

            if (!isSuccess) {
                String desc = (String) body.get("desc");
                String errorMsg = desc != null ? desc : "Unknown error from PayOS";
                System.err.println("PayOS API Error - Code: " + codeObj + ", Desc: " + errorMsg);
                throw new RuntimeException("PayOS API error: " + errorMsg);
            }

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            if (data == null) {
                throw new RuntimeException("PayOS response missing data field");
            }

            // Lưu txnRef theo format ORD-{orderCode}-{orderId}
            payment.setTxnRef("ORD-" + orderCode + "-" + orderId);
            paymentRepo.save(payment);

            return PayOSPaymentResponse.builder()
                    .success(true)
                    .paymentLink((String) data.get("checkoutUrl"))
                    .qrCode((String) data.getOrDefault("qrCode", ""))
                    .orderCode(String.valueOf(orderCode))
                    .expiredAt(expirationTime)
                    .build();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            System.err.println("=== PayOS HTTP Error ===");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Error Body: " + errorBody);
            System.err.println("========================");

            String errorMsg = "PayOS API error (HTTP " + e.getStatusCode() + ")";
            if (errorBody != null && !errorBody.isEmpty()) {
                try {
                    Map<String, Object> errorJson = mapper.readValue(errorBody, Map.class);
                    String desc = (String) errorJson.get("desc");
                    if (desc != null && !desc.isEmpty()) {
                        errorMsg = desc;
                    }
                } catch (Exception ignored) {
                    if (errorBody.length() < 200) {
                        errorMsg = errorBody;
                    }
                }
            }
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            System.err.println("PayOS General Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PayOS error: " + e.getMessage(), e);
        }
    }
}
