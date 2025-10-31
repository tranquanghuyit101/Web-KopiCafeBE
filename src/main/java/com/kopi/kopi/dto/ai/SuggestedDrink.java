package com.kopi.kopi.dto.ai;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class SuggestedDrink {
    private String name;            // ví dụ: "Salted Caramel Latte"
    private String reason;          // vì sao nên bán
    private double score;           // 0..100
    private List<String> tags;      // ["caramel","latte","trend"]
    private String basicRecipe;     // công thức cơ bản tóm tắt
    private List<VideoItem> fromVideos; // các video nguồn
}
