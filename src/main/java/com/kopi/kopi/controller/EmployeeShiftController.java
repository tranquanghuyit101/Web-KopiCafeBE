package com.kopi.kopi.controller;

import com.kopi.kopi.service.EmployeeShiftService;
import com.kopi.kopi.repository.EmployeeShiftRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<java.util.Map<String, Object>> remove(@RequestBody RemoveRequest req) {
        java.time.LocalDate d = java.time.LocalDate.parse(req.date);
        Integer adminUserId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        try {
            var out = employeeShiftService.removeSlots(d, req.shiftId, adminUserId);
            return ResponseEntity.ok(out);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/apiv1/employee-shifts")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> listByDate(
            @org.springframework.web.bind.annotation.RequestParam String date,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer shiftId) {
        java.time.LocalDate d = java.time.LocalDate.parse(date);
        java.util.List<com.kopi.kopi.entity.EmployeeShift> list;
        if (shiftId != null) {
            list = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId, d);
        } else {
            list = employeeShiftRepository.findByShiftDate(d);
        }
        java.util.List<java.util.Map<String, Object>> out = list.stream().map(es -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("employeeShiftId", es.getEmployeeShiftId());
            m.put("shiftId", es.getShift() != null ? es.getShift().getShiftId() : null);
            m.put("shiftDate", es.getShiftDate().toString());
            m.put("employeeId", es.getEmployee() != null ? es.getEmployee().getUserId() : null);
            m.put("status", es.getStatus());
            m.put("workScheduleId", es.getWorkSchedule() != null ? es.getWorkSchedule().getWorkScheduleId() : null);
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
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> listByRange(
            @org.springframework.web.bind.annotation.RequestParam String start,
            @org.springframework.web.bind.annotation.RequestParam String end,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer shiftId) {
        java.time.LocalDate s = java.time.LocalDate.parse(start);
        java.time.LocalDate e = java.time.LocalDate.parse(end);
        java.util.List<com.kopi.kopi.entity.EmployeeShift> list;
        if (shiftId != null) {
            list = employeeShiftRepository.findByShiftShiftIdAndShiftDateBetween(shiftId, s, e);
        } else {
            list = employeeShiftRepository.findByShiftDateBetween(s, e);
        }

        java.util.List<java.util.Map<String, Object>> out = list.stream().map(es -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
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
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    // Return whether a shift has any employee_shift occurrence on any date.
    @GetMapping("/apiv1/employee-shifts/exists")
    public ResponseEntity<java.util.Map<String, Object>> exists(
            @org.springframework.web.bind.annotation.RequestParam Integer shiftId) {
        long count = employeeShiftRepository.countByShiftShiftId(shiftId);
        return ResponseEntity.ok(java.util.Map.of("hasOccurrence", count > 0));
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
        java.time.LocalDate d = java.time.LocalDate.parse(req.shiftDate);
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
}
