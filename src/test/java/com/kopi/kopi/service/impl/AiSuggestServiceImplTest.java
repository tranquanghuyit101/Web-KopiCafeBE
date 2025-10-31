package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.ai.*;
import com.kopi.kopi.service.IAiSuggestService;
import com.kopi.kopi.service.ai.GeminiClient;
import com.kopi.kopi.service.ai.YouTubeClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSuggestServiceImplTest {

        @Mock
        private YouTubeClient yt;
        @Mock
        private GeminiClient gemini;

        private AiSuggestServiceImpl service;

        @BeforeEach
        void setUp() {
                service = new AiSuggestServiceImpl(yt, gemini);
                // default region đã có @Value nhưng không ảnh hưởng tới test vì mình truyền
                // tham số cụ thể vào yt.*
        }

        // ---------- Helpers ----------
        private GeminiClient.TrendingDishInfo dishInfo(String name, double score, String recipe) {
                GeminiClient.TrendingDishInfo info = mock(GeminiClient.TrendingDishInfo.class);
                when(info.getName()).thenReturn(name);
                when(info.getTrendingScore()).thenReturn(score);
                when(info.getBasicRecipe()).thenReturn(recipe);
                return info;
        }

        private VideoItem video(String id, String title, String desc, long views, long likes,
                        OffsetDateTime published) {
                return VideoItem.builder()
                                .videoId(id)
                                .title(title)
                                .description(desc)
                                .viewCount(views)
                                .likeCount(likes)
                                .publishedAt(published)
                                .videoUrl("https://youtube.com/watch?v=" + id)
                                .thumbnailUrl("thumb")
                                .channelTitle("ch")
                                .durationSeconds(60L)
                                .build();
        }

        // =================== groupedByDish ===================

        @Test
        void should_GroupRankAndAnnotateVideos_When_GeminiAndYouTubeReturnData() {
                // Given
                var req = new AiSuggestionRequest();
                req.setDays(30);
                req.setMaxResults(10);

                var d1 = dishInfo("Cà phê muối", 8.0, "R1");
                var d2 = dishInfo("Matcha latte", 7.0, "R2");
                when(gemini.findHotTrendDishes(anyInt(), anyInt())).thenReturn(List.of(d1, d2));

                // YouTube: 2 video/ món, đều hợp lệ (có từ "cách làm")
                var now = OffsetDateTime.now();
                when(yt.searchRecentVideos(eq("Cà phê muối"), anyInt(), anyInt(), anyString(), anyString(),
                                anyBoolean(), isNull(), anyString(), anyBoolean()))
                                .thenReturn(List.of(
                                                video("v1", "Cách làm Cà phê muối ngon", "desc", 20000, 1000,
                                                                now.minusDays(1)),
                                                video("v2", "Cách pha Cà phê muối chuẩn", "desc", 15000, 800,
                                                                now.minusDays(2))));
                when(yt.searchRecentVideos(eq("Matcha latte"), anyInt(), anyInt(), anyString(), anyString(),
                                anyBoolean(), isNull(), anyString(), anyBoolean()))
                                .thenReturn(List.of(
                                                video("v3", "Cách làm Matcha latte béo", "desc", 18000, 900,
                                                                now.minusDays(1)),
                                                video("v4", "How to make matcha latte", "desc", 16000, 700,
                                                                now.minusDays(3))));

                // When
                DishTrendsResponse res = service.groupedByDish(req);

                // Then
                assertThat(res.getData()).hasSize(2);
                // sắp xếp theo topScore giảm dần (khó xác định tuyệt đối, nhưng kiểm tra có
                // topScore>0 và videos sorted theo viralScore)
                for (DishGroup g : res.getData()) {
                        assertThat(g.getTopScore()).isGreaterThan(0.0);
                        assertThat(g.getVideos()).isNotEmpty();
                        // đã annotate
                        for (VideoItem v : g.getVideos()) {
                                assertThat(v.getDishName()).isEqualTo(g.getName());
                                assertThat(v.getDishKey()).isEqualTo(g.getKey());
                                assertThat(v.getViralScore()).isNotNull().isGreaterThan(0.0);
                        }
                        // videos đã sort theo viralScore giảm dần
                        var list = g.getVideos();
                        for (int i = 1; i < list.size(); i++) {
                                assertThat(list.get(i - 1).getViralScore())
                                                .isGreaterThanOrEqualTo(list.get(i).getViralScore());
                        }
                }
                Map<String, Object> meta = res.getMeta();
                assertThat(meta.get("totalVideos")).isInstanceOf(Integer.class);
                assertThat((int) meta.get("totalVideos")).isEqualTo(
                                res.getData().stream().mapToInt(DishGroup::getTotalVideos).sum());
                verify(gemini).findHotTrendDishes(anyInt(), anyInt());
        }

        @Test
        void should_UseFallbackDishes_When_GeminiThrows_AndReturnGroupsEvenWithNoVideos() {
                // Given
                var req = new AiSuggestionRequest();
                req.setDays(15);
                req.setMaxResults(5);
                when(gemini.findHotTrendDishes(anyInt(), anyInt())).thenThrow(new RuntimeException("boom"));
                var fb = dishInfo("Dirty Matcha", 10.0, "fb recipe");
                when(gemini.getFallbackTrendingDishes(anyInt())).thenReturn(List.of(fb));
                // YouTube rỗng
                when(yt.searchRecentVideos(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(),
                                any(), anyString(), anyBoolean()))
                                .thenReturn(List.of());
                when(yt.searchVideos(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(), any(),
                                anyString(), anyBoolean(), anyString()))
                                .thenReturn(List.of());

                // When
                DishTrendsResponse res = service.groupedByDish(req);

                // Then
                assertThat(res.getData()).hasSize(1);
                DishGroup g = res.getData().get(0);
                assertThat(g.getName()).isEqualTo("Dirty Matcha");
                assertThat(g.getBasicRecipe()).isEqualTo("fb recipe");
                assertThat(g.getTotalVideos()).isZero();
                // rating dùng trendingScore/2 = 5.0
                assertThat(g.getRating()).isEqualTo(5.0);
                // topScore từ Gemini (10->100)
                assertThat(g.getTopScore()).isEqualTo(100.0);
                verify(gemini).getFallbackTrendingDishes(anyInt());
        }

        @Test
        void should_FilterOutIrrelevantOrForeignLanguageVideos() {
                // Given
                var req = new AiSuggestionRequest();
                req.setDays(30);
                req.setMaxResults(5);
                var d = dishInfo("Trà sữa", 6.0, "R");
                when(gemini.findHotTrendDishes(anyInt(), anyInt())).thenReturn(List.of(d));

                var now = OffsetDateTime.now();
                var jp = video("x1", "抹茶ラテ 作り方 (matcha latte)", "Japanese title", 5000, 200, now.minusDays(1)); // chứa
                                                                                                                // kana
                                                                                                                // → bị
                                                                                                                // reject
                var ok = video("x2", "Cách làm Trà sữa trân châu tại nhà", "recipe", 10000, 500, now.minusDays(1));
                when(yt.searchRecentVideos(eq("Trà sữa"), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(),
                                any(), anyString(), anyBoolean()))
                                .thenReturn(List.of(jp, ok));

                // When
                DishTrendsResponse res = service.groupedByDish(req);

                // Then
                DishGroup g = res.getData().get(0);
                assertThat(g.getTotalVideos()).isEqualTo(1);
                assertThat(g.getVideos()).extracting(VideoItem::getVideoId).containsExactly("x2");
        }

        @Test
        void should_PopulateMetaFields_And_SortVideosByViralScoreDesc() {
                // Given
                var req = new AiSuggestionRequest();
                req.setDays(7);
                req.setMaxResults(6);
                var d = dishInfo("Cà phê trứng", 7.0, "R");
                when(gemini.findHotTrendDishes(anyInt(), anyInt())).thenReturn(List.of(d));

                var now = OffsetDateTime.now();
                var v1 = video("a", "Cách làm Cà phê trứng", "desc", 30000, 1500, now.minusHours(12));
                var v2 = video("b", "Cách làm Cà phê trứng chuẩn", "desc", 10000, 200, now.minusHours(2)); // ít
                                                                                                           // view/like
                                                                                                           // nhưng rất
                                                                                                           // mới
                when(yt.searchRecentVideos(eq("Cà phê trứng"), anyInt(), anyInt(), anyString(), anyString(),
                                anyBoolean(), any(), anyString(), anyBoolean()))
                                .thenReturn(List.of(v1, v2));

                // When
                DishTrendsResponse res = service.groupedByDish(req);

                // Then
                Map<String, Object> meta = res.getMeta();
                assertThat(meta).containsKeys("days", "max", "totalVideos", "grouped", "tookMs");
                assertThat(meta.get("days")).isEqualTo(7);
                assertThat(meta.get("max")).isEqualTo(6);
                assertThat((int) meta.get("grouped")).isEqualTo(1);
                // videos đã sort theo viralScore desc
                DishGroup g = res.getData().get(0);
                assertThat(g.getVideos()).hasSize(2);
                assertThat(g.getVideos().get(0).getViralScore())
                                .isGreaterThanOrEqualTo(g.getVideos().get(1).getViralScore());
        }

        // =================== search ===================

        @Test
        void should_MapDishGroupsToSuggestedDrinks_InSearch() {
                // Given
                var req = new AiSuggestionRequest();
                req.setDays(30);
                req.setMaxResults(10);
                // Spy service để stub groupedByDish
                AiSuggestServiceImpl spyService = Mockito.spy(new AiSuggestServiceImpl(yt, gemini));

                List<VideoItem> vids = List.of(
                                video("v1", "Cách làm Dirty Matcha", "desc", 1000, 50,
                                                OffsetDateTime.now().minusDays(1)));
                DishGroup g1 = DishGroup.builder()
                                .key("dirty-matcha").name("Dirty Matcha").basicRecipe("mix")
                                .totalVideos(1).topScore(88.0).rating(4.5).videos(vids).build();
                DishTrendsResponse trends = DishTrendsResponse.builder()
                                .data(List.of(g1))
                                .meta(Map.of("tookMs", 123L, "totalVideos", 1))
                                .build();

                doReturn(trends).when(spyService).groupedByDish(any(AiSuggestionRequest.class));

                // When
                AiSuggestResponse resp = spyService.search(req);

                // Then
                assertThat(resp.getItems()).hasSize(1);
                var item = resp.getItems().get(0);
                assertThat(item.getName()).isEqualTo("Dirty Matcha");
                assertThat(item.getScore()).isEqualTo(88.0);
                assertThat(item.getFromVideos()).containsExactlyElementsOf(vids);
                assertThat(item.getTags()).contains("hot-trend", "vietnam");
                assertThat(resp.getModel()).isEqualTo("trend-aggregation");
                assertThat(resp.getTookMs()).isEqualTo(123L);
                assertThat(resp.getVideoFetched()).isEqualTo(1);
                verify(spyService).groupedByDish(any(AiSuggestionRequest.class));
        }

        @Test
        void should_ReturnEmptyItems_When_GroupedByDishReturnsEmpty() {
                // Given
                var req = new AiSuggestionRequest();
                AiSuggestServiceImpl spyService = Mockito.spy(new AiSuggestServiceImpl(yt, gemini));
                DishTrendsResponse empty = DishTrendsResponse.builder()
                                .data(List.of())
                                .meta(Map.of("tookMs", 0L, "totalVideos", 0))
                                .build();
                doReturn(empty).when(spyService).groupedByDish(any());

                // When
                AiSuggestResponse resp = spyService.search(req);

                // Then
                assertThat(resp.getItems()).isEmpty();
                assertThat(resp.getVideoFetched()).isEqualTo(0);
                assertThat(resp.getTookMs()).isEqualTo(0L);
        }

        @Test
        void should_KeepAllGroups_AsItems_InSearch() {
                // Given
                var req = new AiSuggestionRequest();
                AiSuggestServiceImpl spyService = Mockito.spy(new AiSuggestServiceImpl(yt, gemini));

                DishGroup g1 = DishGroup.builder().key("a").name("A").topScore(70.0).videos(List.of()).totalVideos(0)
                                .build();
                DishGroup g2 = DishGroup.builder().key("b").name("B").topScore(80.0).videos(List.of()).totalVideos(0)
                                .build();
                DishTrendsResponse trends = DishTrendsResponse.builder()
                                .data(List.of(g1, g2))
                                .meta(Map.of("tookMs", 10L, "totalVideos", 0))
                                .build();
                doReturn(trends).when(spyService).groupedByDish(any());

                // When
                AiSuggestResponse resp = spyService.search(req);

                // Then
                assertThat(resp.getItems()).hasSize(2);
                assertThat(resp.getItems()).extracting(SuggestedDrink::getName)
                                .containsExactly("A", "B");
        }

        @Test
        void should_FillDefaultTags_AndModel_InSearch() {
                // Given
                var req = new AiSuggestionRequest();
                AiSuggestServiceImpl spyService = Mockito.spy(new AiSuggestServiceImpl(yt, gemini));
                DishGroup g = DishGroup.builder().key("x").name("X").topScore(10.0).videos(List.of()).totalVideos(0)
                                .build();
                DishTrendsResponse trends = DishTrendsResponse.builder()
                                .data(List.of(g))
                                .meta(Map.of("tookMs", 1L, "totalVideos", 0))
                                .build();
                doReturn(trends).when(spyService).groupedByDish(any());

                // When
                AiSuggestResponse resp = spyService.search(req);

                // Then
                assertThat(resp.getModel()).isEqualTo("trend-aggregation");
                assertThat(resp.getItems().get(0).getTags()).contains("hot-trend", "vietnam");
        }
}
