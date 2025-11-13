package com.kopi.kopi.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private List<ChatMessage> history; // Lịch sử chat để context
    private java.util.Map<String, Object> orderContext; // Context bước đặt hàng (từ assistant)
    private String orderState; // Trạng thái flow đặt hàng (ASKING_QUANTITY/DELIVERY/...)
}

