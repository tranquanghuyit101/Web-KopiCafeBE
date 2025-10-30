package com.kopi.kopi.service;

import com.kopi.kopi.dto.ai.AiSuggestResponse;
import com.kopi.kopi.dto.ai.AiSuggestionRequest;

public interface IAiSuggestService {
    AiSuggestResponse search(AiSuggestionRequest req);
}
