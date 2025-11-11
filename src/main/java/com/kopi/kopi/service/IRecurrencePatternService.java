package com.kopi.kopi.service;

import com.kopi.kopi.payload.request.RecurrencePatternRequest;

import java.util.Map;

public interface IRecurrencePatternService {
    Map<String, Object> createRecurrencePattern(RecurrencePatternRequest req, Integer adminUserId);
}
