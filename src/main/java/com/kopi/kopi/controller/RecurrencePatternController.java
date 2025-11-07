package com.kopi.kopi.controller;

import com.kopi.kopi.payload.request.RecurrencePatternRequest;
import com.kopi.kopi.repository.RecurrencePatternRepository;
import com.kopi.kopi.service.IRecurrencePatternService;
import com.kopi.kopi.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RecurrencePatternController {
    private final IRecurrencePatternService service;
    private final RecurrencePatternRepository repo;

    public RecurrencePatternController(IRecurrencePatternService service, RecurrencePatternRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/admin/recurrence-patterns")
    public ResponseEntity<Map<String, Object>> create(@RequestBody RecurrencePatternRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer adminUserId = null;
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        Map<String, Object> out = service.createRecurrencePattern(req, adminUserId);
        return ResponseEntity.status(201).body(out);
    }

    // Validate whether a recurrence pattern of given type/interval can be created
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apiv1/admin/recurrence-patterns/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam String recurrenceType,
            @RequestParam(required = false) Integer interval) {
        Map<String, Object> out = new HashMap<>();
        if (recurrenceType == null || recurrenceType.isBlank()) {
            out.put("canCreate", false);
            out.put("reason", "recurrenceType required");
            return ResponseEntity.badRequest().body(out);
        }
        String type = recurrenceType.toUpperCase();
        if ("DAILY".equals(type)) {
            if (interval == null) {
                out.put("canCreate", false);
                out.put("reason", "interval required for DAILY");
                return ResponseEntity.badRequest().body(out);
            }
            boolean exists = repo.existsByRecurrenceTypeAndIntervalDays("daily", interval);
            out.put("canCreate", !exists);
            if (exists)
                out.put("reason", "sample already exists");
            return ResponseEntity.ok(out);
        }

        // for other types, simple validation
        out.put("canCreate", true);
        return ResponseEntity.ok(out);
    }

    // Public endpoint used by FE to list existing patterns
    @GetMapping("/apiv1/recurrence-patterns")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> listPatterns() {
        var list = repo.findAll();
        var out = list.stream().map(p -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("recurrenceId", p.getRecurrenceId());
            m.put("recurrenceType", p.getRecurrenceType());
            m.put("dayOfWeek", p.getDayOfWeek());
            m.put("intervalDays", p.getIntervalDays());
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/apiv1/admin/recurrence-patterns/{id}")
    public ResponseEntity<java.util.Map<String, Object>> updatePattern(@PathVariable Integer id,
        @RequestBody RecurrencePatternRequest req) {
        var maybe = repo.findById(id);
        if (maybe.isEmpty())
            return ResponseEntity.notFound().build();

        var existing = maybe.get();
        String rType = req.getRecurrenceType();
        if (rType == null || rType.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "recurrenceType required"));
        }
        String type = rType.toUpperCase();
        if ("DAILY".equals(type)) {
            Integer interval = req.getInterval();
            if (interval == null || interval < 1)
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "intervalDays must be >= 1"));
            if (interval > 7)
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "intervalDays for DAILY cannot exceed 7"));

            // check duplicates (exclude self)
            var dup = repo.findAll().stream()
                    .filter(p -> "daily".equals(p.getRecurrenceType()) && p.getIntervalDays() != null
                            && p.getIntervalDays().equals(interval) && !p.getRecurrenceId().equals(id))
                    .findAny();
            if (dup.isPresent())
                return ResponseEntity.status(400).body(java.util.Map.of("message", "sample already exists"));

            existing.setRecurrenceType("daily");
            existing.setIntervalDays(interval);
            existing.setDayOfWeek(null);
            repo.save(existing);
            return ResponseEntity.ok(java.util.Map.of("message", "Updated", "recurrenceId", existing.getRecurrenceId()));
        } else if ("WEEKLY".equals(type)) {
            java.util.List<String> days = req.getDayOfWeek();
            if (days == null || days.isEmpty())
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "dayOfWeek required for WEEKLY"));
            String day = days.get(0);
            String norm = normalizeDayToken(day);
            if (norm == null)
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "invalid dayOfWeek"));

            var dup = repo.findAll().stream()
                    .filter(p -> "weekly".equals(p.getRecurrenceType()) && p.getDayOfWeek() != null
                            && p.getDayOfWeek().equalsIgnoreCase(norm) && !p.getRecurrenceId().equals(id))
                    .findAny();
            if (dup.isPresent())
                return ResponseEntity.status(400).body(java.util.Map.of("message", "sample already exists"));

            existing.setRecurrenceType("weekly");
            existing.setDayOfWeek(norm);
            existing.setIntervalDays(null);
            repo.save(existing);
            return ResponseEntity.ok(java.util.Map.of("message", "Updated", "recurrenceId", existing.getRecurrenceId()));
        }

        return ResponseEntity.badRequest().body(java.util.Map.of("message", "unsupported recurrenceType"));
    }

    private static String normalizeDayToken(String token) {
        if (token == null)
            return null;
        String t = token.trim().toLowerCase();
        return switch (t) {
        case "mon", "monday" -> "Mon";
        case "tue", "tues", "tuesday" -> "Tue";
        case "wed", "weds", "wednesday" -> "Wed";
        case "thu", "thur", "thurs", "thursday" -> "Thu";
        case "fri", "friday" -> "Fri";
        case "sat", "saturday" -> "Sat";
        case "sun", "sunday" -> "Sun";
        default -> null;
        };
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/apiv1/admin/recurrence-patterns/{id}")
    public ResponseEntity<java.util.Map<String, Object>> deletePattern(@PathVariable Integer id) {
        try {
            if (!repo.existsById(id))
                return ResponseEntity.notFound().build();
            repo.deleteById(id);
            return ResponseEntity.ok(java.util.Map.of("message", "Deleted"));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return ResponseEntity.status(400)
                    .body(java.util.Map.of("message", "Cannot delete pattern due to existing references"));
        }
    }
}
