package com.kopi.kopi.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopi.kopi.dto.ai.VideoItem;
import com.kopi.kopi.exception.YouTubeQuotaExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
public class YouTubeClient {
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${ai.youtube.key:${YOUTUBE_API_KEY:}}")
    private String apiKey;

    public List<VideoItem> searchRecentVideos(
            String search,
            int days,
            int maxResults,
            String regionCode,
            String language,
            boolean shortsOnly,
            String videoCategoryId,
            String safeSearch,
            boolean embeddableOnly
    ) {
        return searchVideos(search, days, maxResults, regionCode, language, shortsOnly, videoCategoryId, safeSearch, embeddableOnly, "date");
    }
    
    public List<VideoItem> searchVideos(
            String search,
            int days,
            int maxResults,
            String regionCode,
            String language,
            boolean shortsOnly,
            String videoCategoryId,
            String safeSearch,
            boolean embeddableOnly,
            String order
    ) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                System.err.println("[YT] Missing API key. Set ai.youtube.key or YOUTUBE_API_KEY");
                return List.of();
            }
            // 1) Chuỗi search tự do (giống YouTube)
            String q = (search == null || search.isBlank()) ? "coffee drink" : search.trim();

            int maxSafe  = Math.min(Math.max(1, maxResults), 50);
            
            // 2) Tính publishedAfter nếu days > 0 (phải format UTC với 'Z' cho YouTube API)
            String publishedAfter = null;
            if (days > 0) {
                publishedAfter = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days)
                    .format(DateTimeFormatter.ISO_INSTANT);
            }

            // 3) URL chính với publishedAfter để lọc video gần đây
            String orderParam = (order != null && !order.isBlank()) ? order : "date";
            StringBuilder url = new StringBuilder("https://www.googleapis.com/youtube/v3/search")
                    .append("?part=snippet&type=video&order=").append(orderParam)
                    .append("&maxResults=").append(maxSafe)
                    .append("&q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8))
                    .append("&key=").append(apiKey);
            if (publishedAfter != null) url.append("&publishedAfter=").append(publishedAfter);
            if (regionCode != null && !regionCode.isBlank()) url.append("&regionCode=").append(regionCode);
            if (language != null && !language.isBlank())   url.append("&relevanceLanguage=").append(language);
            if (shortsOnly) url.append("&videoDuration=short");
            if (videoCategoryId != null && !videoCategoryId.isBlank()) url.append("&videoCategoryId=").append(videoCategoryId);
            if (safeSearch != null && !safeSearch.isBlank()) url.append("&safeSearch=").append(safeSearch);
            if (embeddableOnly) url.append("&videoEmbeddable=true");

            System.out.println("[YT] Calling YouTube API with query: " + q);
            System.out.println("[YT] Full URL=" + url);
            ResponseEntity<String> r;
            try {
                r = http.getForEntity(url.toString(), String.class);
                System.out.println("[YT] Response received, status=" + r.getStatusCode());
            } catch (Exception ex) {
                System.err.println("[YT] HTTP Exception: " + ex.getClass().getName() + ": " + ex.getMessage());
                String msg = ex.getMessage() == null ? "" : ex.getMessage();
                
                // Check nếu là quota exceeded
                if (msg.contains("quotaExceeded") || msg.contains("quota")) {
                    String resetTime = calculateQuotaResetTime();
                    System.err.println("[YT] ❌ QUOTA EXCEEDED! Reset time: " + resetTime);
                    throw new YouTubeQuotaExceededException(
                        "YouTube API đã hết lượt tìm kiếm. Vui lòng thử lại sau " + resetTime,
                        resetTime
                    );
                }
                
                // Nếu lỗi vì published_after, thử bỏ tham số này và gọi lại
                if (msg.contains("published_after") || msg.contains("Invalid time format")) {
                    System.err.println("[YT] publishedAfter rejected, retrying without it");
                    StringBuilder url2 = new StringBuilder("https://www.googleapis.com/youtube/v3/search")
                            .append("?part=snippet&type=video&order=date")
                            .append("&maxResults=").append(maxSafe)
                            .append("&q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8))
                            .append("&key=").append(apiKey);
                    if (regionCode != null && !regionCode.isBlank()) url2.append("&regionCode=").append(regionCode);
                    if (language != null && !language.isBlank())   url2.append("&relevanceLanguage=").append(language);
                    if (shortsOnly) url2.append("&videoDuration=short");
                    if (videoCategoryId != null && !videoCategoryId.isBlank()) url2.append("&videoCategoryId=").append(videoCategoryId);
                    if (safeSearch != null && !safeSearch.isBlank()) url2.append("&safeSearch=").append(safeSearch);
                    if (embeddableOnly) url2.append("&videoEmbeddable=true");
                    System.out.println("[YT] URL-retry-noPublishedAfter=" + url2);
                    r = http.getForEntity(url2.toString(), String.class);
                } else {
                    throw ex;
                }
            }

            System.out.println("[YT] status=" + r.getStatusCode().value());
            if (!r.getStatusCode().is2xxSuccessful()) {
                System.err.println("[YT] non-2xx response body: " + r.getBody());
            }

            // 4) Parse items
            List<String> ids = new ArrayList<>();
            Map<String, VideoItem.VideoItemBuilder> map = new LinkedHashMap<>();
            parseSearchItems(om.readTree(r.getBody()), map, ids);
            System.out.println("[YT] Parsed " + ids.size() + " items from first search");

            // Bỏ VI filter để có nhiều kết quả hơn (đã bỏ giới hạn region)

            // 5) Fallback nếu rỗng: bỏ publishedAfter
            if (ids.isEmpty()) {
                System.err.println("[YT] No items from first search. Response body: " + r.getBody());
                StringBuilder url2 = new StringBuilder("https://www.googleapis.com/youtube/v3/search")
                        .append("?part=snippet&type=video&order=date")
                        .append("&maxResults=").append(maxSafe)
                        .append("&q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8))
                        .append("&key=").append(apiKey);
                if (regionCode != null && !regionCode.isBlank()) url2.append("&regionCode=").append(regionCode);
                if (language != null && !language.isBlank())   url2.append("&relevanceLanguage=").append(language);
                if (shortsOnly) url2.append("&videoDuration=short");
                if (videoCategoryId != null && !videoCategoryId.isBlank()) url2.append("&videoCategoryId=").append(videoCategoryId);
                if (safeSearch != null && !safeSearch.isBlank()) url2.append("&safeSearch=").append(safeSearch);
                if (embeddableOnly) url2.append("&videoEmbeddable=true");

                System.out.println("[YT] URL-fallback=" + url2);
                ResponseEntity<String> rFb = http.getForEntity(url2.toString(), String.class);
                if (!rFb.getStatusCode().is2xxSuccessful()) {
                    System.err.println("[YT] fallback non-2xx response body: " + rFb.getBody());
                }
                parseSearchItems(om.readTree(rFb.getBody()), map, ids);
                System.out.println("[YT] Fallback1 parsed " + ids.size() + " items");

                // 5b) Fallback2: nếu vẫn rỗng, nới lỏng thêm (bỏ order/region/lang/safeSearch)
                if (ids.isEmpty()) {
                    StringBuilder url3 = new StringBuilder("https://www.googleapis.com/youtube/v3/search")
                            .append("?part=snippet&type=video")
                            .append("&maxResults=").append(maxSafe)
                            .append("&q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8))
                            .append("&key=").append(apiKey);
                    if (shortsOnly) url3.append("&videoDuration=short");
                    if (videoCategoryId != null && !videoCategoryId.isBlank()) url3.append("&videoCategoryId=").append(videoCategoryId);
                    if (embeddableOnly) url3.append("&videoEmbeddable=true");
                    System.out.println("[YT] URL-fallback2-loose=" + url3);
                    ResponseEntity<String> rLoose = http.getForEntity(url3.toString(), String.class);
                    parseSearchItems(om.readTree(rLoose.getBody()), map, ids);
                }

                // 5c) Fallback3: nếu vẫn rỗng và language=vi, bỏ phần boost tiếng Việt trong q
                if (ids.isEmpty() && "vi".equalsIgnoreCase(language)) {
                    String qNoBoost = q.replace("(Việt Nam OR tiếng Việt OR VN)", "").trim();
                    StringBuilder url4 = new StringBuilder("https://www.googleapis.com/youtube/v3/search")
                            .append("?part=snippet&type=video")
                            .append("&maxResults=").append(maxSafe)
                            .append("&q=").append(URLEncoder.encode(qNoBoost, StandardCharsets.UTF_8))
                            .append("&key=").append(apiKey);
                    if (shortsOnly) url4.append("&videoDuration=short");
                    if (videoCategoryId != null && !videoCategoryId.isBlank()) url4.append("&videoCategoryId=").append(videoCategoryId);
                    if (embeddableOnly) url4.append("&videoEmbeddable=true");
                    System.out.println("[YT] URL-fallback3-remove-vi-boost=" + url4);
                    ResponseEntity<String> rNoBoost = http.getForEntity(url4.toString(), String.class);
                    parseSearchItems(om.readTree(rNoBoost.getBody()), map, ids);
                }

                if (ids.isEmpty()) return List.of();
            }

            // 6) Lấy statistics + contentDetails (không làm fail toàn hàm)
            try {
                String vidsUrl = "https://www.googleapis.com/youtube/v3/videos"
                        + "?part=statistics,contentDetails&id=" + String.join(",", ids)
                        + "&key=" + apiKey;
                ResponseEntity<String> r2 = http.getForEntity(vidsUrl, String.class);
                JsonNode root2 = om.readTree(r2.getBody());
                for (JsonNode it : root2.path("items")) {
                    String vid = it.path("id").asText();
                    JsonNode st = it.path("statistics");
                    JsonNode cd = it.path("contentDetails");
                    VideoItem.VideoItemBuilder b = map.get(vid);
                    if (b != null) {
                        b.viewCount(st.path("viewCount").asLong(0));
                        b.likeCount(st.path("likeCount").asLong(0));
                        long dur = parseIso8601DurationSeconds(cd.path("duration").asText(""));
                        if (dur > 0) b.durationSeconds(dur);
                        if (dur > 0 && dur <= 60) {
                            b.videoUrl("https://www.youtube.com/shorts/" + vid);
                        }
                    }
                }
            } catch (Exception ignore) {}

            // 7) Nếu yêu cầu shortsOnly, lọc thêm <= 60s
            return map.values().stream()
                    .map(VideoItem.VideoItemBuilder::build)
                    .filter(v -> !shortsOnly || (v.getDurationSeconds() != null && v.getDurationSeconds() <= 60))
                    .toList();
        } catch (Exception e) {
            System.err.println("[YT] Exception: " + e.getMessage());
            return List.of();
        }
    }
    private void parseSearchItems(JsonNode root, Map<String, VideoItem.VideoItemBuilder> map, List<String> ids) {
        for (JsonNode it : root.path("items")) {
            String vid = it.path("id").path("videoId").asText(null);
            if (vid == null) continue;
            JsonNode sn = it.path("snippet");

            OffsetDateTime publishedAt = null;
            String publishedAtStr = sn.path("publishedAt").asText(null);
            if (publishedAtStr != null && !publishedAtStr.isBlank()) {
                try { publishedAt = OffsetDateTime.parse(publishedAtStr); } catch (Exception ignore) {}
            }

            // Lấy thumbnail URL (high quality)
            String thumbnailUrl = sn.path("thumbnails").path("high").path("url").asText("");
            if (thumbnailUrl.isBlank()) {
                thumbnailUrl = "https://i.ytimg.com/vi/" + vid + "/hqdefault.jpg";
            }

            map.put(vid, VideoItem.builder()
                    .videoId(vid)
                    .title(sn.path("title").asText(""))
                    .description(sn.path("description").asText(""))
                    .thumbnailUrl(thumbnailUrl)
                    .channelTitle(sn.path("channelTitle").asText(""))
                    .publishedAt(publishedAt)
                    .videoUrl("https://www.youtube.com/watch?v=" + vid));
            ids.add(vid);
        }
    }

    private long parseIso8601DurationSeconds(String iso) {
        if (iso == null || iso.isBlank()) return 0L;
        // Đơn giản: parse PT#M#S | PT#S | PT#H#M#S
        long hours = 0, minutes = 0, seconds = 0;
        try {
            String s = iso;
            if (!s.startsWith("PT")) return 0L;
            s = s.substring(2);
            int hIdx = s.indexOf('H');
            if (hIdx >= 0) { hours = Long.parseLong(s.substring(0, hIdx)); s = s.substring(hIdx + 1); }
            int mIdx = s.indexOf('M');
            if (mIdx >= 0) { minutes = Long.parseLong(s.substring(0, mIdx)); s = s.substring(mIdx + 1); }
            int secIdx = s.indexOf('S');
            if (secIdx >= 0) { seconds = Long.parseLong(s.substring(0, secIdx)); }
            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Tính thời gian còn lại đến khi YouTube API quota reset
     * YouTube API quota reset vào 12:00 AM (midnight) Pacific Time
     */
    private String calculateQuotaResetTime() {
        try {
            // Lấy thời gian hiện tại ở Pacific Time
            ZoneId pacificZone = ZoneId.of("America/Los_Angeles");
            ZonedDateTime nowPacific = ZonedDateTime.now(pacificZone);
            
            // Tính midnight tiếp theo (12:00 AM Pacific)
            ZonedDateTime nextMidnight = nowPacific.toLocalDate()
                .plusDays(1)
                .atStartOfDay(pacificZone);
            
            // Tính khoảng thời gian còn lại
            Duration remaining = Duration.between(nowPacific, nextMidnight);
            long hours = remaining.toHours();
            long minutes = remaining.toMinutes() % 60;
            
            // Format thành chuỗi dễ đọc
            if (hours > 0) {
                return hours + " giờ " + minutes + " phút";
            } else {
                return minutes + " phút";
            }
        } catch (Exception e) {
            return "24 giờ";
        }
    }

    private boolean looksVietnamese(VideoItem v) {
        String t = (v.getTitle() == null ? "" : v.getTitle()) + "\n" + (v.getDescription() == null ? "" : v.getDescription());
        String lower = t.toLowerCase(Locale.ROOT);
        // Heuristic: chứa dấu tiếng Việt hoặc một số từ khoá phổ biến
        boolean hasDiacritics = lower.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*");
        boolean hasCommonWords = lower.matches(".*\\b(cà phê|trà|matcha|việt nam|tiếng việt|pha chế|công thức|hottrend|đồ uống ngon|đồ uống)\\b.*");
        return hasDiacritics || hasCommonWords;
    }

}
