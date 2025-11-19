package com.kopi.kopi.controller;

import com.kopi.kopi.dto.ShiftDto;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.service.IShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Transactional
@RestController
public class ShiftController {
    private final IShiftService shiftService;

    public ShiftController(IShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apiv1/admin/shifts")
    public ResponseEntity<ShiftDto> createShift(@RequestBody ShiftDto dto) {
        // created_by_user_id should be resolved from security context in a later
        // improvement
        // resolve current admin user id from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer adminUserId = null;
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        ShiftDto created = shiftService.createShift(dto, adminUserId);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/apiv1/admin/shifts/{id}")
    public ResponseEntity<ShiftDto> updateShift(@PathVariable Integer id, @RequestBody ShiftDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer adminUserId = null;
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            adminUserId = up.getUser().getUserId();
        }
        ShiftDto updated = shiftService.updateShift(id, dto, adminUserId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/apiv1/shifts")
    public ResponseEntity<List<ShiftDto>> listActiveShifts() {
        var list = shiftService.listActiveShifts();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/apiv1/shifts/{id}")
    public ResponseEntity<ShiftDto> getShiftById(@PathVariable Integer id) {
        ShiftDto dto = shiftService.getShiftById(id);
        if (dto == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    // Get position rules for a given shift
    @GetMapping("/apiv1/shifts/{id}/position-rules")
    public ResponseEntity<List<java.util.Map<String, Object>>> getPositionRules(@PathVariable Integer id) {
        // inject repository via application context lookup to avoid constructor change
        // here
        var repo = org.springframework.web.context.support.WebApplicationContextUtils
                .getRequiredWebApplicationContext(
                        ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                .currentRequestAttributes()).getRequest().getServletContext())
                .getBean(com.kopi.kopi.repository.PositionShiftRuleRepository.class);
        var list = repo.findByShiftShiftId(id);
        List<java.util.Map<String, Object>> out = list.stream().map(r -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("positionId", r.getPosition().getPositionId());
            m.put("positionName", r.getPosition().getPositionName());
            m.put("isAllowed", r.isAllowed());
            m.put("requiredCount", r.getRequiredCount());
            return m;
        }).toList();
        return ResponseEntity.ok(out);
    }

    // Validate whether a shift can be added for a specific date (used by FE)
    @GetMapping("/apiv1/shifts/{id}/validate")
    public ResponseEntity<java.util.Map<String, Object>> validateShift(@PathVariable Integer id,
            @RequestParam String date) {
        java.time.LocalDate d = java.time.LocalDate.parse(date);
        java.time.LocalDate today = java.time.LocalDate.now();
        var repo = org.springframework.web.context.support.WebApplicationContextUtils
                .getRequiredWebApplicationContext(
                        ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                .currentRequestAttributes()).getRequest().getServletContext())
                .getBean(com.kopi.kopi.repository.PositionShiftRuleRepository.class);
        var esRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getRequiredWebApplicationContext(
                        ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                .currentRequestAttributes()).getRequest().getServletContext())
                .getBean(com.kopi.kopi.repository.EmployeeShiftRepository.class);

        java.util.Map<String, Object> out = new java.util.HashMap<>();
        if (d.isBefore(today)) {
            out.put("canAdd", false);
            out.put("reason", "Cannot add shift to past date");
            return ResponseEntity.ok(out);
        }

        var rules = repo.findByShiftShiftId(id);
        int totalReq = rules.stream().mapToInt(r -> r.getRequiredCount() == null ? 0 : r.getRequiredCount()).sum();
        if (totalReq < 1) {
            out.put("canAdd", false);
            out.put("reason", "No required slots configured for this shift");
            out.put("totalRequired", totalReq);
            return ResponseEntity.ok(out);
        }

        var existing = esRepo.findByShiftShiftIdAndShiftDate(id, d);
        out.put("totalRequired", totalReq);
        out.put("existingCount", existing != null ? existing.size() : 0);
        out.put("canAdd", existing == null || existing.isEmpty());
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/apiv1/admin/shifts/{id}/active")
    public ResponseEntity<Void> setShiftActive(@PathVariable Integer id, @RequestParam boolean active) {
        var repo = org.springframework.web.context.support.WebApplicationContextUtils
                .getRequiredWebApplicationContext(
                        ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                .currentRequestAttributes()).getRequest().getServletContext())
                .getBean(com.kopi.kopi.repository.ShiftRepository.class);
        var opt = repo.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        var s = opt.get();
        s.setActive(active);
        repo.save(s);

        // when deactivating, reconcile occurrences across dates
        if (!active) {
            Integer adminUserId = null;
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
                adminUserId = up.getUser().getUserId();
            }
            var esService = org.springframework.web.context.support.WebApplicationContextUtils
                    .getRequiredWebApplicationContext(
                            ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                    .currentRequestAttributes()).getRequest().getServletContext())
                    .getBean(com.kopi.kopi.service.EmployeeShiftService.class);
            esService.handleTemplateDeactivation(id, adminUserId);
        }

        return ResponseEntity.ok().build();
    }
}
