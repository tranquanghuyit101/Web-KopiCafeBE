package com.kopi.kopi.dto.ai;

import lombok.Data;

@Data
public class AiSuggestionRequest {
    // Đơn giản: chỉ cần số ngày và số video tối đa
    private Integer days;            // số ngày gần đây, default 30
    private Integer maxResults;      // số video tối đa, default 50
    private Boolean shortsOnly;      // chỉ lấy Shorts (<=60s). Default: true
}
