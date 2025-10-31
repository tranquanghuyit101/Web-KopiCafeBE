package com.kopi.kopi.dto.ai;

import lombok.*;
import java.time.OffsetDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VideoItem {
    // Thông tin cơ bản từ YouTube
    private String videoId;         // ID video
    private String title;           // tiêu đề
    private String description;     // mô tả
    private String thumbnailUrl;    // ảnh thumbnail
    private String channelTitle;    // kênh
    private OffsetDateTime publishedAt;
    private String videoUrl;        // https://www.youtube.com/watch?v=... hoặc /shorts/...
    private Long viewCount;
    private Long likeCount;
    private Long durationSeconds;   // tổng thời lượng giây (để nhận diện Shorts)

    // Trường tính toán/phân tích
    private Double viralScore;  // công thức tính ở service
    private String dishName;    // tên món đã chuẩn hoá (LLM/heuristic)
    private String dishKey;     // slug của dishName (khóa gom nhóm)
    private String basicRecipe; // công thức cơ bản từ Gemini
}
