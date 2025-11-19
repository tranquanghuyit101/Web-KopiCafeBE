package com.kopi.kopi.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PayOSUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Tạo signature chuẩn cho API /v2/payment-requests
     * format: amount={amount}&cancelUrl={cancelUrl}&description={desc}&orderCode={orderCode}&returnUrl={returnUrl}
     * Note: expiredAt KHÔNG được tính vào signature (theo PayOS docs)
     */
    public static String createPaymentSignature(long amount,
                                                 String cancelUrl,
                                                 String description,
                                                 int orderCode,
                                                 String returnUrl,
                                                 String checksumKey) {
        try {
            // PayOS signature chỉ tính từ các field này (theo thứ tự alphabet)
            String raw = "amount=" + amount
                    + "&cancelUrl=" + cancelUrl
                    + "&description=" + description
                    + "&orderCode=" + orderCode
                    + "&returnUrl=" + returnUrl;

            System.out.println("PayOS Payment Signature raw: " + raw);

            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }

            String signature = hex.toString();
            System.out.println("PayOS Payment Signature: " + signature);
            return signature;

        } catch (Exception e) {
            System.err.println("PayOS Payment Signature Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Signature failed: " + e.getMessage(), e);
        }
    }

    // ======= PHẦN DƯỚI GIỮ LẠI NHƯ CŨ (nếu bạn còn dùng cho webhook) =======

    /**
     * Tạo checksum generic (nếu muốn dùng cho webhook hoặc chỗ khác)
     */
    public static String createChecksum(Map<String, Object> data, String checksumKey) {
        try {
            Map<String, Object> cp = new HashMap<>(data);
            cp.remove("checksum");

            if (cp.containsKey("items")) {
                Object itemsObj = cp.get("items");
                if (itemsObj != null) {
                    String json = mapper.writeValueAsString(itemsObj);
                    json = json.replaceAll("\\s+", "");
                    cp.put("items", json);
                    System.out.println("PayOS Checksum - Items JSON: " + json);
                }
            }

            TreeMap<String, Object> sorted = new TreeMap<>(cp);
            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, Object> e : sorted.entrySet()) {
                if (e.getValue() != null) {
                    if (sb.length() > 0) sb.append("&");
                    String valueStr = (e.getValue() instanceof String)
                            ? (String) e.getValue()
                            : String.valueOf(e.getValue());
                    sb.append(e.getKey()).append("=").append(valueStr);
                }
            }

            String raw = sb.toString();
            System.out.println("PayOS Checksum String: " + raw);
            System.out.println("PayOS Checksum Key length: " + (checksumKey != null ? checksumKey.length() : 0));

            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }

            String checksum = hex.toString();
            System.out.println("PayOS Calculated Checksum: " + checksum);
            return checksum;

        } catch (Exception e) {
            System.err.println("PayOS Checksum Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Checksum failed: " + e.getMessage(), e);
        }
    }

    public static boolean verifyChecksum(Map<String, Object> data, String receivedChecksum, String checksumKey) {
        if (receivedChecksum == null || receivedChecksum.isBlank()) return false;
        String calculated = createChecksum(data, checksumKey);
        return calculated.equalsIgnoreCase(receivedChecksum);
    }
}
