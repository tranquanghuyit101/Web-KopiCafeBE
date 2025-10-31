package com.kopi.kopi.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class DishExtraction {
    private String id;          // videoId
    private String dishName;    // tên món cụ thể (có thể null nếu không xác định)
    private String basicRecipe; // công thức cơ bản
    private double confidence;  // 0..1
}
