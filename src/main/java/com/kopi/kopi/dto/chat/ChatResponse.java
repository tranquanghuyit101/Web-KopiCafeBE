package com.kopi.kopi.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String intent; // "order", "revenue", "inventory", "general"
    private Object data; // Dữ liệu bổ sung (danh sách sản phẩm, báo cáo doanh thu, etc.)
    private List<ChatMessage> suggestions; // Gợi ý câu hỏi tiếp theo

    // Fields for order creation flow
    private Boolean orderCreated; // true if order was successfully created
    private Integer orderId; // ID of created order
    private String redirectTo; // URL/route to redirect after order creation
    private Map<String, Object> orderData; // Order details (product info, delivery type, etc.)

    // Fields for order flow state
    private String orderState; // "SELECTING_PRODUCT", "ASKING_QUANTITY", "ASKING_DELIVERY_TYPE", "ASKING_TABLE", "ASKING_ADDRESS", "CREATING_ORDER"
    private Map<String, Object> orderContext; // Temporary data for order flow (productId, quantity, deliveryType, etc.)
}

