package com.kopi.kopi.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String intent; // "order", "revenue", "inventory", "general"
    private Object data; // Dữ liệu bổ sung (danh sách sản phẩm, báo cáo doanh thu, etc.)
    private List<ChatMessage> suggestions; // Gợi ý câu hỏi tiếp theo
}

