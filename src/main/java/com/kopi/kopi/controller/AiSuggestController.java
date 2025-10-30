package com.kopi.kopi.controller;

import com.kopi.kopi.dto.ai.AiSuggestResponse;
import com.kopi.kopi.dto.ai.AiSuggestionRequest;
import com.kopi.kopi.service.IAiSuggestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apiv1/ai/suggestions")
public class AiSuggestController {
    private final IAiSuggestService svc;

    public AiSuggestController(IAiSuggestService svc) {
        this.svc = svc;
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AiSuggestResponse> search(@RequestBody AiSuggestionRequest req) {
        return ResponseEntity.ok(svc.search(req));
    }
}
