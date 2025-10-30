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
    public DishTrendsResponse groupedByDish(AiSuggestionRequest req) {
        long t0 = System.currentTimeMillis();
        int days  = Optional.ofNullable(req.getDays()).orElse(90);  
        int max   = Math.min(Optional.ofNullable(req.getMaxResults()).orElse(50), 150);  
        boolean shortsOnly = false;  // FORCE BỎ shortsOnly vì gây nhiễu
        
        System.out.println("[AiSuggest] groupedByDish called: days=" + days + ", max=" + max + ", shortsOnly=" + shortsOnly);

        // 1) Queries TỔNG QUÁT - để YouTube tự gợi ý các món
        List<VideoItem> videos = new ArrayList<>();
        String[] queries = {
            // Queries chung về đồ uống
            "cách làm đồ uống", "drink recipe", "beverage recipe",
            // Cà phê tổng quát
            "cách pha cà phê", "coffee drink", "coffee recipe",
            "vietnamese coffee", "iced coffee",
            // Trà tổng quát
            "trà sữa", "milk tea", "bubble tea",
            "trà trái cây", "fruit tea", "tea recipe",
            // Matcha/Chocolate
            "matcha drink", "matcha recipe",
            "chocolate drink", "hot chocolate",
            // Đồ uống mùa hè
            "smoothie recipe", "sinh tố", "juice recipe",
            // Trending
            "trending drink", "viral drink", "đồ uống hot trend"
        };
        
        int perQuery = Math.max(20, max / queries.length + 10);  // Tăng lên để có NHIỀU video hơn
        
        for (String query : queries) {
            try {
                System.out.println("[AiSuggest] Query: " + query);
                // Thêm regionCode=VN và relevanceLanguage=vi để ưu tiên video Việt
                List<VideoItem> batch = yt.searchRecentVideos(
                        query, 0, perQuery, "VN", "vi", shortsOnly, null, "moderate", false
                );
                System.out.println("[AiSuggest]   -> Found " + batch.size() + " videos");
                videos.addAll(batch);
            } catch (Exception e) {
                System.err.println("[AiSuggest] Query failed: " + query + " - " + e.getMessage());
            }
        }
        
        // Deduplicate
        Map<String, VideoItem> unique = new LinkedHashMap<>();
        for (VideoItem v : videos) unique.put(v.getVideoId(), v);
        videos = new ArrayList<>(unique.values());
        
        System.out.println("[AiSuggest] Total unique before filter: " + videos.size());
        
        // FILTER 1: Loại bỏ spam/memes
        videos = videos.stream().filter(v -> {
            String title = v.getTitle().toLowerCase();
            boolean isSpam = 
                title.contains("#meme") || title.contains("#animation") ||
                title.contains("#cartoon") || title.contains("brainrot");
            
            if (isSpam) {
                System.out.println("[AiSuggest] SPAM FILTERED: " + v.getTitle());
                return false;
            }
            return true;
        }).collect(java.util.stream.Collectors.toList());
        
        System.out.println("[AiSuggest] After spam filter: " + videos.size());
        
        // FILTER 2: CHỈ REJECT video CHẮC CHẮN không phải về đồ uống
        videos = videos.stream().filter(v -> {
            String title = v.getTitle().toLowerCase();
            String desc = v.getDescription().toLowerCase();
            
            // REJECT CHẮC CHẮN không phải recipe: sản phẩm/dụng cụ/review thiết bị
            boolean shouldReject = 
                // Sản phẩm vật lý
                title.contains("khay") && !title.contains("cách") ||
                title.contains("ly nhựa") || title.contains("cốc nhựa") ||
                title.contains("quầy bar") || title.contains("kệ trưng bày") ||
                title.contains("gấu bông") || title.contains("đồ chơi") ||
                // Shop/Quán (không phải recipe)
                title.contains("shop") && !title.contains("recipe") ||
                title.contains("store") && !title.contains("how") ||
                title.contains("menu giá") || title.contains("bảng giá") ||
                // Thiết bị
                title.contains("máy pha") && !title.contains("cách") ||
                title.contains("máy xay") && !title.contains("dùng") ||
                title.contains("thiết bị");
            
            if (shouldReject) {
                System.out.println("[AiSuggest] REJECTED (product/shop): " + v.getTitle());
                return false;
            }
            
            // GIỮ LẠI tất cả video còn lại (giả định là về đồ uống)
            return true;
        }).collect(java.util.stream.Collectors.toList());
        
        System.out.println("[AiSuggest] Final count: " + videos.size() + " videos");
        
        // Show first 10 videos for debugging
        System.out.println("[AiSuggest] First 10 videos:");
        for (int i = 0; i < Math.min(10, videos.size()); i++) {
            VideoItem v = videos.get(i);
            System.out.println("  " + (i+1) + ". " + v.getTitle() + " (" + v.getViewCount() + " views)");
        }
        
        if (videos.isEmpty()) {
            System.err.println("[AiSuggest] No videos from YouTube, using MOCK");
            videos = createMockVideos();
        }

        // 2) Tính điểm viral đơn giản theo view/like và độ mới
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
        }

        // 3) DÙNG FALLBACK LOGIC (vì YouTube hết quota, chỉ có mock data tiếng Việt)
        System.out.println("[AiSuggest] Using Vietnamese fallback logic for dish extraction...");
        List<DishExtraction> exts = new ArrayList<>();
        
        for (VideoItem v : videos) {
            String title = v.getTitle().toLowerCase();
            String dishName = null;
            String recipe = "";
            double conf = 0.8;
            
            // Coffee variations (ưu tiên các món cụ thể)
            if (title.contains("cà phê muối") || title.contains("salted coffee")) {
                dishName = "Cà phê muối";
                recipe = "1. Pha 2 shot espresso đậm\n2. Thêm 1/4 thìa cà phê muối hồng Himalaya\n3. Đánh sữa tươi thành foam mịn\n4. Rưới muối lên bọt sữa";
            }
            else if (title.contains("cà phê trứng") || title.contains("egg coffee")) {
                dishName = "Cà phê trứng";
                recipe = "1. Pha cà phê phin đậm 40ml\n2. Đánh lòng đỏ trứng gà với sữa đặc 5 phút\n3. Cho cà phê vào ly, phủ lớp kem trứng lên trên\n4. Trang trí bột ca cao";
            }
            else if (title.contains("cà phê dừa") || title.contains("coconut coffee")) {
                dishName = "Cà phê dừa";
                recipe = "1. Pha 2 shot espresso\n2. Cho 100ml nước cốt dừa tươi\n3. Thêm đá viên\n4. Rưới thêm sữa đặc";
            }
            else if (title.contains("dalgona")) {
                dishName = "Dalgona Coffee";
                recipe = "1. Trộn 2 thìa cà phê bột, 2 thìa đường, 2 thìa nước nóng\n2. Đánh bông 5-7 phút cho đến khi sánh mịn\n3. Cho sữa tươi vào ly đầy đá\n4. Xúc kem cà phê lên trên";
            }
            else if (title.contains("latte")) {
                dishName = "Latte";
                recipe = "1. Pha 2 shot espresso\n2. Hấp 200ml sữa tươi đến 65°C\n3. Rót sữa từ từ vào espresso\n4. Tạo hình latte art";
            }
            else if (title.contains("cappuccino")) {
                dishName = "Cappuccino";
                recipe = "1. Pha 1 shot espresso\n2. Đánh sữa tạo foam dày\n3. Rót sữa và foam theo tỷ lệ 1:1\n4. Rắc bột ca cao";
            }
            
            // Milk tea variations
            else if (title.contains("trà sữa trân châu") || title.contains("boba") || title.contains("đường đen")) {
                dishName = "Trà sữa trân châu đường đen";
                recipe = "1. Nấu trân châu với đường đen 30 phút\n2. Pha trà đen đậm, để nguội\n3. Cho trân châu vào ly, rưới đường đen\n4. Thêm đá, trà và sữa tươi";
            }
            else if (title.contains("kem cheese") || title.contains("cheese")) {
                dishName = "Trà sữa kem cheese";
                recipe = "1. Pha trà oolong\n2. Đánh kem cheese (cream cheese + whipping cream + đường)\n3. Cho trà vào ly có đá\n4. Phủ lớp kem cheese dày lên trên";
            }
            else if (title.contains("trà sữa matcha") || title.contains("matcha") && title.contains("trà sữa")) {
                dishName = "Trà sữa matcha";
                recipe = "1. Hòa 2g bột matcha với 50ml nước nóng 80°C\n2. Thêm 150ml sữa tươi\n3. Cho đá và đường theo khẩu vị\n4. Lắc đều";
            }
            else if (title.contains("trà sữa oolong")) {
                dishName = "Trà sữa Oolong";
                recipe = "1. Pha trà Oolong đậm 5 phút\n2. Lọc bỏ lá trà\n3. Thêm sữa tươi và đường\n4. Cho đá và lắc đều";
            }
            else if (title.contains("trà sữa")) {
                dishName = "Trà sữa";
                recipe = "1. Pha trà đen đậm\n2. Thêm sữa tươi hoặc sữa đặc\n3. Thêm đường/mật ong\n4. Cho đá và lắc đều";
            }
            
            // Matcha variations
            else if (title.contains("dirty matcha")) {
                dishName = "Dirty Matcha";
                recipe = "1. Cho sữa tươi đầy ly có đá\n2. Pha 2g matcha với 30ml nước nóng\n3. Rót matcha từ từ vào sữa tạo hiệu ứng 'dirty'\n4. Không khuấy";
            }
            else if (title.contains("matcha latte")) {
                dishName = "Matcha Latte";
                recipe = "1. Hòa 2g bột matcha với 50ml nước 80°C\n2. Đánh tan hoàn toàn\n3. Hấp 200ml sữa tươi\n4. Rót sữa vào matcha, tạo latte art";
            }
            else if (title.contains("matcha")) {
                dishName = "Matcha";
                recipe = "1. Cho 2g bột matcha vào chawan (bát trà)\n2. Thêm 70ml nước 80°C\n3. Dùng chasen (chổi tre) đánh tròn\n4. Uống ngay khi còn nóng";
            }
            
            // Other drinks
            else if (title.contains("trà đào")) {
                dishName = "Trà đào cam sả";
                recipe = "1. Nấu sả với nước đường\n2. Pha trà xanh, để nguội\n3. Thêm đào, cam thái lát\n4. Cho đá và trộn đều";
            }
            else if (title.contains("sữa chua")) {
                dishName = "Sữa chua dâu tây";
                recipe = "1. Cho sữa chua Hy Lạp vào ly\n2. Thêm dâu tây tươi thái lát\n3. Rưới mật ong\n4. Rắc granola";
            }
            
            if (dishName != null) {
                exts.add(new DishExtraction(v.getVideoId(), dishName, recipe, conf));
            }
        }
        System.out.println("[AiSuggest] Fallback extracted " + exts.size() + " dishes");
        
        Map<String, DishExtraction> idToExt = new HashMap<>();
        for (DishExtraction de : exts) idToExt.put(de.getId(), de);
        for (VideoItem v : videos) {
            DishExtraction de = idToExt.get(v.getVideoId());
            System.out.println("[AiSuggest] Video " + v.getVideoId() + " title: " + v.getTitle());
            if (de != null) {
                String dishName = de.getDishName();
                System.out.println("[AiSuggest]   -> dish=" + dishName + ", recipe=" + 
                    (de.getBasicRecipe() == null || de.getBasicRecipe().isBlank() ? "EMPTY" : "YES") + 
                    ", confidence=" + de.getConfidence());
                
                // REJECT chỉ tên QUÁ chung chung (1 từ đơn)
                boolean isGeneric = dishName != null && dishName.matches("^(Coffee|Tea|Drink)$");
                
                // Chấp nhận nếu có ít nhất 2 từ hoặc có modifier
                boolean isSpecific = dishName != null && (
                    dishName.split("\\s+").length >= 2 ||  // ít nhất 2 từ
                    dishName.contains("Latte") ||
                    dishName.contains("Matcha") ||
                    dishName.contains("Brew") ||
                    dishName.contains("Milk Tea")
                );
                
                if (dishName != null && !dishName.isBlank() && de.getConfidence() >= 0.25 && (isSpecific || !isGeneric)) {
                    v.setDishName(dishName);
                    v.setDishKey(keyOf(dishName));
                    v.setBasicRecipe(de.getBasicRecipe());
                    System.out.println("[AiSuggest]   -> ACCEPTED: " + dishName);
                } else {
                    if (isGeneric) {
                        System.out.println("[AiSuggest]   -> REJECTED: GENERIC NAME '" + dishName + "'");
                    } else {
                        System.out.println("[AiSuggest]   -> REJECTED (confidence=" + de.getConfidence() + ")");
                    }
                }
            } else {
                System.out.println("[AiSuggest]   -> No extraction");
            }
        }

        // 4) Lọc video có dishKey
        List<VideoItem> usable = new ArrayList<>();
        for (VideoItem v : videos) {
            if (v.getDishKey() != null && !v.getDishKey().isBlank()) usable.add(v);
        }

        // 5) Gom nhóm theo dishKey và sort theo viralScore
        Map<String, List<VideoItem>> grouped = new HashMap<>();
        for (VideoItem v : usable) {
            grouped.computeIfAbsent(v.getDishKey(), k -> new ArrayList<>()).add(v);
        }

        List<DishGroup> groups = new ArrayList<>();
        for (Map.Entry<String,List<VideoItem>> e : grouped.entrySet()) {
            List<VideoItem> sorted = new ArrayList<>(e.getValue());
            sorted.sort(Comparator.comparing(VideoItem::getViralScore, Comparator.nullsLast(Double::compareTo)).reversed());
            String name = sorted.get(0).getDishName();
            String recipe = sorted.get(0).getBasicRecipe(); // lấy công thức từ video có viralScore cao nhất
            double top = Optional.ofNullable(sorted.get(0).getViralScore()).orElse(0.0);
            groups.add(DishGroup.builder()
                    .key(e.getKey())
                    .name(name)
                    .basicRecipe(recipe != null ? recipe : "")
                    .totalVideos(sorted.size())
                    .topScore(top)
                    .videos(sorted)
                    .build());
        }

        groups.sort(Comparator.comparing(DishGroup::getTopScore).reversed());

        long tookMs = System.currentTimeMillis() - t0;
        System.out.println("[AiSuggest] Finished: " + groups.size() + " dish groups, took " + tookMs + "ms");
        
        return DishTrendsResponse.builder()
                .data(groups)
                .meta(Map.of(
                        "days", days,
                        "max", max,
                        "totalVideos", videos.size(),
                        "grouped", groups.size(),
                        "tookMs", tookMs
                ))
                .build();
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
