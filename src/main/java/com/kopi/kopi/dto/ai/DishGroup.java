package com.kopi.kopi.dto.ai;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DishGroup {
    private String key;          // slug
    private String name;         // display
    private String basicRecipe;  // công thức cơ bản
    private int totalVideos;
    private double topScore;     // max viralScore trong nhóm
    private List<VideoItem> videos; // đã sort viralScore desc
}
