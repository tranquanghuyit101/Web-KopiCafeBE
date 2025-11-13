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
            // Nếu FE gửi kèm ngữ cảnh đặt hàng, ưu tiên xử lý như intent=order
            Map<String, Object> reqOrderCtx = request.getOrderContext();
            if (request.getOrderState() != null || (reqOrderCtx != null && !reqOrderCtx.isEmpty())) {
                return handleOrderIntent(userMessage, userId, request.getHistory(), userRole, reqOrderCtx, request.getOrderState());
            }
            // Ưu tiên phân tích bằng rule-based trước (nhanh và đáng tin cậy hơn)
            String intent = analyzeIntentSmart(userMessage, userRole);

            // Xử lý theo intent
            switch (intent.toLowerCase()) {
                case "order":
                    return handleOrderIntent(userMessage, userId, request.getHistory(), userRole, request.getOrderContext(), request.getOrderState());
                case "revenue":
                    if ("ADMIN".equalsIgnoreCase(userRole)) {
                        return handleRevenueIntent(userMessage);
                    }
                    // STAFF/EMPLOYEE: chỉ xem doanh thu hôm nay
                    if ("STAFF".equalsIgnoreCase(userRole) || "EMPLOYEE".equalsIgnoreCase(userRole)) {
                        return handleRevenueIntentForStaff(userMessage);
                    }
                    return ChatResponse.builder()
                            .message("Xin lỗi, chỉ admin mới có thể xem báo cáo doanh thu. Vui lòng đăng nhập với tài khoản admin để sử dụng tính năng này.")
                            .intent("revenue")
                            .suggestions(getDefaultSuggestions(userRole))
                            .build();
                case "inventory":
                    // ADMIN và STAFF được xem tồn kho
                    if ("ADMIN".equalsIgnoreCase(userRole) ||
                        "STAFF".equalsIgnoreCase(userRole) ||
                        "EMPLOYEE".equalsIgnoreCase(userRole)) {
                        return handleInventoryIntent(userMessage);
                    }
                    return ChatResponse.builder()
                            .message("Xin lỗi, chỉ admin mới có thể kiểm tra tồn kho. Vui lòng đăng nhập với tài khoản admin để sử dụng tính năng này.")
                            .intent("inventory")
                            .suggestions(getDefaultSuggestions(userRole))
                            .build();
                default:
                    return handleGeneralIntent(userMessage, userRole);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: thử phân tích lại bằng rule-based
            String intent = analyzeIntentSmart(userMessage, userRole);
            if (!"general".equals(intent)) {
                return processMessage(request, userId, userRole);
            }
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau hoặc mô tả rõ hơn yêu cầu của bạn.")
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        }
    }

    /**
     * Phân tích intent thông minh bằng rule-based (ưu tiên) và Gemini AI (fallback)
     */
    private String analyzeIntentSmart(String message, String userRole) {
        String lower = message.toLowerCase().trim();

        // Rule-based analysis (ưu tiên - nhanh và đáng tin cậy)
        // Kiểm tra revenue intent
        if (lower.contains("doanh thu") || lower.contains("báo cáo") || lower.contains("thống kê") ||
            lower.contains("xem doanh thu") || lower.contains("doanh thu hôm nay") ||
            lower.contains("doanh thu hôm qua") || lower.contains("doanh thu theo") ||
            lower.contains("revenue") || lower.contains("doanh số") ||
            lower.contains("báo cáo doanh thu") || lower.contains("thống kê doanh thu")) {
            return "revenue";
        }

        // Kiểm tra inventory intent
        if (lower.contains("tồn kho") || lower.contains("kiểm tra kho") || lower.contains("kiểm tra tồn kho") ||
            lower.contains("số lượng") || lower.contains("hàng hóa") || lower.contains("kho hàng") ||
            lower.contains("sản phẩm trong kho") || lower.contains("sắp hết hàng") ||
            lower.contains("inventory") || lower.contains("stock") ||
            lower.contains("kiểm tra số lượng") || lower.contains("xem tồn kho")) {
            return "inventory";
        }

        // Kiểm tra order intent
        if (lower.contains("đặt") || lower.contains("mua") || lower.contains("giỏ") ||
            lower.contains("sản phẩm") || lower.contains("xem sản phẩm") ||
            lower.contains("tìm sản phẩm") || lower.contains("menu") || lower.contains("xem menu") ||
            lower.contains("danh sách sản phẩm") || lower.contains("sản phẩm nào") ||
            lower.contains("có gì") || lower.contains("bán gì") ||
            // Tiếp diễn flow đặt hàng
            lower.contains("tại quán") || lower.contains("bàn") ||
            lower.contains("ship") || lower.contains("giao hàng") || lower.contains("delivery") ||
            lower.contains("địa chỉ")) {
            return "order";
        }

        // Nếu không match rule-based, thử dùng Gemini AI (nếu có API key)
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                return analyzeIntent(message, userRole);
            } catch (Exception e) {
                // Nếu Gemini fail, trả về general
                return "general";
            }
        }

        return "general";
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

            if (response.getStatusCode().isError() || response.getBody() == null) {
                return fallbackIntent(message);
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // Kiểm tra lỗi từ API
            if (root.has("error")) {
                System.err.println("Gemini API Error: " + root.path("error").toString());
                return fallbackIntent(message);
            }

            String text = root.path("candidates").path(0).path("content").path(0).path("parts")
                    .path(0).path("text").asText("{\"intent\":\"general\"}");

            JsonNode parsed = objectMapper.readTree(text);
            String intent = parsed.path("intent").asText("general");

            // Nếu intent không hợp lệ, dùng fallback
            if (!intent.equals("order") && !intent.equals("revenue") &&
                !intent.equals("inventory") && !intent.equals("general")) {
                return fallbackIntent(message);
            }

            return intent;
        } catch (Exception e) {
            e.printStackTrace();
            return fallbackIntent(message);
        }
    }

    private String fallbackIntent(String message) {
        // Fallback: phân tích đơn giản với từ khóa mở rộng (không gọi API ngoài)
        String lower = message.toLowerCase().trim();
        if (lower.contains("đặt") || lower.contains("mua") || lower.contains("giỏ") ||
            lower.contains("sản phẩm") || lower.contains("xem sản phẩm") ||
            lower.contains("tìm sản phẩm") || lower.contains("menu") || lower.contains("xem menu") ||
            lower.contains("tại quán") || lower.contains("bàn") ||
            lower.contains("ship") || lower.contains("giao hàng") || lower.contains("delivery") ||
            lower.contains("địa chỉ")) {
            return "order";
        }
        if (lower.contains("doanh thu") || lower.contains("báo cáo") || lower.contains("thống kê") ||
            lower.contains("xem doanh thu") || lower.contains("doanh thu hôm nay") ||
            lower.contains("doanh thu hôm qua") || lower.contains("doanh thu theo") ||
            lower.contains("revenue") || lower.contains("doanh số")) {
            return "revenue";
        }
        if (lower.contains("tồn kho") || lower.contains("kiểm tra kho") || lower.contains("kiểm tra tồn kho") ||
            lower.contains("số lượng") || lower.contains("hàng hóa") || lower.contains("kho hàng") ||
            lower.contains("sản phẩm trong kho") || lower.contains("sắp hết hàng") ||
            lower.contains("inventory") || lower.contains("stock")) {
            return "inventory";
        }
        return "general";
    }

    private ChatResponse handleOrderIntent(String message, Integer userId, List<ChatMessage> history, String userRole, Map<String, Object> orderContext, String orderState) {
        try {
            String lowerMessage = message.toLowerCase().trim();

            // Kiểm tra xem có phải là yêu cầu xem menu/tất cả sản phẩm không
            boolean isViewMenu = lowerMessage.contains("xem menu") ||
                                lowerMessage.contains("xem sản phẩm") ||
                                lowerMessage.contains("danh sách sản phẩm") ||
                                lowerMessage.equals("đặt hàng") ||
                                lowerMessage.equals("menu") ||
                                lowerMessage.contains("có gì") ||
                                lowerMessage.contains("bán gì");

            if (isViewMenu) {
                // Hiển thị menu
                return showMenu();
            }

            // Parse order info từ message và history
            OrderParseResult parseResult = parseOrderMessage(message, history, orderContext, orderState);

            // Nếu không tìm thấy sản phẩm, hiển thị menu
            if (parseResult.productId == null) {
                // Tìm sản phẩm theo từ khóa
                String searchTerm = extractProductNameFromMessage(message);
                Map<String, Object> products = productService.list(null, null, null, searchTerm, 10, 1);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> productList = (List<Map<String, Object>>) products.get("data");

                if (productList != null && !productList.isEmpty()) {
                    if (productList.size() == 1) {
                        // Chỉ có 1 sản phẩm, hỏi số lượng
                        Map<String, Object> product = productList.get(0);
                        return askQuantity(product);
                    } else {
                        // Nhiều sản phẩm, hiển thị danh sách
                        return showProductList(productList, "Tôi tìm thấy các sản phẩm sau:\n\n");
                    }
                } else {
                    return ChatResponse.builder()
                            .message("Xin lỗi, tôi không tìm thấy sản phẩm nào phù hợp. Bạn có thể mô tả rõ hơn không?")
                            .intent("order")
                            .suggestions(getDefaultSuggestions("CUSTOMER"))
                            .build();
                }
            }

            // Có sản phẩm, kiểm tra số lượng
            if (parseResult.quantity == null || parseResult.quantity <= 0) {
                // Hỏi số lượng
                Map<String, Object> product = getProductById(parseResult.productId);
                if (product == null) {
                    return ChatResponse.builder()
                            .message("Xin lỗi, không tìm thấy sản phẩm.")
                            .intent("order")
                            .build();
                }
                parseResult.productName = parseResult.productName != null ? parseResult.productName : extractProductDisplayName(product);
                return askQuantity(product);
            }

            // Có sản phẩm và số lượng, kiểm tra delivery type
            if (parseResult.deliveryType == null) {
                // Hỏi delivery type
                Map<String, Object> product = getProductById(parseResult.productId);
                if (product == null) {
                    return ChatResponse.builder()
                            .message("Xin lỗi, không tìm thấy sản phẩm.")
                            .intent("order")
                            .build();
                }
                parseResult.productName = parseResult.productName != null ? parseResult.productName : (String) product.get("name");
                return askDeliveryType(product, parseResult.quantity);
            }

            // Có đủ thông tin, tạo đơn hàng
            return createOrderFromParseResult(parseResult, userId);

        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi xử lý đơn hàng. Vui lòng thử lại.")
                    .intent("order")
                    .build();
        }
    }

    // Helper class để lưu kết quả parse order message
    private static class OrderParseResult {
        Integer productId;
        Integer quantity;
        String deliveryType; // "dine_in", "delivery"
        Integer tableNumber;
        String address;
        String productName;
    }

    private OrderParseResult parseOrderMessage(String message, List<ChatMessage> history, Map<String, Object> orderContext, String orderState) {
        OrderParseResult result = new OrderParseResult();
        String lower = message.toLowerCase().trim();

        // Prefill từ orderContext nếu FE gửi kèm
        if (orderContext != null) {
            Object pid = orderContext.get("productId");
            if (pid instanceof Integer) result.productId = (Integer) pid;
            else if (pid instanceof Number) result.productId = ((Number) pid).intValue();
            Object q = orderContext.get("quantity");
            if (q instanceof Integer) result.quantity = (Integer) q;
            else if (q instanceof Number) result.quantity = ((Number) q).intValue();
            Object pn = orderContext.get("productName");
            if (pn != null) result.productName = String.valueOf(pn);
            Object dt = orderContext.get("deliveryType");
            if (dt != null) result.deliveryType = String.valueOf(dt);
            Object tbl = orderContext.get("tableNumber");
            if (tbl instanceof Number) result.tableNumber = ((Number) tbl).intValue();
            Object addr = orderContext.get("address");
            if (addr != null) result.address = String.valueOf(addr);
        }

        boolean expectingAddress = "ASKING_ADDRESS".equals(orderState);
        boolean expectingTable = "ASKING_TABLE".equals(orderState);

        // Parse từ message hiện tại (chỉ ghi đè khi chưa có sẵn trong context)
        if (!expectingAddress && !expectingTable) {
            Integer quantityCandidate = extractQuantity(lower);
            if (result.quantity == null && quantityCandidate != null) {
                result.quantity = quantityCandidate;
            }
        }

        // Tìm delivery type
        if (lower.contains("tại quán") || lower.contains("tại bàn") || lower.contains("dine in") ||
            lower.contains("bàn") || lower.contains("table")) {
            result.deliveryType = "dine_in";
            Integer tableNum = extractTableNumber(lower);
            result.tableNumber = tableNum;
        } else if (lower.contains("ship") || lower.contains("giao hàng") || lower.contains("delivery") ||
                   lower.contains("địa chỉ") || lower.contains("address")) {
            result.deliveryType = "delivery";
        }

        // Tìm sản phẩm từ message hiện tại
        String productName = extractProductNameFromMessage(message);
        if (productName != null && !productName.trim().isEmpty()) {
            Map<String, Object> product = findProductByName(productName);
            if (product != null) {
                result.productId = (Integer) product.get("id");
                result.productName = extractProductDisplayName(product);
            }
        }

        // Nếu không tìm thấy thông tin trong message hiện tại, parse từ history
        if (history != null && !history.isEmpty()) {
            // Tìm sản phẩm từ các message trước đó (cả user và assistant)
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessage msg = history.get(i);
                String msgContent = msg.getContent();

                // Tìm sản phẩm từ message của user
                if ("user".equals(msg.getRole()) && result.productId == null) {
                    String prevProductName = extractProductNameFromMessage(msgContent);
                    if (prevProductName != null && !prevProductName.trim().isEmpty()) {
                        Map<String, Object> product = findProductByName(prevProductName);
                        if (product != null) {
                            result.productId = (Integer) product.get("id");
                            result.productName = extractProductDisplayName(product);
                        }
                    }
                }

                // Tìm sản phẩm từ câu hỏi của assistant (ví dụ: "Bạn muốn đặt bao nhiêu Tiramisu Coffee?")
                if ("assistant".equals(msg.getRole()) && result.productId == null) {
                    // Tìm tên sản phẩm trong câu hỏi của AI (sau "bao nhiêu" hoặc "đặt")
                    String lowerMsg = msgContent.toLowerCase();
                    if (lowerMsg.contains("bao nhiêu") || lowerMsg.contains("đặt")) {
                        // Tìm từ sau "bao nhiêu" đến dấu "?" hoặc xuống dòng
                        int startIdx = Math.max(lowerMsg.indexOf("bao nhiêu"), lowerMsg.indexOf("đặt"));
                        if (startIdx >= 0) {
                            String afterKeyword = msgContent.substring(startIdx);
                            // Loại bỏ các từ không cần thiết
                            String extractedProductName = afterKeyword
                                .replaceAll("(?i)bao nhiêu|đặt|\\?", "")
                                .trim();
                            if (!extractedProductName.isEmpty() && extractedProductName.length() < 100) {
                                Map<String, Object> product = findProductByName(extractedProductName);
                                if (product != null) {
                                    result.productId = (Integer) product.get("id");
                                    result.productName = extractProductDisplayName(product);
                                }
                            }
                        }
                    }
                    // Fallback: thử khớp tên sản phẩm từ nội dung câu hỏi của assistant (ví dụ câu hỏi chọn hình thức giao hàng có kèm tên món)
                    if (result.productId == null && msgContent != null && !msgContent.isBlank()) {
                        Map<String, Object> hintProduct = findProductByNameFuzzy(msgContent);
                        if (hintProduct != null) {
                            result.productId = (Integer) hintProduct.get("id");
                            result.productName = extractProductDisplayName(hintProduct);
                        }
                    }
                }

                // Tìm số lượng từ message của user trong lịch sử (từ gần nhất trở về trước)
                if ("user".equals(msg.getRole()) && result.quantity == null) {
                    Integer prevQuantity = extractQuantity(msgContent.toLowerCase());
                    if (prevQuantity != null) result.quantity = prevQuantity;
                }

                // Nhận biết ngữ cảnh: assistant đang yêu cầu địa chỉ giao hàng
                if ("assistant".equals(msg.getRole()) && result.deliveryType == null && msgContent != null) {
                    String lowerMsg = msgContent.toLowerCase();
                    if (lowerMsg.contains("cung cấp địa chỉ") ||
                        lowerMsg.contains("địa chỉ giao hàng") ||
                        lowerMsg.contains("vui lòng cung cấp địa chỉ")) {
                        result.deliveryType = "delivery";
                    }
                }
            }
        }

        // Nếu người dùng vừa gửi một chuỗi có vẻ là địa chỉ trần (không có từ khóa)
        if ((result.address == null || result.address.isBlank())) {
            String guessed = guessAddress(message);
            if (guessed != null) {
                result.address = guessed;
                if (result.deliveryType == null) {
                    result.deliveryType = "delivery";
                }
            }
        }

        // Nếu vẫn không có số lượng, kiểm tra xem có phải là câu trả lời số lượng không
        if (result.quantity == null) {
            // Kiểm tra xem message có phải là số đơn giản không (ví dụ: "2", "3 cốc")
            Integer simpleQuantity = extractQuantity(lower);
            if (simpleQuantity != null && result.productId != null) {
                result.quantity = simpleQuantity;
            }
        }

        return result;
    }

    private Integer extractQuantity(String message) {
        // Tìm số trong message (ví dụ: "2 cốc", "3", "hai")
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d+)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        // Tìm số bằng chữ (ví dụ: "một", "hai", "ba")
        String[] numberWords = {"một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười"};
        for (int i = 0; i < numberWords.length; i++) {
            if (message.contains(numberWords[i])) {
                return i + 1;
            }
        }
        return null;
    }

    private Integer extractTableNumber(String message) {
        // Tìm số bàn (ví dụ: "bàn 5", "table 3")
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:bàn|table)\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }

    private String extractAddress(String message) {
        // Tìm địa chỉ sau "địa chỉ" hoặc "address"
        String[] keywords = {"địa chỉ", "address", "giao đến", "ship đến"};
        for (String keyword : keywords) {
            int idx = message.toLowerCase().indexOf(keyword);
            if (idx >= 0) {
                String address = message.substring(idx + keyword.length()).trim();
                if (!address.isEmpty()) {
                    return address;
                }
            }
        }
        return null;
    }

    // Đoán địa chỉ khi người dùng nhập trực tiếp mà không có từ khóa
    private String guessAddress(String message) {
        if (message == null) return null;
        String trimmed = message.trim();
        if (trimmed.length() < 6) return null;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        boolean hasDigit = trimmed.matches(".*\\d+.*");
        boolean hasComma = trimmed.contains(",") || trimmed.contains(" - ");
        boolean hasAddrWord = lower.contains("đường") || lower.contains("phố") ||
                lower.contains("phường") || lower.contains("quận") ||
                lower.contains("thành phố") || lower.contains("tp") ||
                lower.contains("đà nẵng") || lower.contains("hà nội") ||
                lower.contains("hồ chí minh");
        if (hasDigit && (hasComma || hasAddrWord)) {
            return trimmed;
        }
        return null;
    }

    private Map<String, Object> findProductByName(String productName) {
        try {
            // 1) Thử tìm kiếm trực tiếp bằng search term của API trước (nhanh)
            Map<String, Object> productsDirect = productService.list(null, null, null, productName, 1, 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> directList = (List<Map<String, Object>>) productsDirect.get("data");
            if (directList != null && !directList.isEmpty()) {
                return directList.get(0);
            }

            // 2) Fallback: fuzzy search không dấu để khớp các tên như "tiramisu coffee" ~ "Cà phê Tiramisu"
            return findProductByNameFuzzy(productName);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private Map<String, Object> getProductById(Integer productId) {
        try {
            return productService.detail(productId);
        } catch (Exception e) {
            return null;
        }
    }

    private java.math.BigDecimal extractPrice(Map<String, Object> product) {
        if (product == null) return java.math.BigDecimal.ZERO;
        Object priceObj = product.get("price");
        if (priceObj == null) priceObj = product.get("base_price");
        if (priceObj == null) priceObj = product.get("unit_price");
        if (priceObj == null) return java.math.BigDecimal.ZERO;
        try {
            return new java.math.BigDecimal(priceObj.toString());
        } catch (NumberFormatException ex) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private String extractProductDisplayName(Map<String, Object> product) {
        if (product == null) return "sản phẩm này";
        Object nameObj = product.get("name");
        if (nameObj == null) nameObj = product.get("productName");
        if (nameObj == null) nameObj = product.get("product_name");
        if (nameObj == null) nameObj = product.get("title");
        return nameObj != null ? nameObj.toString() : "sản phẩm này";
    }

    // ==========================
    // Fuzzy matching helpers
    // ==========================
    private Map<String, Object> findProductByNameFuzzy(String rawInput) {
        try {
            String input = normalizeVietnamese(rawInput);
            if (input.isBlank()) return null;

            Map<String, Object> all = productService.list(null, null, null, null, 100, 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) all.get("data");
            if (list == null || list.isEmpty()) return null;

            double bestScore = 0.0;
            Map<String, Object> best = null;

            for (Map<String, Object> p : list) {
                String name = String.valueOf(p.get("name"));
                String norm = normalizeVietnamese(name);
                double score = similarityScore(input, norm);
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }

            // Ngưỡng tối thiểu để coi là match
            return bestScore >= 0.35 ? best : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeVietnamese(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT).trim();
        String decomposed = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD);
        String noDiacritics = decomposed.replaceAll("\\p{M}+", "");
        // Chuẩn hóa khoảng trắng và bỏ ký tự không chữ/số cơ bản (giữ khoảng trắng)
        String cleaned = noDiacritics.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private double similarityScore(String a, String b) {
        if (a.equals(b)) return 1.0;
        // Ưu tiên chứa trọn từ
        if (b.contains(a)) return Math.min(1.0, 0.9 * a.length() / Math.max(1, b.length()));
        if (a.contains(b)) return Math.min(1.0, 0.9 * b.length() / Math.max(1, a.length()));

        // Token overlap
        Set<String> at = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> bt = new HashSet<>(Arrays.asList(b.split("\\s+")));
        at.removeIf(String::isBlank);
        bt.removeIf(String::isBlank);
        if (!at.isEmpty() && !bt.isEmpty()) {
            int intersect = 0;
            for (String t : at) if (bt.contains(t)) intersect++;
            double overlap = (double) intersect / Math.max(at.size(), bt.size());
            // Kết hợp với Levenshtein
            double lev = 1.0 - ((double) levenshtein(a, b) / Math.max(a.length(), b.length()));
            return Math.max(overlap, lev * 0.8 + overlap * 0.2);
        }

        // Levenshtein fallback
        return 1.0 - ((double) levenshtein(a, b) / Math.max(Math.max(a.length(), b.length()), 1));
    }

    private int levenshtein(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];
        for (int j = 0; j <= len2; j++) prev[j] = j;
        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            char c1 = s1.charAt(i - 1);
            for (int j = 1; j <= len2; j++) {
                int cost = (c1 == s2.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[len2];
    }

    private ChatResponse showMenu() {
        Map<String, Object> products = productService.list(null, null, null, null, 50, 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productList = (List<Map<String, Object>>) products.get("data");
        return showProductList(productList, "📋 Menu sản phẩm:\n\n");
    }

    private ChatResponse showProductList(List<Map<String, Object>> productList, String header) {
        StringBuilder response = new StringBuilder(header);
        for (Map<String, Object> p : productList) {
            java.math.BigDecimal price = extractPrice(p);
            response.append(String.format("• %s - %s VNĐ (Còn: %s)\n",
                    extractProductDisplayName(p),
                    formatPrice(price),
                    p.get("stock") != null ? p.get("stock") : 0));
        }
        response.append("\nBạn muốn đặt món nào? 😊");

        return ChatResponse.builder()
                .message(response.toString())
                .intent("order")
                .data(productList)
                .suggestions(List.of(
                        ChatMessage.builder().role("assistant").content("Xem menu").build(),
                        ChatMessage.builder().role("assistant").content("Đặt hàng").build()
                ))
                .build();
    }

    private ChatResponse askQuantity(Map<String, Object> product) {
        String productName = extractProductDisplayName(product);
        String message = String.format("Bạn muốn đặt bao nhiêu %s? 😊\n\n" +
                "Ví dụ: \"2 cốc\" hoặc \"3\"", productName);

        Map<String, Object> context = new HashMap<>();
        context.put("productId", product.get("id"));
        context.put("productName", productName);

        return ChatResponse.builder()
                .message(message)
                .intent("order")
                .orderState("ASKING_QUANTITY")
                .orderContext(context)
                .data(product)
                .suggestions(List.of(
                        ChatMessage.builder().role("assistant").content("1 cốc").build(),
                        ChatMessage.builder().role("assistant").content("2 cốc").build(),
                        ChatMessage.builder().role("assistant").content("3 cốc").build()
                ))
                .build();
    }

    private ChatResponse askDeliveryType(Map<String, Object> product, Integer quantity) {
        String productName = extractProductDisplayName(product);
        String message = String.format("Bạn muốn uống tại quán hay ship đi cho món %s x%d? 🚚\n\n" +
                "• Tại quán: Chọn số bàn\n" +
                "• Ship đi: Mình sẽ hướng dẫn nhập địa chỉ & số điện thoại ở bước thanh toán", productName, quantity != null ? quantity : 1);

        Map<String, Object> context = new HashMap<>();
        context.put("productId", product.get("id"));
        context.put("productName", productName);
        context.put("quantity", quantity);

        return ChatResponse.builder()
                .message(message)
                .intent("order")
                .orderState("ASKING_DELIVERY_TYPE")
                .orderContext(context)
                .data(product)
                .suggestions(List.of(
                        ChatMessage.builder().role("assistant").content("Tại quán - Bàn 1").build(),
                        ChatMessage.builder().role("assistant").content("Tại quán - Bàn 2").build(),
                        ChatMessage.builder().role("assistant").content("Ship đi").build()
                ))
                .build();
    }

    private ChatResponse createOrderFromParseResult(OrderParseResult parseResult, Integer userId) {
        try {
            // Kiểm tra thông tin còn thiếu
            if (parseResult.deliveryType == null) {
                Map<String, Object> product = getProductById(parseResult.productId);
                if (product == null) {
                    return ChatResponse.builder()
                            .message("Xin lỗi, không tìm thấy sản phẩm.")
                            .intent("order")
                            .build();
                }
                return askDeliveryType(product, parseResult.quantity);
            }

            // Kiểm tra thông tin còn thiếu dựa trên delivery type
            if ("dine_in".equals(parseResult.deliveryType) && parseResult.tableNumber == null) {
                // Hỏi số bàn
                Map<String, Object> product = getProductById(parseResult.productId);
                if (product == null) {
                    return ChatResponse.builder()
                            .message("Xin lỗi, không tìm thấy sản phẩm.")
                            .intent("order")
                            .build();
                }
                return askTableNumber(product, parseResult.quantity);
            }

            if ("delivery".equals(parseResult.deliveryType)) {
                // Với flow giao hàng qua chatbot: thêm sản phẩm vào giỏ và nhắc người dùng nhập thông tin ở trang thanh toán
                Map<String, Object> product = getProductById(parseResult.productId);
                java.math.BigDecimal price = extractPrice(product);
                int qty = parseResult.quantity != null ? parseResult.quantity : 1;
                String productName = parseResult.productName != null ? parseResult.productName :
                        extractProductDisplayName(product);
                String message = String.format("✅ Đã thêm vào giỏ hàng:\n\n" +
                                "📦 %s x%d\n" +
                                "💰 Tạm tính: %s VNĐ\n\n" +
                                "💡 Vui lòng nhập địa chỉ giao hàng và số điện thoại ở trang giỏ hàng trước khi thanh toán nhé.",
                        parseResult.productName != null ? parseResult.productName : "Sản phẩm",
                        qty,
                        formatPrice(price.multiply(java.math.BigDecimal.valueOf(qty))));

                Map<String, Object> orderData = new HashMap<>();
                orderData.put("productId", parseResult.productId);
                orderData.put("productName", productName);
                orderData.put("quantity", qty);
                orderData.put("price", price.doubleValue());
                orderData.put("subtotal", price.multiply(java.math.BigDecimal.valueOf(qty)).doubleValue());
                if (product != null && product.get("img") != null) {
                    orderData.put("img", product.get("img"));
                }

                return ChatResponse.builder()
                        .message(message)
                        .intent("order")
                        .orderCreated(true) // dùng để trigger FE add-to-cart + redirect
                        .orderId(null)
                        .orderData(orderData)
                        .redirectTo("/cart")
                        .build();
            }

            // Tạo đơn hàng
            if ("dine_in".equals(parseResult.deliveryType)) {
                // Tạo guest table order
                return createGuestTableOrder(parseResult);
            } else {
                // Tạo delivery order (cần userId và address)
                // Note: Cần User object để tạo transaction, nhưng hiện tại chỉ có userId
                // Sẽ cần refactor để lấy User object hoặc tạo method mới
                return ChatResponse.builder()
                        .message("Tính năng ship hàng đang được phát triển. Vui lòng đặt hàng tại quán hoặc qua trang web.")
                        .intent("order")
                        .suggestions(List.of(
                                ChatMessage.builder().role("assistant").content("Tại quán - Bàn 1").build(),
                                ChatMessage.builder().role("assistant").content("Xem menu").build()
                        ))
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi tạo đơn hàng. Vui lòng thử lại.")
                    .intent("order")
                    .build();
        }
    }

    private ChatResponse askTableNumber(Map<String, Object> product, Integer quantity) {
        String productName = extractProductDisplayName(product);
        String message = String.format("Bạn đang ngồi ở bàn số mấy? 🪑\n\n" +
                "Ví dụ: \"Bàn 1\" hoặc \"Bàn 5\"");

        Map<String, Object> context = new HashMap<>();
        context.put("productId", product.get("id"));
        context.put("productName", productName);
        context.put("quantity", quantity);
        context.put("deliveryType", "dine_in");

        return ChatResponse.builder()
                .message(message)
                .intent("order")
                .orderState("ASKING_TABLE")
                .orderContext(context)
                .data(product)
                .suggestions(List.of(
                        ChatMessage.builder().role("assistant").content("Bàn 1").build(),
                        ChatMessage.builder().role("assistant").content("Bàn 2").build(),
                        ChatMessage.builder().role("assistant").content("Bàn 3").build()
                ))
                .build();
    }

    private ChatResponse askAddress(Map<String, Object> product, Integer quantity) {
        String productName = extractProductDisplayName(product);
        String message = String.format("Vui lòng cung cấp địa chỉ giao hàng 📍\n\n" +
                "Ví dụ: \"123 đường ABC, Quận XYZ, Đà Nẵng\"");

        Map<String, Object> context = new HashMap<>();
        context.put("productId", product.get("id"));
        context.put("productName", productName);
        context.put("quantity", quantity);
        context.put("deliveryType", "delivery");

        return ChatResponse.builder()
                .message(message)
                .intent("order")
                .orderState("ASKING_ADDRESS")
                .orderContext(context)
                .data(product)
                .build();
    }

    private ChatResponse createGuestTableOrder(OrderParseResult parseResult) {
        try {
            // Tạo guest table order
            com.kopi.kopi.controller.GuestOrderController.GuestOrderItem item =
                new com.kopi.kopi.controller.GuestOrderController.GuestOrderItem(
                    parseResult.productId,
                    parseResult.quantity != null ? parseResult.quantity : 1
                );

            com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest request =
                new com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest(
                    null, // qr_token
                    parseResult.tableNumber, // table_number
                    List.of(item), // products
                    "Đặt hàng qua chatbot", // notes
                    1, // payment_id (CASH)
                    false // paid
                );

            ResponseEntity<?> response = orderService.createGuestTableOrder(request);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                Integer orderId = (Integer) data.get("id");
                Integer tableNumber = (Integer) data.get("table_number");

                // Lấy thông tin sản phẩm để tính tổng tiền
                Map<String, Object> product = getProductById(parseResult.productId);
                java.math.BigDecimal price = extractPrice(product);
                int qty = parseResult.quantity != null ? parseResult.quantity : 1;
                java.math.BigDecimal total = price.multiply(java.math.BigDecimal.valueOf(qty));

                String message = String.format("✅ Đơn hàng đã được tạo thành công!\n\n" +
                        "📦 Món: %s x%d\n" +
                        "🪑 Bàn: %d\n" +
                        "💰 Tổng tiền: %s VNĐ\n\n" +
                        "Vui lòng thanh toán tại quầy. Cảm ơn bạn! 😊",
                        parseResult.productName != null ? parseResult.productName : "Sản phẩm",
                        qty,
                        tableNumber != null ? tableNumber : parseResult.tableNumber,
                        formatPrice(total));

                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderId", orderId);
                orderData.put("tableNumber", tableNumber);
                String productName = parseResult.productName != null ? parseResult.productName :
                        extractProductDisplayName(product);
                orderData.put("productName", productName);
                orderData.put("quantity", parseResult.quantity);
                orderData.put("productId", parseResult.productId);
                orderData.put("price", price.doubleValue());
                orderData.put("subtotal", total.doubleValue());
                if (product != null && product.get("img") != null) {
                    orderData.put("img", product.get("img"));
                }

                return ChatResponse.builder()
                        .message(message)
                        .intent("order")
                        .orderCreated(true)
                        .orderId(orderId)
                        .orderData(orderData)
                        .redirectTo("/cart") // Điều hướng tới giỏ hàng để thanh toán/ chỉnh sửa
                        .build();
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorBody = (Map<String, Object>) response.getBody();
                String errorMessage = (String) errorBody.getOrDefault("message", "Có lỗi khi tạo đơn hàng");
                return ChatResponse.builder()
                        .message("Xin lỗi, " + errorMessage)
                        .intent("order")
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi tạo đơn hàng: " + e.getMessage())
                    .intent("order")
                    .build();
        }
    }

    private ChatResponse handleRevenueIntent(String message) {
        try {
            // Phân tích yêu cầu: ngày/tháng/năm
            String lowerMessage = message.toLowerCase();
            ReportService.Granularity granularity = ReportService.Granularity.monthly;
            LocalDate from = null;
            LocalDate to = LocalDate.now();

            // Xử lý "hôm nay" (today)
            if (lowerMessage.contains("hôm nay") || lowerMessage.contains("today")) {
                granularity = ReportService.Granularity.daily;
                from = to; // Chỉ lấy dữ liệu hôm nay
            }
            // Xử lý "hôm qua" (yesterday)
            else if (lowerMessage.contains("hôm qua") || lowerMessage.contains("yesterday")) {
                granularity = ReportService.Granularity.daily;
                from = to.minusDays(1);
                to = to.minusDays(1);
            }
            // Xử lý theo ngày (last 7 days)
            else if (lowerMessage.contains("ngày") || lowerMessage.contains("day")) {
                granularity = ReportService.Granularity.daily;
                from = to.minusDays(6);
            }
            // Xử lý theo tháng (last 6 months)
            else if (lowerMessage.contains("tháng") || lowerMessage.contains("month")) {
                granularity = ReportService.Granularity.monthly;
                from = to.minusMonths(5);
            }
            // Xử lý theo năm (last 6 years)
            else if (lowerMessage.contains("năm") || lowerMessage.contains("year")) {
                granularity = ReportService.Granularity.yearly;
                from = to.minusYears(5);
            }
            // Default: hôm nay nếu không có từ khóa rõ ràng
            else {
                granularity = ReportService.Granularity.daily;
                from = to;
            }

            List<com.kopi.kopi.dto.RevenuePoint> revenueData = reportService.revenue(granularity, from, to, 10);

            if (revenueData != null && !revenueData.isEmpty()) {
                // Xác định tiêu đề dựa trên khoảng thời gian
                String title = "📊 Báo cáo doanh thu";
                if (lowerMessage.contains("hôm nay") || lowerMessage.contains("today")) {
                    title = "📊 Doanh thu hôm nay";
                } else if (lowerMessage.contains("hôm qua") || lowerMessage.contains("yesterday")) {
                    title = "📊 Doanh thu hôm qua";
                } else if (lowerMessage.contains("ngày") || lowerMessage.contains("day")) {
                    title = "📊 Doanh thu theo ngày (7 ngày gần nhất)";
                } else if (lowerMessage.contains("tháng") || lowerMessage.contains("month")) {
                    title = "📊 Doanh thu theo tháng (6 tháng gần nhất)";
                } else if (lowerMessage.contains("năm") || lowerMessage.contains("year")) {
                    title = "📊 Doanh thu theo năm (6 năm gần nhất)";
                }

                StringBuilder response = new StringBuilder(title + ":\n\n");
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

    // STAFF/EMPLOYEE: khóa phạm vi ở HÔM NAY, luôn granularity=daily
    private ChatResponse handleRevenueIntentForStaff(String message) {
        try {
            LocalDate today = LocalDate.now();
            List<com.kopi.kopi.dto.RevenuePoint> revenueData =
                    reportService.revenue(ReportService.Granularity.daily, today, today, 1);

            if (revenueData != null && !revenueData.isEmpty()) {
                StringBuilder response = new StringBuilder("📊 Doanh thu hôm nay:\n\n");
                double total = 0;
                int totalOrders = 0;

                for (com.kopi.kopi.dto.RevenuePoint point : revenueData) {
                    response.append(String.format("• %s: %s VNĐ (%d đơn hàng)\n",
                            point.getLabel(),
                            formatPrice(point.getTotal_sum()),
                            point.getOrderCount()));
                    if (point.getTotal_sum() != null) total += point.getTotal_sum().doubleValue();
                    totalOrders += point.getOrderCount();
                }

                response.append(String.format("\n📈 Tổng cộng: %s VNĐ (%d đơn hàng)",
                        formatPrice(total), totalOrders));

                return ChatResponse.builder()
                        .message(response.toString())
                        .intent("revenue")
                        .data(revenueData)
                        .suggestions(List.of(
                                ChatMessage.builder().role("assistant").content("Xem doanh thu hôm nay").build(),
                                ChatMessage.builder().role("assistant").content("Kiểm tra tồn kho").build()
                        ))
                        .build();
            }

            return ChatResponse.builder()
                    .message("Không có dữ liệu doanh thu cho hôm nay.")
                    .intent("revenue")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi lấy doanh thu hôm nay.")
                    .intent("revenue")
                    .build();
        }
    }

    private ChatResponse handleInventoryIntent(String message) {
        try {
            String lower = message.toLowerCase().trim();
            boolean showLowStockOnly = lower.contains("sắp hết") || lower.contains("hết hàng") ||
                                      lower.contains("low stock") || lower.contains("ít");

            // Lấy tất cả sản phẩm (không filter theo search term để hiển thị đầy đủ)
            Map<String, Object> products = productService.list(null, null, null, null, 100, 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productList = (List<Map<String, Object>>) products.get("data");

            if (productList != null && !productList.isEmpty()) {
                StringBuilder response = new StringBuilder();
                int lowStockCount = 0;
                int totalProducts = 0;

                // Lọc sản phẩm sắp hết hàng nếu cần
                List<Map<String, Object>> filteredList = new ArrayList<>();
                for (Map<String, Object> p : productList) {
                    Integer stock = (Integer) p.get("stock");
                    if (stock != null && stock < 10) {
                        lowStockCount++;
                    }
                    totalProducts++;

                    if (!showLowStockOnly || (stock != null && stock < 10)) {
                        filteredList.add(p);
                    }
                }

                if (showLowStockOnly) {
                    response.append("⚠️ Danh sách sản phẩm sắp hết hàng (< 10):\n\n");
                } else {
                    response.append("📦 Danh sách tồn kho sản phẩm:\n\n");
                }

                if (filteredList.isEmpty()) {
                    response.append("✅ Không có sản phẩm nào sắp hết hàng. Tất cả sản phẩm đều đủ số lượng!\n");
                } else {
                    for (Map<String, Object> p : filteredList) {
                        Integer stock = (Integer) p.get("stock");
                        String stockStatus = stock != null && stock < 10 ? "⚠️" : "✅";

                        response.append(String.format("%s %s - Còn: %d sản phẩm\n",
                                stockStatus,
                                p.get("name"),
                                stock != null ? stock : 0));
                    }
                }

                if (!showLowStockOnly && lowStockCount > 0) {
                    response.append(String.format("\n⚠️ Cảnh báo: %d/%d sản phẩm sắp hết hàng (< 10)",
                            lowStockCount, totalProducts));
                }

                if (showLowStockOnly && lowStockCount == 0) {
                    response.append("\n✅ Tất cả sản phẩm đều đủ số lượng!");
                }

                return ChatResponse.builder()
                        .message(response.toString())
                        .intent("inventory")
                        .data(filteredList)
                        .suggestions(List.of(
                                ChatMessage.builder().role("assistant").content("Xem sản phẩm sắp hết hàng").build(),
                                ChatMessage.builder().role("assistant").content("Xem tất cả sản phẩm").build(),
                                ChatMessage.builder().role("assistant").content("Kiểm tra tồn kho").build()
                        ))
                        .build();
            } else {
                return ChatResponse.builder()
                        .message("Không tìm thấy sản phẩm nào trong kho.")
                        .intent("inventory")
                        .suggestions(getDefaultSuggestions("ADMIN"))
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ChatResponse.builder()
                    .message("Xin lỗi, có lỗi khi kiểm tra tồn kho. Vui lòng thử lại.")
                    .intent("inventory")
                    .suggestions(getDefaultSuggestions("ADMIN"))
                    .build();
        }
    }

    private ChatResponse handleGeneralIntent(String message, String userRole) {
        String lower = message.toLowerCase().trim();

        // Xử lý các câu chào hỏi thông thường
        if (lower.contains("xin chào") || lower.contains("hello") || lower.contains("hi") ||
            lower.contains("chào") || lower.contains("hey")) {
            return ChatResponse.builder()
                    .message("Xin chào! 👋 Tôi là trợ lý ảo của Kopi Coffee & Workspace. Tôi có thể giúp bạn:\n\n" +
                            "✨ Xem và đặt hàng sản phẩm\n" +
                            "📋 Xem danh sách sản phẩm\n" +
                            (userRole.equals("ADMIN") ? "📊 Kiểm tra tồn kho\n💰 Xem báo cáo doanh thu\n" : "") +
                            "💬 Trả lời các câu hỏi\n\n" +
                            "Bạn cần tôi giúp gì hôm nay? 😊")
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        }

        // Xử lý câu hỏi về giờ mở cửa
        if (lower.contains("giờ") && (lower.contains("mở") || lower.contains("đóng") || lower.contains("hoạt động"))) {
            return ChatResponse.builder()
                    .message("⏰ Kopi Coffee & Workspace mở cửa:\n\n" +
                            "🕐 Thứ 2 - Chủ nhật: 7:00 - 22:00\n\n" +
                            "Bạn có muốn đặt chỗ trước không? 😊")
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        }

        // Xử lý câu hỏi về địa chỉ
        if (lower.contains("địa chỉ") || lower.contains("ở đâu") || lower.contains("location") ||
            lower.contains("address")) {
            return ChatResponse.builder()
                    .message("📍 Địa chỉ của chúng tôi:\n\n" +
                            "🏪 Kopi Coffee & Workspace\n" +
                            "38 đường Phạm Văn Đồng, An Hải Bắc, Sơn Trà, Đà Nẵng 550000\n\n" +
                            "Bạn có thể xem bản đồ trên trang web hoặc đặt hàng online nhé! 😊")
                    .intent("general")
                    .suggestions(getDefaultSuggestions(userRole))
                    .build();
        }

        // Sử dụng Gemini để trả lời câu hỏi chung (nếu có API key)
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String prompt = String.format("""
                    Bạn là trợ lý ảo thân thiện của quán cà phê Kopi Coffee & Workspace. 
                    Trả lời câu hỏi của khách hàng một cách tự nhiên, thân thiện, ngắn gọn (dưới 150 từ).
                    
                    Vai trò người dùng: %s
                    Câu hỏi: %s
                    
                    Trả lời bằng tiếng Việt, tự nhiên như đang trò chuyện. Nếu không chắc chắn, hãy đề xuất các tính năng có thể giúp.
                    """, userRole, message);

                String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent?key=" + apiKey;

                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of("temperature", 0.8)
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        url, new HttpEntity<>(body, headers), String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    if (!root.has("error")) {
                        String reply = root.path("candidates").path(0).path("content").path(0).path("parts")
                                .path(0).path("text").asText("");

                        if (!reply.isEmpty()) {
                            return ChatResponse.builder()
                                    .message(reply)
                                    .intent("general")
                                    .suggestions(getDefaultSuggestions(userRole))
                                    .build();
                        }
                    }
                }
            } catch (Exception e) {
                // Nếu Gemini fail, tiếp tục với fallback
            }
        }

        // Fallback: Trả lời thân thiện và đề xuất
        return ChatResponse.builder()
                .message("Tôi hiểu bạn đang hỏi về \"" + message + "\". " +
                        "Hiện tại tôi có thể giúp bạn:\n\n" +
                        "✨ Xem và đặt hàng sản phẩm\n" +
                        "📋 Xem danh sách sản phẩm\n" +
                        (userRole.equals("ADMIN") ? "📊 Kiểm tra tồn kho\n💰 Xem báo cáo doanh thu\n" : "") +
                        "💬 Trả lời các câu hỏi\n\n" +
                        "Bạn muốn tôi giúp gì cụ thể hơn không? 😊")
                .intent("general")
                .suggestions(getDefaultSuggestions(userRole))
                .build();
    }

    private String extractProductNameFromMessage(String message) {
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
        if (lower.contains("hôm nay") || lower.contains("today")) return "hôm nay";
        if (lower.contains("hôm qua") || lower.contains("yesterday")) return "hôm qua";
        if (lower.contains("ngày") || lower.contains("day")) return "ngày";
        if (lower.contains("tháng") || lower.contains("month")) return "tháng";
        if (lower.contains("năm") || lower.contains("year")) return "năm";
        return "hôm nay"; // default to today
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

