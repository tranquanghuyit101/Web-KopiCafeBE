package com.kopi.kopi.controller;

import com.kopi.kopi.entity.WorkSchedule;
import com.kopi.kopi.repository.EmployeeShiftRepository;
import com.kopi.kopi.repository.RecurrencePatternRepository;
import com.kopi.kopi.repository.WorkScheduleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.kopi.kopi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
public class WorkScheduleGenerationController {
    private final WorkScheduleRepository workScheduleRepository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final UserRepository userRepository;
    private final RecurrencePatternRepository recurrencePatternRepository;
    private final com.kopi.kopi.repository.WorkScheduleGenerationRepository workScheduleGenerationRepository;
    // jdbcTemplate was previously used to detect the presence of the generation
    // history table; persistence is disabled so this field is no longer needed.
    // Keep the field for compatibility with constructor injection but do not use
    // it.
    private final JdbcTemplate jdbcTemplate;

    public WorkScheduleGenerationController(WorkScheduleRepository workScheduleRepository,
            EmployeeShiftRepository employeeShiftRepository,
            UserRepository userRepository,
            RecurrencePatternRepository recurrencePatternRepository,
            com.kopi.kopi.repository.WorkScheduleGenerationRepository workScheduleGenerationRepository,
            JdbcTemplate jdbcTemplate) {
        this.workScheduleRepository = workScheduleRepository;
        this.employeeShiftRepository = employeeShiftRepository;
        this.userRepository = userRepository;
        this.recurrencePatternRepository = recurrencePatternRepository;
        this.workScheduleGenerationRepository = workScheduleGenerationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class GenerateRequest {
        public String name;
        public String description;
        public String anchorDate; // YYYY-MM-DD
        public String startDate;
        public String endDate;
        public String type; // DAILY or WEEKLY
        public Integer interval; // for DAILY: every N days
        public java.util.List<String> daysOfWeek; // for WEEKLY: tokens like MON,TUE...
        public Integer recurrenceId; // optional: reference to an existing recurrence pattern
        public Boolean preview = false;
        public Boolean overwrite = false;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/admin/work-schedules/generate-from-pattern")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        // basic validation
        if (req.anchorDate == null || req.startDate == null || req.endDate == null || req.type == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "anchorDate,startDate,endDate,type are required"));

        LocalDate anchor = LocalDate.parse(req.anchorDate);
        LocalDate start = LocalDate.parse(req.startDate);
        LocalDate end = LocalDate.parse(req.endDate);
        if (start.isAfter(end))
            return ResponseEntity.badRequest().body(Map.of("message", "startDate must be before endDate"));

        List<LocalDate> candidates = computeCandidates(req.type, anchor, start, end, req.interval, req.daysOfWeek);

        java.time.LocalDate today = java.time.LocalDate.now();

        if (req.preview != null && req.preview) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (LocalDate d : candidates) {
                // treat today as non-creatable as well: "past or present" should be skipped
                boolean isPast = !d.isAfter(today);
                boolean hasConflict = !employeeShiftRepository.findByShiftDate(d).isEmpty();
                rows.add(Map.of("date", d.toString(), "isPast", isPast, "hasConflict", hasConflict));
            }
            return ResponseEntity.ok(Map.of("totalCount", rows.size(), "candidates", rows));
        }

        // load anchor shifts once (we'll copy these to other dates)
        List<com.kopi.kopi.entity.EmployeeShift> anchorShifts = employeeShiftRepository.findByShiftDate(anchor);

        // Perform generation: create or update work_schedules and copy anchor shifts
        // where allowed
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> conflicts = new ArrayList<>();

        // resolve admin user id from security context when available
        Integer adminUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }

        // Evaluate per-date decisions first so we can create a single WorkSchedule
        // spanning the entire requested range if we will create any shifts.
        Map<LocalDate, Boolean> willCreate = new LinkedHashMap<>();
        boolean anyWillCreate = false;
        if (anchorShifts == null || anchorShifts.isEmpty()) {
            // nothing to create at all
            for (LocalDate d : candidates) {
                willCreate.put(d, false);
            }
        } else {
            for (LocalDate d : candidates) {
                if (!d.isAfter(today)) {
                    willCreate.put(d, false);
                    continue;
                }
                List<com.kopi.kopi.entity.EmployeeShift> existing = employeeShiftRepository.findByShiftDate(d);
                boolean will;
                if (Boolean.TRUE.equals(req.overwrite)) {
                    will = true;
                } else {
                    will = (existing == null || existing.isEmpty());
                }
                willCreate.put(d, will);
                if (will)
                    anyWillCreate = true;
            }
        }

        if (!anyWillCreate) {
            // nothing to change
            Map<String, Object> outNoChange = new HashMap<>();
            outNoChange.put("created", 0);
            outNoChange.put("updated", 0);
            outNoChange.put("skipped", candidates.size());
            outNoChange.put("message", "No date to change");
            java.util.List<String> candList = new java.util.ArrayList<>();
            for (LocalDate dd : candidates)
                candList.add(dd.toString());
            outNoChange.put("candidates", candList);
            outNoChange.put("anchorShiftCount", anchorShifts != null ? anchorShifts.size() : 0);
            return ResponseEntity.ok(outNoChange);
        }

        // Create a single WorkSchedule for the whole requested range
        WorkSchedule spanWs = WorkSchedule.builder()
                .startDate(start)
                .endDate(end)
                .name(req.name != null ? req.name : ("Generated: " + start.toString() + ".." + end.toString()))
                .description(req.description)
                .createdAt(LocalDateTime.now())
                .build();
        if (adminUserId != null)
            userRepository.findById(adminUserId).ifPresent(spanWs::setCreatedByUser);
        if (req.recurrenceId != null) {
            recurrencePatternRepository.findById(req.recurrenceId).ifPresent(spanWs::setRecurrencePattern);
        }
        spanWs = workScheduleRepository.save(spanWs);
        created++;

        // track old work schedules we modified so we can attempt to delete them if
        // empty
        Set<WorkSchedule> touchedOldWs = new HashSet<>();

        for (LocalDate d : candidates) {
            if (!Boolean.TRUE.equals(willCreate.get(d))) {
                skipped++;
                continue;
            }

            // If overwrite, delete existing shifts for that date (and remember old WS)
            List<com.kopi.kopi.entity.EmployeeShift> existing = employeeShiftRepository.findByShiftDate(d);
            if (Boolean.TRUE.equals(req.overwrite) && existing != null && !existing.isEmpty()) {
                // collect old WS ids referenced
                for (com.kopi.kopi.entity.EmployeeShift ex : existing) {
                    if (ex.getWorkSchedule() != null)
                        touchedOldWs.add(ex.getWorkSchedule());
                }
                employeeShiftRepository.deleteAll(existing);
                updated += existing.size();
            }

            // create new rows for this date attached to spanWs
            int createdThisDate = 0;
            if (anchorShifts == null || anchorShifts.isEmpty())
                continue;
            for (com.kopi.kopi.entity.EmployeeShift anchorEs : anchorShifts) {
                Integer shiftId = anchorEs.getShift() != null ? anchorEs.getShift().getShiftId() : null;
                if (shiftId == null)
                    continue;
                com.kopi.kopi.entity.EmployeeShift newEs = com.kopi.kopi.entity.EmployeeShift.builder()
                        .workSchedule(spanWs)
                        .employee(anchorEs.getEmployee())
                        .shift(anchorEs.getShift())
                        .shiftDate(d)
                        .status(anchorEs.getStatus() != null ? anchorEs.getStatus() : "assigned")
                        .notes(anchorEs.getNotes())
                        .createdAt(LocalDateTime.now())
                        .overrideStartTime(anchorEs.getOverrideStartTime())
                        .overrideEndTime(anchorEs.getOverrideEndTime())
                        .build();
                if (adminUserId != null)
                    userRepository.findById(adminUserId).ifPresent(newEs::setCreatedByUser);
                employeeShiftRepository.save(newEs);
                createdThisDate++;
            }
            created += createdThisDate;
        }

        // If overwrite=true, attempt to delete any old work schedules that now have no
        // shifts
        if (Boolean.TRUE.equals(req.overwrite) && !touchedOldWs.isEmpty()) {
            for (WorkSchedule old : touchedOldWs) {
                try {
                    boolean stillHas = employeeShiftRepository
                            .existsByWorkScheduleWorkScheduleId(old.getWorkScheduleId());
                    if (!stillHas) {
                        workScheduleRepository.delete(old);
                    }
                } catch (Exception ex) {
                    // ignore deletion failures
                }
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("created", created);
        out.put("updated", updated);
        out.put("skipped", skipped);
        out.put("conflicts", conflicts);
        // diagnostics to help debug when no rows are created
        java.util.List<String> candStr = new java.util.ArrayList<>();
        for (LocalDate d : candidates)
            candStr.add(d.toString());
        out.put("candidates", candStr);
        out.put("anchorShiftCount", anchorShifts != null ? anchorShifts.size() : 0);
        if (anchorShifts == null || anchorShifts.isEmpty()) {
            out.put("warning", "No anchor shifts found for anchorDate; no employee_shifts will be created.");
        }

        // Persistence of generation history is disabled by configuration/choice.
        out.put("generationPersisted", false);
        out.put("generationReason", "persistence disabled; work_schedule_generations not used");
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apiv1/admin/work-schedules/generations")
    public ResponseEntity<?> listGenerations() {
        try {
            var list = workScheduleGenerationRepository.findAllByOrderByCreatedAtDesc();
            java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
            for (com.kopi.kopi.entity.WorkScheduleGeneration g : list) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("generationId", g.getGenerationId());
                m.put("name", g.getName());
                m.put("description", g.getDescription());
                m.put("anchorDate", g.getAnchorDate() != null ? g.getAnchorDate().toString() : null);
                m.put("startDate", g.getStartDate() != null ? g.getStartDate().toString() : null);
                m.put("endDate", g.getEndDate() != null ? g.getEndDate().toString() : null);
                m.put("type", g.getType());
                m.put("intervalDays", g.getIntervalDays());
                m.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
                m.put("createdCount", g.getCreatedCount());
                m.put("updatedCount", g.getUpdatedCount());
                m.put("skippedCount", g.getSkippedCount());
                m.put("conflicts", g.getConflicts());
                out.add(m);
            }
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            // If the table does not exist (user doesn't want SQL changes), return empty
            // list instead
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apiv1/admin/work-schedules/with-recurrence")
    public ResponseEntity<?> listWorkSchedulesWithRecurrence() {
        var list = workScheduleRepository.findByRecurrencePatternIsNotNullOrderByCreatedAtDesc();
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (com.kopi.kopi.entity.WorkSchedule w : list) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("workScheduleId", w.getWorkScheduleId());
            m.put("name", w.getName());
            m.put("description", w.getDescription());
            m.put("startDate", w.getStartDate() != null ? w.getStartDate().toString() : null);
            m.put("endDate", w.getEndDate() != null ? w.getEndDate().toString() : null);
            m.put("createdAt", w.getCreatedAt() != null ? w.getCreatedAt().toString() : null);
            m.put("recurrenceId", w.getRecurrencePattern() != null ? w.getRecurrencePattern().getRecurrenceId() : null);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apiv1/admin/work-schedules/{id}")
    public ResponseEntity<?> getWorkScheduleDetails(@PathVariable("id") Integer id) {
        var opt = workScheduleRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "WorkSchedule not found"));
        WorkSchedule w = opt.get();
        java.util.List<com.kopi.kopi.entity.EmployeeShift> shifts = employeeShiftRepository
                .findByWorkScheduleWorkScheduleIdOrderByShiftDateAsc(id);
        java.util.List<String> dates = new java.util.ArrayList<>();
        for (com.kopi.kopi.entity.EmployeeShift es : shifts) {
            if (es.getShiftDate() != null)
                dates.add(es.getShiftDate().toString());
        }
        Map<String, Object> out = new HashMap<>();
        out.put("workScheduleId", w.getWorkScheduleId());
        out.put("name", w.getName());
        out.put("description", w.getDescription());
        out.put("startDate", w.getStartDate() != null ? w.getStartDate().toString() : null);
        out.put("endDate", w.getEndDate() != null ? w.getEndDate().toString() : null);
        out.put("createdAt", w.getCreatedAt() != null ? w.getCreatedAt().toString() : null);
        out.put("shiftDates", dates);
        out.put("shiftCount", shifts.size());
        return ResponseEntity.ok(out);
    }

    /**
     * Delete a work schedule applying these rules:
     * - For employee_shifts in the future (shift_date > today): delete them.
     * - For employee_shifts in the past or today (shift_date <= today): do not
     * delete; set work_schedule_id = NULL.
     * - After processing, if no employee_shifts reference the work schedule, delete
     * the work_schedule record.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @org.springframework.web.bind.annotation.DeleteMapping("/apiv1/admin/work-schedules/{id}")
    public ResponseEntity<?> deleteWorkSchedule(@PathVariable("id") Integer id) {
        var opt = workScheduleRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "WorkSchedule not found"));
        WorkSchedule w = opt.get();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<com.kopi.kopi.entity.EmployeeShift> shifts = employeeShiftRepository
                .findByWorkScheduleWorkScheduleIdOrderByShiftDateAsc(id);
        int deletedFuture = 0;
        int unlinkedPast = 0;
        for (com.kopi.kopi.entity.EmployeeShift es : shifts) {
            java.time.LocalDate d = es.getShiftDate();
            if (d != null && d.isAfter(today)) {
                // future -> delete
                try {
                    employeeShiftRepository.delete(es);
                    deletedFuture++;
                } catch (Exception ex) {
                    // ignore individual failures
                }
            } else {
                // past or today -> unlink from this work schedule
                try {
                    es.setWorkSchedule(null);
                    employeeShiftRepository.save(es);
                    unlinkedPast++;
                } catch (Exception ex) {
                    // ignore individual failures
                }
            }
        }

        boolean stillHas = employeeShiftRepository.existsByWorkScheduleWorkScheduleId(id);
        boolean deletedWs = false;
        if (!stillHas) {
            try {
                workScheduleRepository.delete(w);
                deletedWs = true;
            } catch (Exception ex) {
                // ignore
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("deletedFutureShifts", deletedFuture);
        out.put("unlinkedPastShifts", unlinkedPast);
        out.put("workScheduleDeleted", deletedWs);
        return ResponseEntity.ok(out);
    }

    // FE currently posts to /apiv1/work-schedules/generate-from-pattern (non-admin
    // path).
    // Provide the same behavior on that path but still require ADMIN role.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/work-schedules/generate-from-pattern")
    public ResponseEntity<?> publicGenerate(@RequestBody GenerateRequest req) {
        return generate(req);
    }

    private List<LocalDate> computeCandidates(String type, LocalDate anchor, LocalDate start, LocalDate end,
            Integer interval, List<String> daysOfWeek) {
        List<LocalDate> out = new ArrayList<>();
        String t = (type == null) ? "" : type.toUpperCase();
        if ("DAILY".equals(t)) {
            int iv = (interval == null || interval < 1) ? 1 : interval;
            // For DAILY patterns the step should be calculated from the provided start date
            // (start is always selected). So include start and then step by interval days.
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(iv)) {
                out.add(d);
            }
            return out;
        }
        if ("WEEKLY".equals(t)) {
            Set<DayOfWeek> set = new HashSet<>();
            if (daysOfWeek != null) {
                for (String s : daysOfWeek) {
                    if (s == null)
                        continue;
                    switch (s.trim().toUpperCase()) {
                        case "MON", "MONDAY" -> set.add(DayOfWeek.MONDAY);
                        case "TUE", "TUESDAY" -> set.add(DayOfWeek.TUESDAY);
                        case "WED", "WEDNESDAY" -> set.add(DayOfWeek.WEDNESDAY);
                        case "THU", "THURSDAY" -> set.add(DayOfWeek.THURSDAY);
                        case "FRI", "FRIDAY" -> set.add(DayOfWeek.FRIDAY);
                        case "SAT", "SATURDAY" -> set.add(DayOfWeek.SATURDAY);
                        case "SUN", "SUNDAY" -> set.add(DayOfWeek.SUNDAY);
                    }
                }
            }
            int weekIv = (interval == null || interval < 1) ? 1 : interval;
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (!set.contains(d.getDayOfWeek()))
                    continue;
                long weeks = ChronoUnit.WEEKS.between(anchor, d);
                long absWeeks = Math.abs(weeks);
                if (absWeeks % weekIv == 0)
                    out.add(d);
            }
            return out;
        }
        // default: no candidates
        return out;
    }
}
