package com.kopi.kopi.controller.ai;

import com.kopi.kopi.dto.ai.AiSuggestionRequest;
import com.kopi.kopi.dto.ai.DishTrendsResponse;
import com.kopi.kopi.service.impl.AiSuggestServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apiv1/trends")
@RequiredArgsConstructor
public class TrendsController {

    private final AiSuggestServiceImpl svc;

    @GetMapping("/dishes")
    public DishTrendsResponse dishes(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(required = false) Boolean shortsOnly
    ) {
        AiSuggestionRequest req = new AiSuggestionRequest();
        req.setDays(days);
        req.setMaxResults(maxResults);
        req.setShortsOnly(shortsOnly);
        return svc.groupedByDish(req);
    }
}
