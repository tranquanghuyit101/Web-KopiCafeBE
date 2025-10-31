package com.kopi.kopi.dto.ai;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DishTrendsResponse {
    private List<DishGroup> data;
    private Map<String, Object> meta; // ví dụ: { "days":7, "region":"VN", ... }
}
