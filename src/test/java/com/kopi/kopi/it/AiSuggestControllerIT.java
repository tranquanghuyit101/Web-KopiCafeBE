package com.kopi.kopi.it;

import com.kopi.kopi.controller.AiSuggestController;
import com.kopi.kopi.dto.ai.AiSuggestResponse;
import com.kopi.kopi.dto.ai.AiSuggestionRequest;
import com.kopi.kopi.dto.ai.SuggestedDrink;
import com.kopi.kopi.dto.ai.VideoItem;
import com.kopi.kopi.service.IAiSuggestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kopi.kopi.security.JwtAuthenticationFilter;
import com.kopi.kopi.security.JwtTokenProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = AiSuggestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiSuggestControllerIT {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IAiSuggestService aiSuggestService;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /apiv1/ai/suggestions/search -> 200 and returns items")
    void search_ok() throws Exception {
        VideoItem video = VideoItem.builder()
                .videoId("abc")
                .title("Salted Caramel Latte")
                .videoUrl("https://youtu.be/abc")
                .publishedAt(OffsetDateTime.now())
                .build();

        SuggestedDrink drink = SuggestedDrink.builder()
                .name("Salted Caramel Latte")
                .reason("Trending in Vietnam")
                .score(92.0)
                .basicRecipe("Espresso, caramel syrup, milk, salt")
                .fromVideos(List.of(video))
                .build();

        AiSuggestResponse response = AiSuggestResponse.builder()
                .items(List.of(drink))
                .model("gemini-1.5-flash")
                .tookMs(12L)
                .videoFetched(1)
                .build();

        when(aiSuggestService.search(any(AiSuggestionRequest.class))).thenReturn(response);

        String body = "{\"days\":7,\"maxResults\":8}";

        mvc.perform(post("/apiv1/ai/suggestions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Salted Caramel Latte"))
                .andExpect(jsonPath("$.tookMs").value(12))
                .andExpect(jsonPath("$.videoFetched").value(1));
    }
}
