package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.RecurrencePattern;

import com.kopi.kopi.payload.request.RecurrencePatternRequest;
import com.kopi.kopi.repository.RecurrencePatternRepository;

import com.kopi.kopi.service.IRecurrencePatternService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecurrencePatternServiceImpl implements IRecurrencePatternService {
    private final RecurrencePatternRepository repo;

    public RecurrencePatternServiceImpl(RecurrencePatternRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Map<String, Object> createRecurrencePattern(RecurrencePatternRequest req, Integer adminUserId) {
        // The existing DB table only stores recurrence_type, day_of_week and
        // interval_days.
        // We validate required request fields but only persist supported columns.
        String type = req.getRecurrenceType();
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("recurrenceType is required");
        }
    type = type.toUpperCase();
    String typeLower = type.toLowerCase();

        Integer interval = req.getInterval() == null ? 1 : req.getInterval();
        if (interval < 1)
            throw new IllegalArgumentException("interval phải là số nguyên dương >= 1");

        if ("DAILY".equals(type)) {
            // business rule: DAILY interval must be between 1 and 7 inclusive
            if (interval < 1 || interval > 7)
                throw new IllegalArgumentException("DAILY, interval must be between [1,7]");
            // ensure uniqueness per interval for DAILY: cannot create DAILY pattern with same interval twice
            if (repo.existsByRecurrenceTypeAndIntervalDays(typeLower, interval)) {
                throw new IllegalArgumentException("A pattern with interval " + interval + " already exists");
            }
        }

        if ("WEEKLY".equals(type)) {
            List<String> dow = req.getDayOfWeek();
            if (dow == null || dow.isEmpty())
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một ngày trong tuần cho WEEKLY");
        }

        // create entity with DB-supported fields only
        RecurrencePattern p = new RecurrencePattern();
    // persist recurrence_type in lowercase to match DB check constraint (e.g. 'daily','weekly')
    p.setRecurrenceType(typeLower);
        p.setIntervalDays(interval);
        if (req.getDayOfWeek() != null && !req.getDayOfWeek().isEmpty()) {
            String csv = req.getDayOfWeek().stream().map(String::toUpperCase).collect(Collectors.joining(","));
            p.setDayOfWeek(csv);
        } else {
            p.setDayOfWeek(null);
        }

        RecurrencePattern saved = repo.save(p);

        Map<String, Object> out = new HashMap<>();
        out.put("recurrenceId", saved.getRecurrenceId());
        out.put("recurrenceType", saved.getRecurrenceType());
        out.put("message", "Created");
        return out;
    }
}
