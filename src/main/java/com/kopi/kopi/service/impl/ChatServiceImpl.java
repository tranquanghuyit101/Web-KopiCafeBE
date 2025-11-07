package com.kopi.kopi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopi.kopi.dto.chat.ChatMessage;
import com.kopi.kopi.dto.chat.ChatRequest;
import com.kopi.kopi.dto.chat.ChatResponse;
import com.kopi.kopi.service.IChatService;
import com.kopi.kopi.service.OrderService;
import com.kopi.kopi.service.ProductService;
import com.kopi.kopi.service.ReportService;
import com.kopi.kopi.service.ai.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final GeminiClient geminiClient;
    private final ProductService productService;
    private final ReportService reportService;
    private final OrderService orderService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.gemini.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;

    @Override
    public ChatResponse processMessage(ChatRequest request, Integer userId, String userRole) {
        String userMessage = request.getMessage();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ChatResponse.builder()
                    .message("Xin chào! Tôi có thể giúp gì cho bạn?")
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        }

        try {
            // Phân tích intent bằng Gemini AI
            String intent = analyzeIntent(userMessage, userRole);
            
            // Xử lý theo intent
            switch (intent.toLowerCase()) {
                case "order":
                    return handleOrderIntent(userMessage, userId);
                case "revenue":
                    if ("ADMIN".equalsIgnoreCase(userRole)) {
                        return handleRevenueIntent(userMessage);
                    }
                    return ChatResponse.builder()
                            .message("Xin lỗi, chỉ admin mới có thể xem báo cáo doanh thu.")
                            .intent("revenue")
                            .build();
                case "inventory":
                    if ("ADMIN".equalsIgnoreCase(userRole)) {
                        return handleInventoryIntent(userMessage);
                    }
                    return ChatResponse.builder()
                            .message("Xin lỗi, chỉ admin mới có thể kiểm tra tồn kho.")
                            .intent("inventory")
                            .build();
                default:
                    return handleGeneralIntent(userMessage, userRole);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.")
                    .intent("general")
                    .build();
        }
    }

    private String analyzeIntent(String message, String userRole) {
        try {
            String prompt = String.format("""
                Phân tích câu hỏi của người dùng và trả về intent phù hợp. Chỉ trả về JSON với format:
                {"intent": "order|revenue|inventory|general"}
                
                Quy tắc:
                - "order": đặt hàng, mua hàng, thêm vào giỏ, sản phẩm để mua
                - "revenue": doanh thu, báo cáo, thống kê, phân tích doanh thu (chỉ cho admin)
                - "inventory": tồn kho, kiểm tra kho, số lượng sản phẩm, sản phẩm trong kho (chỉ cho admin)
                - "general": các câu hỏi khác
                
                Vai trò người dùng: %s
                Câu hỏi: %s
                
                Chỉ trả về JSON, không có text khác.
                """, userRole, message);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("candidates").path(0).path("content").path(0).path("parts")
                    .path(0).path("text").asText("{\"intent\":\"general\"}");

            JsonNode parsed = objectMapper.readTree(text);
            return parsed.path("intent").asText("general");
        } catch (Exception e) {
            // Fallback: phân tích đơn giản
            String lower = message.toLowerCase();
            if (lower.contains("đặt") || lower.contains("mua") || lower.contains("giỏ") || lower.contains("sản phẩm")) {
                return "order";
            }
            if (lower.contains("doanh thu") || lower.contains("báo cáo") || lower.contains("thống kê")) {
                return "revenue";
            }
            if (lower.contains("tồn kho") || lower.contains("kiểm tra kho") || lower.contains("số lượng")) {
                return "inventory";
            }
            return "general";
        }
    }

    private ChatResponse handleOrderIntent(String message, Integer userId) {
        try {
            // Tìm sản phẩm theo từ khóa trong message
            Map<String, Object> products = productService.list(null, null, null, extractProductName(message), 10, 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productList = (List<Map<String, Object>>) products.get("data");

            if (productList != null && !productList.isEmpty()) {
                StringBuilder response = new StringBuilder("Tôi tìm thấy các sản phẩm sau:\n\n");
                for (Map<String, Object> p : productList) {
                    response.append(String.format("• %s - %s VNĐ (Còn: %s)\n",
                            p.get("name"),
                            formatPrice(p.get("price")),
                            p.get("stock")));
                }
                response.append("\nBạn có muốn đặt hàng không?");

                return ChatResponse.builder()
                        .message(response.toString())
                        .intent("order")
                        .data(productList)
                        .suggestions(List.of(
                                ChatMessage.builder().role("assistant").content("Đặt hàng ngay").build(),
                                ChatMessage.builder().role("assistant").content("Xem thêm sản phẩm").build()
                        ))
                        .build();
            } else {
                return ChatResponse.builder()
                        .message("Xin lỗi, tôi không tìm thấy sản phẩm nào phù hợp. Bạn có thể mô tả rõ hơn không?")
                        .intent("order")
                        .suggestions(getDefaultSuggestions("CUSTOMER"))
                        .build();
            }
        } catch (Exception e) {
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi tìm sản phẩm. Vui lòng thử lại.")
                    .intent("order")
                    .build();
        }
    }

    private ChatResponse handleRevenueIntent(String message) {
        try {
            // Phân tích yêu cầu: ngày/tháng/năm
            String period = extractPeriod(message);
            ReportService.Granularity granularity = ReportService.Granularity.monthly;
            LocalDate from = null;
            LocalDate to = LocalDate.now();

            if (period.contains("ngày") || period.contains("day")) {
                granularity = ReportService.Granularity.daily;
                from = to.minusDays(6);
            } else if (period.contains("tháng") || period.contains("month")) {
                granularity = ReportService.Granularity.monthly;
                from = to.minusMonths(6);
            } else if (period.contains("năm") || period.contains("year")) {
                granularity = ReportService.Granularity.yearly;
                from = to.minusYears(6);
            }

            List<com.kopi.kopi.dto.RevenuePoint> revenueData = reportService.revenue(granularity, from, to, 10);

            if (revenueData != null && !revenueData.isEmpty()) {
                StringBuilder response = new StringBuilder("📊 Báo cáo doanh thu:\n\n");
                double total = 0;
                int totalOrders = 0;

                for (com.kopi.kopi.dto.RevenuePoint point : revenueData) {
                    response.append(String.format("• %s: %s VNĐ (%d đơn hàng)\n",
                            point.getLabel(),
                            formatPrice(point.getTotal_sum()),
                            point.getOrderCount()));
                    if (point.getTotal_sum() != null) {
                        total += point.getTotal_sum().doubleValue();
                    }
                    totalOrders += point.getOrderCount();
                }

                response.append(String.format("\n📈 Tổng cộng: %s VNĐ (%d đơn hàng)",
                        formatPrice(total),
                        totalOrders));

                return ChatResponse.builder()
                        .message(response.toString())
                        .intent("revenue")
                        .data(revenueData)
                        .suggestions(List.of(
                                ChatMessage.builder().role("assistant").content("Xem doanh thu theo ngày").build(),
                                ChatMessage.builder().role("assistant").content("Xem doanh thu theo tháng").build(),
                                ChatMessage.builder().role("assistant").content("Xem doanh thu theo năm").build()
                        ))
                        .build();
            } else {
                return ChatResponse.builder()
                        .message("Không có dữ liệu doanh thu trong khoảng thời gian này.")
                        .intent("revenue")
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi lấy báo cáo doanh thu. Vui lòng thử lại.")
                    .intent("revenue")
                    .build();
        }
    }

    private ChatResponse handleInventoryIntent(String message) {
        try {
            // Tìm sản phẩm theo yêu cầu
            String searchTerm = extractProductName(message);
            Map<String, Object> products = productService.list(null, null, null, searchTerm, 50, 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productList = (List<Map<String, Object>>) products.get("data");

            if (productList != null && !productList.isEmpty()) {
                StringBuilder response = new StringBuilder("📦 Danh sách sản phẩm trong kho:\n\n");
                
                int lowStockCount = 0;
                for (Map<String, Object> p : productList) {
                    Integer stock = (Integer) p.get("stock");
                    String stockStatus = stock != null && stock < 10 ? "⚠️" : "✅";
                    if (stock != null && stock < 10) lowStockCount++;
                    
                    response.append(String.format("%s %s - Còn: %d\n",
                            stockStatus,
                            p.get("name"),
                            stock != null ? stock : 0));
                }

                if (lowStockCount > 0) {
                    response.append(String.format("\n⚠️ Cảnh báo: %d sản phẩm sắp hết hàng (< 10)", lowStockCount));
                }

                return ChatResponse.builder()
                        .message(response.toString())
                        .intent("inventory")
                        .data(productList)
                        .suggestions(List.of(
                                ChatMessage.builder().role("assistant").content("Xem sản phẩm sắp hết hàng").build(),
                                ChatMessage.builder().role("assistant").content("Xem tất cả sản phẩm").build()
                        ))
                        .build();
            } else {
                return ChatResponse.builder()
                        .message("Không tìm thấy sản phẩm nào phù hợp.")
                        .intent("inventory")
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi kiểm tra tồn kho. Vui lòng thử lại.")
                    .intent("inventory")
                    .build();
        }
    }

    private ChatResponse handleGeneralIntent(String message, String userRole) {
        // Sử dụng Gemini để trả lời câu hỏi chung
        try {
            String prompt = String.format("""
                Bạn là trợ lý ảo của quán cà phê Kopi. Trả lời câu hỏi của khách hàng một cách thân thiện, ngắn gọn.
                Vai trò người dùng: %s
                Câu hỏi: %s
                
                Trả lời bằng tiếng Việt, ngắn gọn (dưới 200 từ).
                """, userRole, message);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.7)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String reply = root.path("candidates").path(0).path("content").path(0).path("parts")
                    .path(0).path("text").asText("Xin lỗi, tôi không hiểu câu hỏi của bạn.");

            return ChatResponse.builder()
                    .message(reply)
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        } catch (Exception e) {
            return ChatResponse.builder()
                    .message("Xin chào! Tôi có thể giúp bạn đặt hàng, xem sản phẩm, hoặc trả lời các câu hỏi.")
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        }
    }

    private String extractProductName(String message) {
        // Đơn giản: loại bỏ các từ không cần thiết
        String[] stopWords = {"tôi", "muốn", "mua", "đặt", "xem", "cho", "tôi", "của", "với", "có", "không", "là", "gì", "nào"};
        String result = message.toLowerCase();
        for (String word : stopWords) {
            result = result.replaceAll("\\b" + word + "\\b", "").trim();
        }
        return result.trim();
    }

    private String extractPeriod(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("ngày") || lower.contains("day")) return "ngày";
        if (lower.contains("tháng") || lower.contains("month")) return "tháng";
        if (lower.contains("năm") || lower.contains("year")) return "năm";
        return "tháng"; // default
    }

    private String formatPrice(Object price) {
        if (price == null) return "0";
        if (price instanceof Number) {
            return String.format("%,.0f", ((Number) price).doubleValue());
        }
        return price.toString();
    }

    private List<ChatMessage> getDefaultSuggestions(String userRole) {
        List<ChatMessage> suggestions = new ArrayList<>();
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            suggestions.add(ChatMessage.builder().role("assistant").content("Xem doanh thu hôm nay").build());
            suggestions.add(ChatMessage.builder().role("assistant").content("Kiểm tra tồn kho").build());
            suggestions.add(ChatMessage.builder().role("assistant").content("Xem sản phẩm").build());
        } else {
            suggestions.add(ChatMessage.builder().role("assistant").content("Xem menu").build());
            suggestions.add(ChatMessage.builder().role("assistant").content("Đặt hàng").build());
            suggestions.add(ChatMessage.builder().role("assistant").content("Giờ mở cửa").build());
        }
        return suggestions;
    }
}

