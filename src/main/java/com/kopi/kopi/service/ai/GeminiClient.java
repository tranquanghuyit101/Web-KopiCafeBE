package com.kopi.kopi.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopi.kopi.dto.ai.SuggestedDrink;
import com.kopi.kopi.dto.ai.DishExtraction;
import java.util.*;
import java.util.stream.Collectors;

import com.kopi.kopi.dto.ai.VideoItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiClient {
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${ai.gemini.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;

    /** NEW: Trích tên món cho từng video (batch). */
    public List<DishExtraction> extractDishNames(List<VideoItem> videos, String language) {
        if (videos == null || videos.isEmpty()) return List.of();

        String dataJson;
        try {
            dataJson = om.writeValueAsString(videos.stream().map(v -> Map.of(
                    "id",    v.getVideoId(),
                    "title", v.getTitle(),
                    "desc",  v.getDescription()
            )).collect(Collectors.toList()));
        } catch (Exception e) {
            dataJson = "[]";
        }

        String instr = """
            Role: Expert Vietnamese Coffee Shop Menu Consultant
            
            Task: Analyze EACH YouTube video INDEPENDENTLY and extract:
            1. SPECIFIC DRINK NAME (dishName) - in Vietnamese if possible
            2. BASIC RECIPE (basicRecipe) - 3-4 steps in Vietnamese
            
            CRITICAL INSTRUCTIONS:
            ══════════════════════════════════════════════════════════════
            
            📋 STEP 1: IDENTIFY THE MAIN DRINK TYPE
            - Read title & description carefully
            - Identify MAIN ingredient/theme:
              * Cà phê (Coffee): espresso, cold brew, phin, latte, cappuccino
              * Trà sữa (Milk Tea): boba, trân châu, cheese foam, oolong
              * Trà trái cây (Fruit Tea): đào, chanh, cam, dâu, đậu biếc
              * Matcha: latte, dirty, frappe, kem muối
              * Chocolate: hot chocolate, socola, cacao
              * Smoothie/Sinh tố: bơ, dâu, xoài, chuối
              * Yogurt/Sữa chua: dâu, trái cây, hạt chia
            
            📋 STEP 2: EXTRACT SPECIFIC VARIANT
            - USE EXACT NAME from title if available
            - Include ALL modifiers:
              ✅ "Cà phê muối" (NOT just "Coffee")
              ✅ "Trà sữa trân châu đường đen" (NOT just "Milk Tea")
              ✅ "Matcha Latte" (NOT just "Matcha")
              ✅ "Hot Chocolate bạc hà" (NOT just "Hot Chocolate")
            
            📋 STEP 3: AVOID DUPLICATES
            - If video is about "Matcha" but query was "coffee recipe" → dishName = null
            - If video is UNRELATED to drinks (product review, shop tour) → dishName = null
            - If video is TUTORIAL/RECIPE → confidence HIGH (0.7-1.0)
            - If video is just DRINKING/TASTING → confidence LOW (0.3-0.5)
            
            📋 STEP 4: GENERATE RECIPE
            - MUST be in Vietnamese
            - 3-4 clear steps
            - Include measurements (ml, g, thìa)
            - Example:
              "1. Pha 2 shot espresso đậm\\n2. Thêm 1/4 thìa muối hồng Himalaya\\n3. Đánh sữa tạo foam\\n4. Rưới muối lên bọt sữa"
            
            ══════════════════════════════════════════════════════════════
            
            FORBIDDEN GENERIC NAMES (AUTO-REJECT):
            ❌ "Coffee", "Tea", "Milk Tea", "Matcha", "Drink"
            ✅ "Iced Coffee", "Matcha Latte", "Trà sữa Oolong" (OK if ≥2 words)
            
            CONFIDENCE SCORING:
            - 0.8-1.0: Title explicitly states drink name + clear recipe/tutorial
            - 0.5-0.7: Drink name clear but video is taste test/review
            - 0.3-0.4: Can infer from context but not explicitly stated
            - < 0.3: Too vague or unrelated → dishName = null
            
            SPECIAL CASES:
            - Matcha videos: MUST specify variant ("Matcha Latte", "Dirty Matcha", etc.)
            - Coffee videos: MUST specify type ("Cà phê muối", "Latte", "Cappuccino", etc.)
            - If video shows multiple drinks: Pick the MAIN one (highest focus)
            
            OUTPUT (STRICT JSON):
            { "items": [ { "id": string, "dishName": string|null, "basicRecipe": string, "confidence": number } ] }
            
            Language: %s
            Videos:
            %s
        """.formatted(language == null ? "vi" : language, dataJson);

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            Map<String,Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", instr)))),
                    "generationConfig", Map.of("responseMimeType", "application/json")
            );

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> r = http.postForEntity(url, new HttpEntity<>(body, h), String.class);
            JsonNode root = om.readTree(r.getBody());
            String text = root.path("candidates").path(0).path("content").path(0).path("parts")
                    .path(0).path("text").asText("{}");

            JsonNode parsed = om.readTree(text);
            List<DishExtraction> out = new ArrayList<>();
            for (JsonNode it : parsed.path("items")) {
                String id = it.path("id").asText(null);
                String dish = it.hasNonNull("dishName") ? it.path("dishName").asText() : null;
                String recipe = it.path("basicRecipe").asText("");
                double conf = it.path("confidence").asDouble(0.0);
                if (id != null) out.add(new DishExtraction(id, dish, recipe, conf));
            }
            return out;
        } catch (Exception e) {
            // Fallback: rất đơn giản (heuristic) nếu API lỗi
            List<DishExtraction> out = new ArrayList<>();
            for (VideoItem v : videos) {
                String dish = guessDishName(v.getTitle(), v.getDescription());
                out.add(new DishExtraction(
                        v.getVideoId(),
                        dish,
                        "",  // không có công thức trong fallback
                        dish == null ? 0.0 : 0.55
                ));
            }
            return out;
        }
    }

    // === HÀM CŨ CỦA BẠN: GIỮ NGUYÊN ===
    public List<SuggestedDrink> rankSuggestions(List<VideoItem> videos, String language) {
        if (videos == null || videos.isEmpty()) return List.of();

        String dataJson;
        try {
            dataJson = om.writeValueAsString(videos.stream().map(v -> Map.of(
                    "id", v.getVideoId(),
                    "title", v.getTitle(),
                    "desc", v.getDescription(),
                    "views", v.getViewCount(),
                    "likes", v.getLikeCount(),
                    "url", v.getVideoUrl()
            )).collect(Collectors.toList()));
        } catch (Exception e) {
            dataJson = "[]";
        }

        String instr = """
            Role: You are a senior barista-menu consultant for a Vietnamese coffee shop (Kopi).
            Task: From the given YouTube video list (JSON), select ONLY videos clearly related to drinks/beverages, such as:
            - How-to/recipe/tips for making drinks (e.g., pha chế, recipe, how to make, hướng dẫn)
            - Hot-trend beverages, seasonal/menu ideas for cafes in Vietnam
            - Coffee, tea, matcha, fruit tea, soda, signature drinks; exclude food/non-drink topics

            Strict filters BEFORE proposing items:
            - Exclude videos unrelated to beverages (gaming, vlog đời sống không pha chế, review không rõ đồ uống, thiết bị không liên quan)
            - Exclude non-feasible items for typical cafe operations (quá phức tạp, khó nguồn nguyên liệu)
            - Prefer videos with clearer recipe/process signals in title/description (recipe, công thức, cách làm, hướng dẫn, mix, barista)

            Output: Identify distinct drink concepts that Kopi có thể bán. Consider popularity (views/likes), feasibility, cost, and Vietnamese taste.
            Return STRICT JSON ONLY, no extra text, with schema:
            { "items": [ {
                "name": string,
                "reason": string,
                "score": number,
                "tags": [string],
                "basicRecipe": string,
                "fromVideos": [ { "id": string } ]
            } ] }

            Language: %s.
            Videos JSON:
            %s
        """.formatted(language == null ? "vi" : language, dataJson);

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            Map<String,Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", instr)))),
                    "generationConfig", Map.of("responseMimeType", "application/json")
            );

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> r = http.postForEntity(url, new HttpEntity<>(body, h), String.class);

            JsonNode root = om.readTree(r.getBody());
            String text = root.path("candidates").path(0).path("content").path(0).path("parts").path(0).path("text").asText("{}");

            JsonNode parsed = om.readTree(text);
            List<SuggestedDrink> out = new ArrayList<>();
            for (JsonNode it : parsed.path("items")) {
                List<VideoItem> src = new ArrayList<>();
                for (JsonNode vv : it.path("fromVideos")) {
                    String id = vv.path("id").asText(null);
                    if (id != null) {
                        src.add(VideoItem.builder()
                                .videoId(id)
                                .videoUrl("https://www.youtube.com/watch?v=" + id)
                                .build());
                    }
                }
                List<String> tags = new ArrayList<>();
                it.path("tags").forEach(n -> tags.add(n.asText("")));

                out.add(SuggestedDrink.builder()
                        .name(it.path("name").asText(""))
                        .reason(it.path("reason").asText(""))
                        .score(it.path("score").asDouble(0))
                        .basicRecipe(it.path("basicRecipe").asText(""))
                        .tags(tags)
                        .fromVideos(src)
                        .build());
            }
            return out;
        } catch (Exception e) {
            // Fallback: gộp theo tiêu đề "đơn giản"
            return videos.stream()
                    .collect(Collectors.groupingBy(v -> simplify(v.getTitle()), LinkedHashMap::new, Collectors.toList()))
                    .entrySet().stream().map(e1 -> {
                        long views = e1.getValue().stream().mapToLong(v -> Optional.ofNullable(v.getViewCount()).orElse(0L)).sum();
                        long likes = e1.getValue().stream().mapToLong(v -> Optional.ofNullable(v.getLikeCount()).orElse(0L)).sum();
                        double score = Math.min(100, Math.log10(1 + views) * 20 + Math.log10(1 + likes) * 10);
                        return SuggestedDrink.builder()
                                .name(e1.getKey())
                                .reason("Heuristic ranking by views/likes (fallback).")
                                .score(score)
                                .tags(List.of("fallback"))
                                .fromVideos(e1.getValue())
                                .build();
                    }).collect(Collectors.toList());
        }
    }

    private String simplify(String s) {
        return s == null ? "" : s.replaceAll("\\(.*?\\)", "").replaceAll("\\[.*?\\]", "").trim();
    }

    /**
     * Tìm các món đồ uống hot trend ở Việt Nam hiện tại
     * @param days số ngày gần đây để xem xét trend
     * @param maxResults số lượng món tối đa muốn lấy
     * @return danh sách tên món và công thức cơ bản
     */
    public List<TrendingDishInfo> findHotTrendDishes(int days, int maxResults) {
        System.out.println("[Gemini] findHotTrendDishes called with days=" + days + ", maxResults=" + maxResults);
        System.out.println("[Gemini] API Key present: " + (apiKey != null && !apiKey.isBlank()));
        System.out.println("[Gemini] Model: " + model);
        
        String currentDate = java.time.LocalDate.now().toString();
        String prompt = """
            Nhiệm vụ: Tìm kiếm và liệt kê các đồ uống HOT TREND hiện nay tại Việt Nam (tính đến %s).
            
            Hãy search trên Google với các từ khóa:
            - "đồ uống hot trend việt nam 2024 2025"
            - "món nước viral tiktok instagram"
            - "cafe trà sữa mới lạ"
            - "drink trends vietnam"
            
            Yêu cầu:
            1. Tìm ít nhất %d món đồ uống đang THỰC SỰ trending (có bằng chứng từ mạng xã hội, báo chí)
            2. Bao gồm cả:
               - Cà phê đặc biệt (cà phê muối, cà phê trứng, cold brew, ...)
               - Trà sữa và biến thể (shan tuyết, kem trứng nướng, kem cheese, ...)
               - Matcha variations (dirty matcha, matcha latte, coco matcha, ...)
               - Trà trái cây (trà đào cam sả, trà mãng cầu, ...)
               - Healthy drinks (kombucha, wellness shots, sinh tố xanh, oat milk latte, ...)
            3. Mỗi món PHẢI có công thức cơ bản (3-4 bước bằng tiếng Việt)
            4. Ưu tiên món MỚI, ĐỘCĐÁO, đang VIRAL trên social media
            
            Format JSON (BẮT BUỘC):
            {
              "dishes": [
                {
                  "name": "Cà phê muối",
                  "basicRecipe": "1. Pha 2 shot espresso đậm\\n2. Thêm 1/4 thìa muối hồng Himalaya\\n3. Đánh sữa tạo foam\\n4. Rưới muối lên bọt sữa",
                  "trendingScore": 0.85,
                  "source": "TikTok viral"
                }
              ]
            }
            
            Trả về đúng %d món, sắp xếp theo độ trending (cao nhất trước).
        """.formatted(currentDate, maxResults, maxResults);

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
            
            System.out.println("[Gemini] Calling URL: " + url.replace(apiKey, "***"));

            // Enable Google Search grounding (for gemini-2.0 models)
            Map<String,Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7
                    ),
                    "tools", List.of(Map.of(
                        "googleSearch", Map.of()  // Sử dụng googleSearch cho gemini-2.0
                    ))
            );

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> r = http.postForEntity(url, new HttpEntity<>(body, h), String.class);
            System.out.println("[Gemini] Response status: " + r.getStatusCode());
            System.out.println("[Gemini] Response body (first 500 chars): " + 
                (r.getBody() != null ? r.getBody().substring(0, Math.min(500, r.getBody().length())) : "null"));
            
            JsonNode root = om.readTree(r.getBody());
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            
            // Gemini có thể trả về multiple parts, cần concatenate lại
            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode part : parts) {
                String partText = part.path("text").asText("");
                textBuilder.append(partText);
            }
            String text = textBuilder.toString();
            
            // Strip markdown code block nếu có
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
            
            System.out.println("[Gemini] Extracted text (first 300 chars): " + 
                (text.length() > 300 ? text.substring(0, 300) : text));

            JsonNode parsed = om.readTree(text);
            List<TrendingDishInfo> out = new ArrayList<>();
            for (JsonNode it : parsed.path("dishes")) {
                String name = it.path("name").asText("");
                String recipe = it.path("basicRecipe").asText("");
                double score = it.path("trendingScore").asDouble(0.5);
                if (!name.isBlank()) {
                    out.add(new TrendingDishInfo(name, recipe, score));
                }
            }
            System.out.println("[Gemini] Successfully parsed " + out.size() + " dishes");
            return out;
        } catch (Exception e) {
            System.err.println("[Gemini] EXCEPTION: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            // Fallback: return common Vietnamese drinks
            System.out.println("[Gemini] Using fallback dishes...");
            return getFallbackTrendingDishes(maxResults);
        }
    }

    public List<TrendingDishInfo> getFallbackTrendingDishes(int maxResults) {
        // Mở rộng danh sách fallback để match với trend hiện tại
        List<TrendingDishInfo> fallback = List.of(
                // Cà phê trending
                new TrendingDishInfo("Cà phê muối", "1. Pha 2 shot espresso đậm\n2. Thêm 1/4 thìa muối hồng Himalaya\n3. Đánh sữa tạo foam\n4. Rưới muối lên bọt sữa", 0.85),
                new TrendingDishInfo("Cà phê trứng", "1. Pha cà phê phin đậm 40ml\n2. Đánh lòng đỏ trứng gà với sữa đặc 5 phút\n3. Cho cà phê vào ly, phủ lớp kem trứng lên trên\n4. Trang trí bột ca cao", 0.82),
                new TrendingDishInfo("Cà phê ủ lạnh Cold Brew", "1. Ngâm 100g cà phê xay thô với 1 lít nước lạnh\n2. Để tủ lạnh 12-24 giờ\n3. Lọc bỏ bã\n4. Pha loãng với nước đá và sữa", 0.80),
                new TrendingDishInfo("Oat Milk Latte", "1. Pha 2 shot espresso\n2. Hấp 200ml sữa yến mạch đến 65°C\n3. Rót sữa vào espresso\n4. Tạo latte art", 0.78),
                
                // Matcha trending
                new TrendingDishInfo("Coco Matcha", "1. Hòa 2g matcha với 30ml nước dừa nóng\n2. Thêm 150ml nước cốt dừa tươi\n3. Cho đá viên\n4. Trang trí dừa nạo", 0.82),
                new TrendingDishInfo("Dirty Matcha", "1. Cho sữa tươi đầy ly có đá\n2. Pha 2g matcha với 30ml nước nóng\n3. Rót matcha từ từ vào sữa tạo hiệu ứng 'dirty'\n4. Không khuấy", 0.80),
                new TrendingDishInfo("Matcha Latte", "1. Hòa 2g bột matcha với 50ml nước 80°C\n2. Đánh tan hoàn toàn\n3. Hấp 200ml sữa tươi\n4. Rót sữa vào matcha, tạo latte art", 0.75),
                
                // Trà sữa trending
                new TrendingDishInfo("Trà sữa Shan Tuyết", "1. Pha trà Shan Tuyết đậm 5 phút\n2. Lọc lá trà\n3. Thêm sữa tươi và đường mật\n4. Cho đá và lắc đều", 0.78),
                new TrendingDishInfo("Trà sữa kem trứng nướng", "1. Pha trà đen đậm\n2. Làm kem trứng nướng (trứng + sữa đặc + caramel)\n3. Cho trà vào ly có đá\n4. Phủ lớp kem trứng nướng lên trên", 0.76),
                new TrendingDishInfo("Trà sữa kem cheese", "1. Pha trà oolong\n2. Đánh kem cheese (cream cheese + whipping cream + đường)\n3. Cho trà vào ly có đá\n4. Phủ lớp kem cheese dày lên trên", 0.74),
                
                // Trà trái cây
                new TrendingDishInfo("Trà đào cam sả", "1. Nấu sả với nước đường\n2. Pha trà xanh, để nguội\n3. Thêm đào, cam thái lát\n4. Cho đá và trộn đều", 0.75),
                new TrendingDishInfo("Trà mãng cầu", "1. Pha trà xanh đậm\n2. Thêm mãng cầu tươi xay nhuyễn\n3. Cho đường mật\n4. Thêm đá và trang trí mãng cầu", 0.72),
                
                // Healthy drinks
                new TrendingDishInfo("Kombucha", "1. Ủ trà đen với SCOBY 7-14 ngày\n2. Lọc bỏ SCOBY\n3. Thêm nước ép trái cây\n4. Ủ thêm 3 ngày để có ga", 0.70),
                new TrendingDishInfo("Wellness Shot", "1. Xay gừng tươi, nghệ tươi, chanh\n2. Thêm mật ong và tiêu đen\n3. Lọc lấy nước\n4. Uống 30ml/ngày", 0.68),
                new TrendingDishInfo("Sinh tố xanh", "1. Xay rau bina, chuối, táo\n2. Thêm sữa hạnh nhân\n3. Thêm mật ong\n4. Xay nhuyễn với đá", 0.66)
        );
        return fallback.stream().limit(maxResults).collect(Collectors.toList());
    }

    // Heuristic cực nhẹ để dự phòng khi không gọi được API
    private String guessDishName(String title, String desc) {
        String t = (title == null ? "" : title).toLowerCase();
        String d = (desc == null ? "" : desc).toLowerCase();
        String text = t + " " + d;

        if (text.contains("salted") && text.contains("caramel") && text.contains("latte")) return "Salted Caramel Latte";
        if (text.contains("dirty")  && text.contains("matcha")) return "Dirty Matcha";
        if (text.contains("coconut")&& text.contains("coffee")) return "Coconut Coffee";
        if (text.contains("strawberry") && text.contains("latte")) return "Strawberry Latte";
        if (text.contains("matcha") && text.contains("latte")) return "Matcha Latte";
        if (text.contains("caramel") && text.contains("latte")) return "Caramel Latte";
        if (text.contains("latte")) return "Latte";
        if (text.contains("coffee")) return "Coffee";
        if (text.contains("matcha")) return "Matcha";
        return null;
    }

    /**
     * DTO cho thông tin món hot trend từ Gemini
     */
    public static class TrendingDishInfo {
        private final String name;
        private final String basicRecipe;
        private final double trendingScore;

        public TrendingDishInfo(String name, String basicRecipe, double trendingScore) {
            this.name = name;
            this.basicRecipe = basicRecipe;
            this.trendingScore = trendingScore;
        }

        public String getName() { return name; }
        public String getBasicRecipe() { return basicRecipe; }
        public double getTrendingScore() { return trendingScore; }
    }
}
