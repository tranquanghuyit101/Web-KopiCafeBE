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
            Role: Expert barista & drink analyzer
            
            Task: Analyze each YouTube video and extract:
            1. SPECIFIC DRINK NAME (dishName)
            2. BASIC RECIPE (basicRecipe)
            
            CRITICAL NAMING RULES:
            - MUST be ULTRA-SPECIFIC. Extract the EXACT variant from title/description:
              * "Cold Brew Coffee", "Iced Latte", "Cappuccino", "Espresso", "Americano" → ALL DIFFERENT
              * "Vietnamese Egg Coffee", "Salted Coffee", "Coconut Coffee" → ALL DIFFERENT
              * "Brown Sugar Milk Tea", "Matcha Milk Tea", "Cheese Foam Milk Tea" → ALL DIFFERENT
              * "Dirty Matcha", "Iced Matcha Latte", "Matcha Frappe" → ALL DIFFERENT
            
            - FORBIDDEN GENERIC NAMES (AUTO-REJECT):
              * "Coffee" ❌ → must be "Cold Brew Coffee", "Iced Coffee", etc.
              * "Milk Tea" ❌ → must be "Brown Sugar Milk Tea", "Boba Milk Tea", etc.
              * "Matcha" ❌ → must be "Matcha Latte", "Iced Matcha", etc.
              * "Tea" ❌ → must be specific tea type
            
            - If title has specific name → USE IT EXACTLY
            - If title only says "coffee recipe" without variant → dishName = null
            
            RECIPE RULES:
            - MANDATORY: Must provide recipe (infer from title/desc if needed)
            - Format: "1. [Step 1]\\n2. [Step 2]\\n3. [Step 3]"
            - Examples:
              * "1. Brew 2 espresso shots\\n2. Add 1 tsp salt\\n3. Froth with milk"
              * "1. Mix matcha powder with hot water\\n2. Add milk\\n3. Pour over ice"
            - If video is NOT a recipe/tutorial → dishName = null
            
            CONFIDENCE:
            - 0.7-1.0: Specific name clearly stated
            - 0.4-0.6: Can infer specific variant
            - < 0.4: Too vague → dishName = null
            
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
}
