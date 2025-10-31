package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.ai.AiSuggestResponse;
import com.kopi.kopi.dto.ai.AiSuggestionRequest;
import com.kopi.kopi.dto.ai.DishExtraction;
import com.kopi.kopi.dto.ai.DishGroup;
import com.kopi.kopi.dto.ai.DishTrendsResponse;
import com.kopi.kopi.dto.ai.VideoItem;
import com.kopi.kopi.service.IAiSuggestService;
import com.kopi.kopi.service.ai.GeminiClient;
import com.kopi.kopi.service.ai.YouTubeClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiSuggestServiceImpl implements IAiSuggestService {
    private final YouTubeClient yt;
    private final GeminiClient gemini;

    @Value("${ai.youtube.regionCode:VN}")
    private String defaultRegion;

    public AiSuggestServiceImpl(YouTubeClient yt, GeminiClient gemini) {
        this.yt = yt;
        this.gemini = gemini;
    }

    @Override
    public AiSuggestResponse search(AiSuggestionRequest req) {
        // Đơn giản: mặc định tìm hot trend drink Việt Nam
        DishTrendsResponse trends = groupedByDish(req);
        
        // Chuyển DishGroup sang SuggestedDrink để tương thích response cũ
        List<com.kopi.kopi.dto.ai.SuggestedDrink> items = new ArrayList<>();
        for (DishGroup g : trends.getData()) {
            items.add(com.kopi.kopi.dto.ai.SuggestedDrink.builder()
                    .name(g.getName())
                    .score(g.getTopScore())
                    .reason("Trending drink with " + g.getTotalVideos() + " videos")
                    .tags(List.of("hot-trend", "vietnam"))
                    .basicRecipe("")
                    .fromVideos(g.getVideos())
                    .build());
        }

        return AiSuggestResponse.builder()
                .items(items)
                .model("trend-aggregation")
                .tookMs((Long)trends.getMeta().getOrDefault("tookMs", 0L))
                .videoFetched((Integer)trends.getMeta().getOrDefault("totalVideos", 0))
                .build();
    }

    // Phục vụ TrendsController: gom nhóm video theo món đồ uống
    // LUỒNG MỚI: Gemini tìm món hot trend → YouTube search video cho từng món
    public DishTrendsResponse groupedByDish(AiSuggestionRequest req) {
        long t0 = System.currentTimeMillis();
        int days  = Optional.ofNullable(req.getDays()).orElse(30);
        int max   = Math.min(Optional.ofNullable(req.getMaxResults()).orElse(100), 200);
        boolean shortsOnly = false;
        
        System.out.println("[AiSuggest] groupedByDish called: days=" + days + ", max=" + max + ", shortsOnly=" + shortsOnly);

        // BƯỚC 1: Gọi Gemini để tìm các món hot trend ở Việt Nam
        System.out.println("[AiSuggest] Step 1: Calling Gemini to find hot trend dishes in Vietnam...");
        List<GeminiClient.TrendingDishInfo> trendingDishes;
        try {
            // Tăng số lượng món yêu cầu (để có nhiều lựa chọn hơn)
            int dishCount = Math.max(10, max / 3);  // Tăng từ max/10 lên max/3, min 10 món
            System.out.println("[AiSuggest] Requesting " + dishCount + " trending dishes from Gemini...");
            trendingDishes = gemini.findHotTrendDishes(days, dishCount);
            System.out.println("[AiSuggest] Gemini returned " + trendingDishes.size() + " trending dishes");
            for (GeminiClient.TrendingDishInfo dish : trendingDishes) {
                System.out.println("  - " + dish.getName() + " (trending score: " + dish.getTrendingScore() + ")");
            }
        } catch (Exception e) {
            System.err.println("[AiSuggest] Gemini failed: " + e.getMessage() + ". Using fallback dishes...");
            trendingDishes = gemini.getFallbackTrendingDishes(Math.max(5, max / 10));
        }

        if (trendingDishes.isEmpty()) {
            System.err.println("[AiSuggest] No trending dishes found, returning empty result");
            return DishTrendsResponse.builder()
                    .data(List.of())
                    .meta(Map.of("days", days, "max", max, "totalVideos", 0, "grouped", 0, "tookMs", System.currentTimeMillis() - t0))
                    .build();
        }

        // BƯỚC 2: Với mỗi món, tìm video GẦN ĐÂY trên YouTube
        System.out.println("[AiSuggest] Step 2: Searching RECENT YouTube videos for each dish...");
        Map<String, List<VideoItem>> dishToVideos = new LinkedHashMap<>();
        int videosPerDish = Math.max(3, max / trendingDishes.size()); // Ít nhất 3 video/món
        
        // Giảm xuống 60 ngày để có nhiều video hơn (90 ngày quá khắt)
        int recentDays = Math.max(days, 60);
        System.out.println("[AiSuggest] Will search for videos published within last " + recentDays + " days");

        for (GeminiClient.TrendingDishInfo dishInfo : trendingDishes) {
            String dishName = dishInfo.getName();
            System.out.println("[AiSuggest] Searching videos for: " + dishName);
            
            // Tạo query tìm kiếm đơn giản (chỉ tên món)
            String query = dishName;
            
            try {
                // Thử tìm với region=VN và THỜI GIAN GẦN ĐÂY trước
                List<VideoItem> videos = yt.searchRecentVideos(
                        query, recentDays, videosPerDish, "VN", "vi", shortsOnly, null, "moderate", false
                );
                
                System.out.println("[AiSuggest]   -> Found " + videos.size() + " videos (VN, last " + recentDays + " days) for " + dishName);
                
                // Nếu không có kết quả, thử bỏ region nhưng VẪN GIỮ time filter
                if (videos.isEmpty()) {
                    System.out.println("[AiSuggest]   -> Retry without region but keep time filter...");
                    videos = yt.searchRecentVideos(
                            query, recentDays, videosPerDish, "", "", shortsOnly, null, "moderate", false
                    );
                    System.out.println("[AiSuggest]   -> Found " + videos.size() + " videos (global, last " + recentDays + " days) for " + dishName);
                }
                
                // Nếu vẫn rỗng, thử với relevance order (nhưng vẫn giữ time)
                if (videos.isEmpty()) {
                    System.out.println("[AiSuggest]   -> Retry with relevance order...");
                    videos = yt.searchVideos(
                            query, recentDays, videosPerDish, "", "", shortsOnly, null, "moderate", false, "relevance"
                    );
                    System.out.println("[AiSuggest]   -> Found " + videos.size() + " videos (relevance, last " + recentDays + " days) for " + dishName);
                }
                
                // CUỐI CÙNG: Nếu vẫn rỗng, thử nới lỏng time filter (120 ngày)
                if (videos.isEmpty()) {
                    System.out.println("[AiSuggest]   -> Last retry with extended time (120 days)...");
                    videos = yt.searchVideos(
                            query, 120, videosPerDish, "", "", false, null, "moderate", false, "relevance"
                    );
                    System.out.println("[AiSuggest]   -> Found " + videos.size() + " videos (120 days, no shorts filter) for " + dishName);
                }
                
                // Lọc video không liên quan
                videos = filterRelevantVideos(videos, dishName);
                System.out.println("[AiSuggest]   -> After filter: " + videos.size() + " videos");
                
                // Tính viral score cho từng video
                OffsetDateTime now = OffsetDateTime.now();
                for (VideoItem v : videos) {
                    double view = Math.max(1, Optional.ofNullable(v.getViewCount()).orElse(0L));
                    double like = Math.max(1, Optional.ofNullable(v.getLikeCount()).orElse(0L));
                    double base = Math.log(view) + 2.0 * Math.log(like);
                    long hours = Math.max(1, Duration.between(
                            Optional.ofNullable(v.getPublishedAt()).orElse(now.minusDays(days)), now
                    ).toHours());
                    double recency = 48.0 / hours;
                    v.setViralScore(base + recency);
                    
                // Gán tên món và công thức cho video
                v.setDishName(dishName);
                v.setDishKey(keyOf(dishName));
                v.setBasicRecipe(dishInfo.getBasicRecipe());
            }
            
            // LUÔN thêm món, kể cả khi không có video (theo yêu cầu user)
            dishToVideos.put(dishName, videos);
            if (videos.isEmpty()) {
                System.out.println("[AiSuggest]   ⚠️  Added dish '" + dishName + "' with NO videos (will show as 'Không có video cụ thể')");
            } else {
                System.out.println("[AiSuggest]   ✅ Added dish '" + dishName + "' with " + videos.size() + " videos");
            }
                
            } catch (Exception e) {
                System.err.println("[AiSuggest] Failed to search videos for " + dishName + ": " + e.getMessage());
            }
        }
        
        System.out.println("[AiSuggest] Total dishes with videos: " + dishToVideos.size() + "/" + trendingDishes.size());

        // BƯỚC 3: Gom nhóm và sắp xếp
        System.out.println("[AiSuggest] Step 3: Grouping and ranking dishes...");
        List<DishGroup> groups = new ArrayList<>();
        
        for (Map.Entry<String, List<VideoItem>> entry : dishToVideos.entrySet()) {
            String dishName = entry.getKey();
            List<VideoItem> videos = entry.getValue();
            
            double topScore = 0.0;
            double avgScore = 0.0;
            String recipe = "";
            
            if (!videos.isEmpty()) {
                // Sort theo viral score
                videos.sort(Comparator.comparing(VideoItem::getViralScore, Comparator.nullsLast(Double::compareTo)).reversed());
                
                topScore = Optional.ofNullable(videos.get(0).getViralScore()).orElse(0.0);
                avgScore = videos.stream()
                        .mapToDouble(v -> Optional.ofNullable(v.getViralScore()).orElse(0.0))
                        .average()
                        .orElse(0.0);
                
                // Lấy công thức từ video đầu tiên
                recipe = videos.get(0).getBasicRecipe();
            }
            
            // Nếu không có recipe từ video, lấy từ Gemini
            if (recipe == null || recipe.isBlank()) {
                // Tìm từ Gemini info
                for (GeminiClient.TrendingDishInfo dishInfo : trendingDishes) {
                    if (dishInfo.getName().equals(dishName)) {
                        recipe = dishInfo.getBasicRecipe();
                        break;
                    }
                }
            }
            
            double rating = calculateRating(avgScore, videos.size());
            
            groups.add(DishGroup.builder()
                    .key(keyOf(dishName))
                    .name(dishName)
                    .basicRecipe(recipe != null ? recipe : "")
                    .totalVideos(videos.size())
                    .topScore(topScore)
                    .rating(rating)
                    .videos(videos)
                    .build());
        }

        // Sort theo topScore
        groups.sort(Comparator.comparing(DishGroup::getTopScore).reversed());
        
        // Giới hạn số lượng món
        if (groups.size() > max / 5) {
            groups = groups.subList(0, Math.min(max / 5, groups.size()));
        }

        long tookMs = System.currentTimeMillis() - t0;
        int totalVideos = groups.stream().mapToInt(DishGroup::getTotalVideos).sum();
        
        System.out.println("[AiSuggest] Finished: " + groups.size() + " dish groups, " + totalVideos + " total videos, took " + tookMs + "ms");
        
        return DishTrendsResponse.builder()
                .data(groups)
                .meta(Map.of(
                        "days", days,
                        "max", max,
                        "totalVideos", totalVideos,
                        "grouped", groups.size(),
                        "tookMs", tookMs
                ))
                .build();
    }

    /**
     * Lọc video liên quan đến món cụ thể (công thức/giới thiệu)
     */
    private List<VideoItem> filterRelevantVideos(List<VideoItem> videos, String dishName) {
        String dishLower = dishName.toLowerCase();
        return videos.stream().filter(v -> {
            String title = (v.getTitle() != null ? v.getTitle() : "").toLowerCase();
            String desc = (v.getDescription() != null ? v.getDescription() : "").toLowerCase();
            String text = title + " " + desc;
            
            // ===== BƯỚC 1: REJECT các loại video CHẮC CHẮN không phải recipe =====
            
            // REJECT: Spam, memes, animations
            if (title.contains("#meme") || title.contains("#animation") || title.contains("#cartoon") || 
                title.contains("brainrot") || title.contains("gấu bông") || title.contains("đồ chơi")) {
                System.out.println("[Filter] REJECTED (spam/meme): " + v.getTitle());
                return false;
            }
            
            // REJECT: Thiết bị/máy móc (ưu tiên cao vì dễ nhầm)
            boolean isEquipment = 
                title.contains("máy pha") || title.contains("máy xay") || title.contains("máy đánh") ||
                title.contains("máy làm") || title.contains("máy nén") || title.contains("thiết bị") ||
                title.contains("đèn thả") || title.contains("đèn led") || title.contains("đèn trang trí") ||
                title.contains("bếp điện") || title.contains("lò nướng") || title.contains("tủ lạnh") ||
                title.matches(".*\\d+k.*") && (title.contains("mua") || title.contains("bán") || title.contains("giá")) ||  // "giá 2 triệu", "chỉ 99k"
                title.matches(".*\\d+ triệu.*") || title.matches(".*giá chỉ.*") ||
                title.contains("sale") || title.contains("deal") || title.contains("giảm giá");
            
            // CHỈ ACCEPT thiết bị NẾU có từ "cách dùng" hoặc "hướng dẫn"
            if (isEquipment && !title.contains("cách dùng") && !title.contains("hướng dẫn sử dụng")) {
                System.out.println("[Filter] REJECTED (equipment): " + v.getTitle());
                return false;
            }
            
            // REJECT: Shop/Quán/Địa điểm
            boolean isShopLocation = 
                title.contains("shop") || title.contains("store") || title.contains("cửa hàng") ||
                title.contains("quán") && (title.contains("ghé") || title.contains("check in") || title.contains("review địa điểm")) ||
                title.contains("menu giá") || title.contains("bảng giá") || title.contains("nhà hàng") ||
                title.contains("decor") || title.contains("trang trí") || title.contains("thiết kế quán") ||
                title.contains("lắp đèn") || title.contains("sửa chữa") || title.contains("thi công");
            
            // CHỈ ACCEPT shop NẾU có từ recipe/công thức
            if (isShopLocation && !title.contains("công thức") && !title.contains("recipe") && !title.contains("cách làm")) {
                System.out.println("[Filter] REJECTED (shop/location): " + v.getTitle());
                return false;
            }
            
            // REJECT: Sản phẩm vật lý khác
            if ((title.contains("khay") || title.contains("ly nhựa") || title.contains("cốc nhựa") ||
                 title.contains("quầy bar") || title.contains("kệ trưng bày") || title.contains("bao bì") ||
                 title.contains("túi giấy") || title.contains("hộp đựng")) && 
                !title.contains("cách") && !title.contains("recipe") && !title.contains("hướng dẫn")) {
                System.out.println("[Filter] REJECTED (physical product): " + v.getTitle());
                return false;
            }
            
            // ===== BƯỚC 2: ACCEPT video về recipe/tutorial/giới thiệu =====
            
            // Từ khóa MẠNH về recipe/tutorial (ưu tiên cao)
            boolean hasStrongRecipeKeyword = 
                title.contains("cách làm") || title.contains("cách pha") || title.contains("recipe") || 
                title.contains("how to make") || title.contains("hướng dẫn") || 
                title.contains("công thức") || title.contains("pha chế") ||
                title.contains("bí quyết") || title.contains("mẹo") || title.contains("tips") ||
                title.contains("diy") || title.contains("homemade") || title.contains("tự làm");
            
            // Từ khóa về giới thiệu/review món (medium)
            boolean hasIntroKeyword = 
                title.contains("giới thiệu") || title.contains("thử") || title.contains("review món") ||
                title.contains("món mới") || title.contains("hot trend") || title.contains("viral") ||
                title.contains("trải nghiệm") && text.contains(dishLower);
            
            // Kiểm tra xem title có chứa tên món không
            String[] dishKeywords = dishLower.split("\\s+");
            boolean hasDishKeyword = false;
            for (String keyword : dishKeywords) {
                if (keyword.length() > 2 && title.contains(keyword)) {
                    hasDishKeyword = true;
                    break;
                }
            }
            
            // LOGIC ACCEPT:
            // 1. Có từ khóa recipe MẠNH → ACCEPT ngay
            if (hasStrongRecipeKeyword) {
                System.out.println("[Filter] ACCEPTED (strong recipe keyword): " + v.getTitle());
                return true;
            }
            
            // 2. Có từ khóa giới thiệu + tên món → ACCEPT
            if (hasIntroKeyword && hasDishKeyword) {
                System.out.println("[Filter] ACCEPTED (intro + dish name): " + v.getTitle());
                return true;
            }
            
            // 3. Có tên món + từ "cách" (rộng hơn) → ACCEPT
            if (hasDishKeyword && title.contains("cách")) {
                System.out.println("[Filter] ACCEPTED (dish + cách): " + v.getTitle());
                return true;
            }
            
            // 4. Nới lỏng: Nếu video có tên món và không bị reject ở trên → ACCEPT
            // (để tránh mất quá nhiều video hợp lệ)
            if (hasDishKeyword) {
                System.out.println("[Filter] ACCEPTED (has dish keyword, not spam/shop/equipment): " + v.getTitle());
                return true;
            }
            
            // 5. REJECT tất cả các trường hợp còn lại
            System.out.println("[Filter] REJECTED (no dish keyword): " + v.getTitle());
            return false;
            
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Tính rating 1-5 sao dựa trên viral score và số lượng video
     * @param avgScore viral score trung bình
     * @param videoCount số lượng video
     * @return rating từ 1.0 đến 5.0
     */
    private double calculateRating(double avgScore, int videoCount) {
        // Công thức:
        // - Base rating từ viral score (0-4 sao)
        // - Bonus từ số lượng video (0-1 sao)
        
        // 1. Base rating từ viral score (log scale)
        // avgScore thường trong khoảng 10-50
        double baseRating = Math.min(4.0, avgScore / 10.0);
        
        // 2. Bonus từ số lượng video (càng nhiều video = càng hot)
        double videoBonus = Math.min(1.0, videoCount / 5.0);
        
        // 3. Tổng rating (1.0 - 5.0)
        double rating = Math.max(1.0, baseRating + videoBonus);
        
        // 4. Làm tròn 1 chữ số thập phân
        return Math.round(rating * 10.0) / 10.0;
    }
    
    private String keyOf(String name) {
        if (name == null) return null;
        String s = name.toLowerCase().trim();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("-+", "-");
        return s.replaceAll("(^-|-$)", "");
    }
    
    private boolean looksVietnamese(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        
        // Check dấu tiếng Việt
        boolean hasDiacritics = text.matches(".*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ].*");
        
        // Check từ khóa tiếng Việt phổ biến
        boolean hasVietnameseWords = 
            lower.contains("cách") || lower.contains("pha") || lower.contains("chế") ||
            lower.contains("cà phê") || lower.contains("trà sữa") || lower.contains("đồ uống") ||
            lower.contains("công thức") || lower.contains("hot trend") || lower.contains("viral") ||
            lower.contains("ngon") || lower.contains("dễ") || lower.contains("đơn giản");
        
        return hasDiacritics || hasVietnameseWords;
    }
    
    private List<VideoItem> createMockVideos() {
        OffsetDateTime now = OffsetDateTime.now();
        List<VideoItem> mocks = new ArrayList<>();
        
        // Cà phê Việt Nam
        mocks.add(createMockVideo("v001", "Cách pha Cà phê muối ngon đúng điệu", 
                "Hướng dẫn chi tiết cách pha cà phê muối đặc trưng miền Trung", 
                15000L, 800L, now.minusDays(2), 58));
        mocks.add(createMockVideo("v002", "Cà phê trứng Hà Nội - Công thức gốc", 
                "Bí quyết pha cà phê trứng béo ngậy như ở Giảng", 
                28000L, 1500L, now.minusDays(1), 55));
        mocks.add(createMockVideo("v003", "Cà phê dừa sảng khoái mùa hè", 
                "Cách làm cà phê dừa mát lạnh cho ngày nóng", 
                12000L, 600L, now.minusDays(3), 60));
        
        // Trà sữa đa dạng
        mocks.add(createMockVideo("v004", "Trà sữa trân châu đường đen HOT TREND", 
                "Công thức trà sữa trân châu đường đen đang viral", 
                45000L, 2300L, now.minusDays(1), 59));
        mocks.add(createMockVideo("v005", "Trà sữa kem cheese siêu thơm ngon", 
                "Cách làm lớp kem cheese mịn tan cho trà sữa", 
                22000L, 1100L, now.minusDays(4), 57));
        mocks.add(createMockVideo("v006", "Trà sữa matcha Nhật Bản đắng nhẹ", 
                "Pha trà sữa matcha chuẩn vị Nhật", 
                18000L, 900L, now.minusDays(5), 56));
        
        // Matcha variations
        mocks.add(createMockVideo("v007", "Dirty Matcha - Xu hướng 2024", 
                "Cách làm Dirty Matcha đẹp lạ mắt", 
                38000L, 1900L, now.minusDays(1), 58));
        mocks.add(createMockVideo("v008", "Matcha latte kem trứng béo ngậy", 
                "Kết hợp matcha với kem trứng siêu ngon", 
                25000L, 1200L, now.minusDays(2), 60));
        
        // Đồ uống trending khác
        mocks.add(createMockVideo("v009", "Dalgona Coffee - Viral TikTok", 
                "Cách đánh cà phê bông mịn như mây", 
                52000L, 2800L, now.minusDays(1), 45));
        mocks.add(createMockVideo("v010", "Trà đào cam sả ngày hè", 
                "Công thức trà đào cam sả thanh mát", 
                31000L, 1600L, now.minusDays(2), 58));
        mocks.add(createMockVideo("v011", "Sữa chua dâu tây mix yogurt", 
                "Làm sữa chua dâu tây mát lạnh", 
                19000L, 950L, now.minusDays(3), 52));
        mocks.add(createMockVideo("v012", "Trà sữa Oolong - Vị trà thuần", 
                "Pha trà sữa Oolong cho người yêu trà", 
                16000L, 820L, now.minusDays(4), 59));
        
        return mocks;
    }
    
    private VideoItem createMockVideo(String id, String title, String desc, 
                                     Long views, Long likes, OffsetDateTime published, int durationSec) {
        VideoItem v = VideoItem.builder()
                .videoId(id)
                .title(title)
                .description(desc)
                .thumbnailUrl("https://i.ytimg.com/vi/" + id + "/hqdefault.jpg")
                .channelTitle("Kopi Cafe Channel")
                .publishedAt(published)
                .videoUrl("https://youtube.com/shorts/" + id)
                .viewCount(views)
                .likeCount(likes)
                .durationSeconds((long) durationSec)
                .build();
        
        // Thêm công thức mẫu dựa vào title
        String lowerTitle = title.toLowerCase();
        String recipe = "";
        if (lowerTitle.contains("muối") || lowerTitle.contains("salt")) {
            recipe = "1. Pha 2 shot espresso đậm đặc\\n2. Thêm 1 thìa muối\\n3. Đánh bông với sữa tươi";
        } else if (lowerTitle.contains("trứng") || lowerTitle.contains("egg")) {
            recipe = "1. Đánh trứng gà với sữa đặc\\n2. Pha cà phê phin đậm\\n3. Đổ trứng lên trên";
        } else if (lowerTitle.contains("dừa") || lowerTitle.contains("coconut")) {
            recipe = "1. Pha cà phê đen đá\\n2. Thêm nước cốt dừa\\n3. Cho đá viên và khuấy đều";
        } else if (lowerTitle.contains("matcha")) {
            recipe = "1. Hòa bột matcha với nước nóng\\n2. Thêm sữa tươi\\n3. Đổ lên đá và trang trí";
        } else if (lowerTitle.contains("trân châu")) {
            recipe = "1. Nấu trân châu đen\\n2. Pha trà sữa với đường nâu\\n3. Cho trân châu vào ly";
        } else {
            recipe = "1. Pha nguyên liệu chính\\n2. Thêm sữa hoặc nước\\n3. Trộn đều và phục vụ";
        }
        v.setBasicRecipe(recipe);
        
        return v;
    }
}
