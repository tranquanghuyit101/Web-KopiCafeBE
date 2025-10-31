package com.kopi.kopi.controller.ai;

import com.kopi.kopi.dto.ai.AiSuggestionRequest;
import com.kopi.kopi.dto.ai.DishTrendsResponse;
import com.kopi.kopi.exception.YouTubeQuotaExceededException;
import com.kopi.kopi.service.impl.AiSuggestServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/apiv1/trends")
@RequiredArgsConstructor
public class TrendsController {

    private final AiSuggestServiceImpl svc;

    @GetMapping("/dishes")
    public ResponseEntity<?> dishes(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(required = false) Boolean shortsOnly
    ) {
        try {
            AiSuggestionRequest req = new AiSuggestionRequest();
            req.setDays(days);
            req.setMaxResults(maxResults);
            req.setShortsOnly(shortsOnly);
            return ResponseEntity.ok(svc.groupedByDish(req));
        } catch (YouTubeQuotaExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "QUOTA_EXCEEDED",
                "message", e.getMessage(),
                "resetTime", e.getResetTime()
            ));
        }
    }
}
