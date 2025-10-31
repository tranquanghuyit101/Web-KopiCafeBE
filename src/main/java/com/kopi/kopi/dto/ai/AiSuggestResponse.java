package com.kopi.kopi.dto.ai;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AiSuggestResponse {
    private List<SuggestedDrink> items;
    private String model;        // model Gemini sử dụng
    private long tookMs;         // thời gian xử lý
    private int videoFetched;    // số video lấy từ YouTube
}
