package com.kopi.kopi.controller;

import com.kopi.kopi.repository.EmployeeShiftRepository;
import com.kopi.kopi.service.EmployeeShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
public class EmployeeShiftController {
    private final EmployeeShiftService employeeShiftService;
    private final EmployeeShiftRepository employeeShiftRepository;

    public EmployeeShiftController(EmployeeShiftService employeeShiftService,
                                   EmployeeShiftRepository employeeShiftRepository) {
        this.employeeShiftService = employeeShiftService;
        this.employeeShiftRepository = employeeShiftRepository;
    }

    public static class AddOpenRequest {
        public Integer shiftId;
        public String date; // YYYY-MM-DD
        public Boolean createWorkScheduleIfMissing = true;
        public Boolean mergeIfExists = true;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/admin/employee-shifts/add-open")
    public ResponseEntity<Map<String, Object>> addOpen(@RequestBody AddOpenRequest req) {
        LocalDate d = LocalDate.parse(req.date);
        // resolve admin user id from security context when available
        Integer adminUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        Map<String, Object> out = employeeShiftService.addOpenSlots(d, req.shiftId, adminUserId,
                req.createWorkScheduleIfMissing == null || req.createWorkScheduleIfMissing,
                req.mergeIfExists == null || req.mergeIfExists);
        return ResponseEntity.status(201).body(out);
    }

    public static class RemoveRequest {
        public String date;
        public Integer shiftId;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/admin/employee-shifts/remove")
    public ResponseEntity<Map<String, Object>> remove(@RequestBody RemoveRequest req) {
        LocalDate d = LocalDate.parse(req.date);
        Integer adminUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        try {
            var out = employeeShiftService.removeSlots(d, req.shiftId, adminUserId);
            return ResponseEntity.ok(out);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/apiv1/employee-shifts")
    public ResponseEntity<java.util.List<Map<String, Object>>> listByDate(
            @org.springframework.web.bind.annotation.RequestParam String date,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer shiftId) {
        LocalDate d = LocalDate.parse(date);
        java.util.List<com.kopi.kopi.entity.EmployeeShift> list;
        if (shiftId != null) {
            list = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId, d);
        } else {
            list = employeeShiftRepository.findByShiftDate(d);
        }
        // Auto-mark MISSED for occurrences that ended without check-in
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        for (var es : list) {
            if (es.getShiftDate() != null) {
                if (es.getShiftDate().equals(today)) {
                    java.time.LocalTime endTime = es.getOverrideEndTime() != null ? es.getOverrideEndTime()
                            : (es.getShift() != null ? es.getShift().getEndTime() : null);
                    if (endTime != null && now.isAfter(endTime) && es.getActualCheckIn() == null) {
                        String st = es.getStatus() == null ? "" : es.getStatus().toLowerCase();
                        if (!st.contains("miss") && !st.contains("completed")) {
                            es.setStatus("MISSED");
                            es.setUpdatedAt(java.time.LocalDateTime.now());
                            employeeShiftRepository.save(es);
                        }
                    }
                }
            }
        }

        java.util.List<Map<String, Object>> out = list.stream().map(es -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("employeeShiftId", es.getEmployeeShiftId());
            m.put("shiftId", es.getShift() != null ? es.getShift().getShiftId() : null);
            m.put("shiftDate", es.getShiftDate().toString());
            m.put("employeeId", es.getEmployee() != null ? es.getEmployee().getUserId() : null);
            m.put("status", es.getStatus());
            m.put("workScheduleId", es.getWorkSchedule() != null ? es.getWorkSchedule().getWorkScheduleId() : null);
            m.put("overrideStartTime", es.getOverrideStartTime() != null ? es.getOverrideStartTime().toString() : null);
            m.put("overrideEndTime", es.getOverrideEndTime() != null ? es.getOverrideEndTime().toString() : null);
            m.put("actualCheckIn", es.getActualCheckIn() != null ? es.getActualCheckIn().toString() : null);
            m.put("actualCheckOut", es.getActualCheckOut() != null ? es.getActualCheckOut().toString() : null);
            // If DB has explicit overtimeMinutes use it. Otherwise, when we have
            // actualCheckOut and overrideEndTime compute overtime as the positive
            // difference between actualCheckOut and scheduled end time.
            Integer ov = es.getOvertimeMinutes();
            if (ov != null) {
                // DB value is authoritative
                m.put("overtimeMinutes", ov);
            } else if (es.getActualCheckOut() != null && es.getOverrideEndTime() != null) {
                // Compute overtime as time-of-day difference (minutes) between actual check-out
                // and scheduled end time. Do NOT include date difference; use only local time
                // within the day (as requested).
                java.time.LocalTime actualT = es.getActualCheckOut().toLocalTime();
                java.time.LocalTime scheduledT = es.getOverrideEndTime();
                int actualMins = actualT.getHour() * 60 + actualT.getMinute();
                int scheduledMins = scheduledT.getHour() * 60 + scheduledT.getMinute();
                int diff = actualMins - scheduledMins;
                if (diff < 0)
                    diff = 0; // overtime only when actual > scheduled
                m.put("overtimeMinutes", diff);
            } else {
                m.put("overtimeMinutes", null);
            }
            m.put("reason", es.getReason());
            m.put("notes", es.getNotes());
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    /**
     * Return employee shifts for a date range. Query params:
     * - start (YYYY-MM-DD) required
     * - end (YYYY-MM-DD) required
     * - shiftId (optional) to filter by shift template
     */
    @GetMapping("/apiv1/employee-shifts/range")
    public ResponseEntity<java.util.List<Map<String, Object>>> listByRange(
            @org.springframework.web.bind.annotation.RequestParam String start,
            @org.springframework.web.bind.annotation.RequestParam String end,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer shiftId) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        java.util.List<com.kopi.kopi.entity.EmployeeShift> list;
        if (shiftId != null) {
            list = employeeShiftRepository.findByShiftShiftIdAndShiftDateBetween(shiftId, s, e);
        } else {
            list = employeeShiftRepository.findByShiftDateBetween(s, e);
        }

        // Auto-mark MISSED for occurrences that ended without check-in (for dates up to
        // today)
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        for (var es : list) {
            if (es.getShiftDate() != null) {
                if (!es.getShiftDate().isAfter(today)) {
                    java.time.LocalTime endTime = es.getOverrideEndTime() != null ? es.getOverrideEndTime()
                            : (es.getShift() != null ? es.getShift().getEndTime() : null);
                    if (end != null) {
                        // if end is before now when date==today, or if date is before today
                        boolean ended = es.getShiftDate().isBefore(today)
                                || (es.getShiftDate().equals(today) && now.isAfter(endTime));
                        if (ended && es.getActualCheckIn() == null) {
                            String st = es.getStatus() == null ? "" : es.getStatus().toLowerCase();
                            if (!st.contains("miss") && !st.contains("completed")) {
                                es.setStatus("MISSED");
                                es.setUpdatedAt(java.time.LocalDateTime.now());
                                employeeShiftRepository.save(es);
                            }
                        }
                    }
                }
            }
        }

        java.util.List<Map<String, Object>> out = list.stream().map(es -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("employeeShiftId", es.getEmployeeShiftId());
            m.put("shiftId", es.getShift() != null ? es.getShift().getShiftId() : null);
            m.put("shiftDate", es.getShiftDate().toString());
            m.put("employeeId", es.getEmployee() != null ? es.getEmployee().getUserId() : null);
            m.put("status", es.getStatus());
            m.put("workScheduleId", es.getWorkSchedule() != null ? es.getWorkSchedule().getWorkScheduleId() : null);
            // include convenient employee display fields
            if (es.getEmployee() != null) {
                var emp = es.getEmployee();
                m.put("employeeName", emp.getFullName() != null ? emp.getFullName() : emp.getUsername());
                m.put("employeePositionName", emp.getPosition() != null ? emp.getPosition().getPositionName() : null);
            }
            m.put("overrideStartTime", es.getOverrideStartTime() != null ? es.getOverrideStartTime().toString() : null);
            m.put("overrideEndTime", es.getOverrideEndTime() != null ? es.getOverrideEndTime().toString() : null);
            m.put("actualCheckIn", es.getActualCheckIn() != null ? es.getActualCheckIn().toString() : null);
            m.put("actualCheckOut", es.getActualCheckOut() != null ? es.getActualCheckOut().toString() : null);
            Integer ov2 = es.getOvertimeMinutes();
            if (ov2 != null) {
                m.put("overtimeMinutes", ov2);
            } else if (es.getActualCheckOut() != null && es.getOverrideEndTime() != null) {
                java.time.LocalTime actualT = es.getActualCheckOut().toLocalTime();
                java.time.LocalTime scheduledT = es.getOverrideEndTime();
                int actualMins = actualT.getHour() * 60 + actualT.getMinute();
                int scheduledMins = scheduledT.getHour() * 60 + scheduledT.getMinute();
                int diff = actualMins - scheduledMins;
                if (diff < 0)
                    diff = 0;
                m.put("overtimeMinutes", diff);
            } else {
                m.put("overtimeMinutes", null);
            }
            m.put("reason", es.getReason());
            m.put("notes", es.getNotes());
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    // Return whether a shift has any employee_shift occurrence on any date.
    @GetMapping("/apiv1/employee-shifts/exists")
    public ResponseEntity<Map<String, Object>> exists(
            @org.springframework.web.bind.annotation.RequestParam Integer shiftId) {
        long count = employeeShiftRepository.countByShiftShiftId(shiftId);
        return ResponseEntity.ok(Map.of("hasOccurrence", count > 0));
    }

    // Admin: assign an employee to a shift/date
    public static class AssignRequest {
        public Integer employeeId;
        public Integer shiftId;
        public String shiftDate; // YYYY-MM-DD
        public String notes;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/admin/employee-shifts")
    public ResponseEntity<?> assign(@RequestBody AssignRequest req) {
        LocalDate d = LocalDate.parse(req.shiftDate);
        Integer adminUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        try {
            var out = employeeShiftService.addEmployeeToShift(d, req.shiftId, req.employeeId, req.notes, adminUserId);
            return ResponseEntity.status(201).body(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Return the raw message body (string) so frontend sees just the text
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/apiv1/admin/employee-shifts/{id}")
    public ResponseEntity<?> deleteAssignment(@PathVariable Integer id) {
        Integer adminUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        try {
            var out = employeeShiftService.removeEmployeeFromShift(id, adminUserId);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    public static class CancelRequest {
        public String reason;
    }

    @PostMapping("/apiv1/employee-shifts/{id}/cancel")
    public ResponseEntity<?> cancelAssignment(@PathVariable Integer id, @RequestBody CancelRequest req) {
        Integer actingUserId = null;
        boolean actingIsAdmin = false;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            actingUserId = up.getUser().getUserId();
            actingIsAdmin = up.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        try {
            var out = employeeShiftService.cancelEmployeeShift(id, actingUserId, actingIsAdmin, req.reason);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/apiv1/employee-shifts/{id}/restore")
    public ResponseEntity<?> restoreAssignment(@PathVariable Integer id) {
        Integer actingUserId = null;
        boolean actingIsAdmin = false;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            actingUserId = up.getUser().getUserId();
            actingIsAdmin = up.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        try {
            var out = employeeShiftService.restoreEmployeeShift(id, actingUserId, actingIsAdmin);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/apiv1/employee-shifts/{id}/checkin")
    public ResponseEntity<?> checkinAssignment(@PathVariable Integer id) {
        Integer actingUserId = null;
        boolean actingIsAdmin = false;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            actingUserId = up.getUser().getUserId();
            actingIsAdmin = up.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        try {
            var out = employeeShiftService.checkinEmployeeShift(id, actingUserId, actingIsAdmin);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/apiv1/employee-shifts/{id}/checkout")
    public ResponseEntity<?> checkoutAssignment(@PathVariable Integer id) {
        Integer actingUserId = null;
        boolean actingIsAdmin = false;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            actingUserId = up.getUser().getUserId();
            actingIsAdmin = up.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        }
        try {
            var out = employeeShiftService.checkoutEmployeeShift(id, actingUserId, actingIsAdmin);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
