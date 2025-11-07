package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.EmployeeShift;
import com.kopi.kopi.entity.Shift;
import com.kopi.kopi.entity.WorkSchedule;
import com.kopi.kopi.repository.EmployeeShiftRepository;
import com.kopi.kopi.repository.PositionShiftRuleRepository;
import com.kopi.kopi.repository.ShiftRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.repository.WorkScheduleRepository;
import com.kopi.kopi.service.EmployeeShiftService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EmployeeShiftServiceImpl implements EmployeeShiftService {
    private final WorkScheduleRepository workScheduleRepository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final ShiftRepository shiftRepository;
    private final PositionShiftRuleRepository ruleRepository;
    private final UserRepository userRepository;

    public EmployeeShiftServiceImpl(WorkScheduleRepository workScheduleRepository,
            EmployeeShiftRepository employeeShiftRepository,
            ShiftRepository shiftRepository,
            PositionShiftRuleRepository ruleRepository,
            UserRepository userRepository) {
        this.workScheduleRepository = workScheduleRepository;
        this.employeeShiftRepository = employeeShiftRepository;
        this.shiftRepository = shiftRepository;
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> addOpenSlots(LocalDate date, Integer shiftId, Integer adminUserId,
            boolean createWorkScheduleIfMissing, boolean mergeIfExists) {
        Map<String, Object> out = new HashMap<>();
        // validate shift
        var sopt = shiftRepository.findById(shiftId);
        if (sopt.isEmpty())
            throw new IllegalArgumentException("Shift not found");
        Shift shift = sopt.get();

        // do not allow adding occurrence for past dates
        java.time.LocalDate today = java.time.LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalStateException("Cannot add shift to past date");
        }

        // check time overlap with other existing shift occurrences on the same date
        // ignore canceled occurrences and ignore same shift (duplicate handled later)
        List<EmployeeShift> existingOnDate = employeeShiftRepository.findByShiftDate(date);
        for (EmployeeShift es : existingOnDate) {
            if (es.getShift() == null)
                continue;
            if (es.getShift().getShiftId().equals(shiftId))
                continue; // same shift
            if (es.getStatus() != null && "canceled".equalsIgnoreCase(es.getStatus()))
                continue; // ignore canceled
            var s2 = es.getShift();
            var s1start = shift.getStartTime();
            var s1end = shift.getEndTime();
            var s2start = s2.getStartTime();
            var s2end = s2.getEndTime();
            if (s1start != null && s1end != null && s2start != null && s2end != null) {
                // overlap if start1 < end2 && start2 < end1
                if (s1start.isBefore(s2end) && s2start.isBefore(s1end))
                    throw new IllegalStateException("Shift time conflicts with existing occurrence on this date");
            }
        }

        // find or create work schedule covering date
        var wsOpt = workScheduleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        WorkSchedule ws = null;
        if (wsOpt.isPresent())
            ws = wsOpt.get();
        else if (createWorkScheduleIfMissing) {
            WorkSchedule w = WorkSchedule.builder()
                    .startDate(date)
                    .endDate(date)
                    .name("Auto: " + date.toString())
                    .createdAt(LocalDateTime.now())
                    .build();
            if (adminUserId != null) {
                userRepository.findById(adminUserId).ifPresent(w::setCreatedByUser);
            }
            ws = workScheduleRepository.save(w);
        }

        // position rules check
        var rules = ruleRepository.findByShiftShiftId(shiftId);
        int totalReq = rules.stream().mapToInt(r -> r.getRequiredCount() == null ? 0 : r.getRequiredCount()).sum();
        if (totalReq < 1)
            throw new IllegalArgumentException("No required slots configured for this shift");

        // existing employee shifts for date/shift
        List<EmployeeShift> existing = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId, date);
        if (!existing.isEmpty()) {
            if (!mergeIfExists) {
                throw new IllegalStateException("Employee shifts already exist for this shift and date");
            }
            // merge: return existing info
            out.put("created", false);
            out.put("existingCount", existing.size());
            out.put("employeeShiftId", existing.get(0).getEmployeeShiftId());
            out.put("workScheduleId",
                    existing.get(0).getWorkSchedule() != null ? existing.get(0).getWorkSchedule().getWorkScheduleId()
                            : null);
            return out;
        }

        // create single employee_shifts row (schema doesn't support multiple rows per
        // shift/date)
        EmployeeShift es = EmployeeShift.builder()
                .workSchedule(ws)
                .employee(null)
                .shift(shift)
                .shiftDate(date)
                // Use 'assigned' to comply with DB CHECK constraint on status
                .status("assigned")
                .createdAt(LocalDateTime.now())
                .build();
        if (adminUserId != null)
            userRepository.findById(adminUserId).ifPresent(es::setCreatedByUser);
        EmployeeShift saved = employeeShiftRepository.save(es);

        out.put("created", true);
        out.put("employeeShiftId", saved.getEmployeeShiftId());
        out.put("workScheduleId", ws != null ? ws.getWorkScheduleId() : null);
        out.put("createdCount", 1);
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> removeSlots(java.time.LocalDate date, Integer shiftId, Integer adminUserId) {
        Map<String, Object> out = new HashMap<>();
        var sopt = shiftRepository.findById(shiftId);
        if (sopt.isEmpty())
            throw new IllegalArgumentException("Shift not found");
        Shift shift = sopt.get();

        java.time.LocalDate today = java.time.LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalStateException("Cannot remove past occurrence");
        }

        List<EmployeeShift> existing = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId, date);
        if (existing.isEmpty()) {
            out.put("removed", false);
            out.put("existingCount", 0);
            return out;
        }

        // If today and within active time window, mark as canceled instead of delete
        if (date.equals(today)) {
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime start = shift.getStartTime();
            java.time.LocalTime end = shift.getEndTime();
            boolean inWindow = false;
            if (start != null && end != null) {
                if (!now.isBefore(start) && !now.isAfter(end))
                    inWindow = true;
            }
            if (inWindow) {
                existing.forEach(es -> {
                    es.setStatus("canceled");
                    es.setUpdatedAt(java.time.LocalDateTime.now());
                    if (adminUserId != null)
                        userRepository.findById(adminUserId).ifPresent(es::setUpdatedByUser);
                    employeeShiftRepository.save(es);
                });
                out.put("removed", false);
                out.put("canceled", true);
                out.put("affected", existing.size());
                return out;
            }
        }

        // future date -> delete occurrences
        int removed = 0;
        // track affected work schedule ids so we can clean up auto-created schedules
        Set<Integer> affectedWs = new HashSet<>();
        for (EmployeeShift es : existing) {
            if (es.getWorkSchedule() != null && es.getWorkSchedule().getWorkScheduleId() != null)
                affectedWs.add(es.getWorkSchedule().getWorkScheduleId());
            employeeShiftRepository.delete(es);
            removed++;
        }

        // cleanup: remove any auto-created work schedules that no longer have
        // occurrences
        for (Integer wsId : affectedWs) {
            boolean anyLeft = employeeShiftRepository.existsByWorkScheduleWorkScheduleId(wsId);
            if (!anyLeft) {
                workScheduleRepository.findById(wsId).ifPresent(ws -> {
                    String name = ws.getName();
                    if (name != null && name.startsWith("Auto:")) {
                        workScheduleRepository.delete(ws);
                    }
                });
            }
        }

        out.put("removed", true);
        out.put("removedCount", removed);
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> handleTemplateDeactivation(Integer shiftId, Integer adminUserId) {
        Map<String, Object> out = new HashMap<>();
        var sopt = shiftRepository.findById(shiftId);
        if (sopt.isEmpty())
            throw new IllegalArgumentException("Shift not found");
        Shift shift = sopt.get();

        java.time.LocalDate today = java.time.LocalDate.now();
        int canceled = 0;
        int deleted = 0;
        int kept = 0;

        List<EmployeeShift> existing = employeeShiftRepository.findAll().stream()
                .filter(es -> es.getShift() != null && es.getShift().getShiftId().equals(shiftId))
                .toList();

        for (EmployeeShift es : existing) {
            java.time.LocalDate d = es.getShiftDate();
            if (d.isBefore(today)) {
                // keep past occurrences
                kept++;
                continue;
            }
            if (d.equals(today)) {
                // if within active window -> cancel, else treat as future (delete) or cancel
                // depending on time
                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.LocalTime start = shift.getStartTime();
                java.time.LocalTime end = shift.getEndTime();
                boolean inWindow = false;
                if (start != null && end != null) {
                    if (!now.isBefore(start) && !now.isAfter(end))
                        inWindow = true;
                }
                if (inWindow) {
                    es.setStatus("canceled");
                    es.setUpdatedAt(java.time.LocalDateTime.now());
                    if (adminUserId != null)
                        userRepository.findById(adminUserId).ifPresent(es::setUpdatedByUser);
                    employeeShiftRepository.save(es);
                    canceled++;
                    continue;
                } else {
                    // not in window -> treat as future deletion
                    employeeShiftRepository.delete(es);
                    deleted++;
                    continue;
                }
            }
            // future date -> delete
            if (d.isAfter(today)) {
                employeeShiftRepository.delete(es);
                deleted++;
            }
        }

        out.put("keptPast", kept);
        out.put("canceledToday", canceled);
        out.put("deletedFuture", deleted);
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> addEmployeeToShift(java.time.LocalDate date, Integer shiftId, Integer employeeId,
            String notes, Integer adminUserId) {
        Map<String, Object> out = new HashMap<>();
        var sopt = shiftRepository.findById(shiftId);
        if (sopt.isEmpty())
            throw new IllegalArgumentException("Shift not found");
        Shift shift = sopt.get();

        var uopt = userRepository.findById(employeeId);
        if (uopt.isEmpty())
            throw new IllegalArgumentException("Employee not found");
        var employee = uopt.get();

        java.time.LocalDate today = java.time.LocalDate.now();
        if (date.isBefore(today))
            throw new IllegalStateException("Cannot assign employee to past date");

        // prevent duplicate assignment for same shift/date/employee
        var dup = employeeShiftRepository.findByShiftShiftIdAndShiftDateAndEmployeeUserId(shiftId, date,
                employeeId);
        if (dup != null && !dup.isEmpty()) {
            // Return a simple, user-friendly error message so the frontend shows
            // {"error":"please choose another employee"}
            throw new IllegalStateException("please choose another employee");
        }

        // check position rules (if rules defined)
        var rules = ruleRepository.findByShiftShiftId(shiftId);
        String empPos = employee.getPosition() != null ? employee.getPosition().getPositionName() : null;

        // If there are any position rules for this shift, only employees whose
        // position matches an allowed rule may be assigned. This prevents adding
        // employees of roles that are not part of the shift's configuration.
        if (rules != null && !rules.isEmpty()) {
            boolean hasMatchingRule = false;
            if (empPos != null) {
                hasMatchingRule = rules.stream()
                        .anyMatch(r -> r.getPosition() != null && r.isAllowed()
                                && empPos.equals(r.getPosition().getPositionName()));
            }
            if (!hasMatchingRule) {
                throw new IllegalStateException("please choose another employee");
            }

            // If there is a matching rule, enforce its required_count capacity
            var ruleOpt = rules.stream()
                    .filter(r -> r.getPosition() != null && empPos != null
                            && empPos.equals(r.getPosition().getPositionName()) && r.isAllowed())
                    .findFirst();
            if (ruleOpt.isPresent()) {
                Integer req = ruleOpt.get().getRequiredCount();
                if (req != null) {
                    // count existing assigned for this position on this shift/date
                    List<EmployeeShift> assigned = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId,
                            date);
                    int cur = 0;
                    for (EmployeeShift es : assigned) {
                        if (es.getEmployee() != null && es.getEmployee().getPosition() != null && empPos != null
                                && empPos.equals(es.getEmployee().getPosition().getPositionName())
                                && !"canceled".equalsIgnoreCase(es.getStatus()))
                            cur++;
                    }
                    if (cur >= req)
                        // Simplified message requested by the user
                        throw new IllegalStateException("please choose another employee");
                }
            }
        }

        // basic overlap check: employee should not have another assignment overlapping
        // on same date
        List<EmployeeShift> empOnDate = employeeShiftRepository.findByEmployeeUserIdAndShiftDate(employeeId, date);
        for (EmployeeShift es : empOnDate) {
            if (es.getShift() == null)
                continue;
            var s2 = es.getShift();
            var s1start = shift.getStartTime();
            var s1end = shift.getEndTime();
            var s2start = s2.getStartTime();
            var s2end = s2.getEndTime();
            if (s1start != null && s1end != null && s2start != null && s2end != null) {
                // overlap if start1 < end2 && start2 < end1
                if (s1start.isBefore(s2end) && s2start.isBefore(s1end))
                    throw new IllegalStateException("Employee has a conflicting assignment at the same time");
            }
        }

        // If there is an existing open slot (employee == null) for this shift/date,
        // reuse it by assigning this employee into that row instead of creating a new
        // one.
        List<EmployeeShift> existingForShift = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId,
                date);
        for (EmployeeShift open : existingForShift) {
            if (open.getEmployee() == null
                    && (open.getStatus() == null || !"canceled".equalsIgnoreCase(open.getStatus()))) {
                open.setEmployee(employee);
                open.setNotes(notes);
                open.setUpdatedAt(java.time.LocalDateTime.now());
                if (adminUserId != null)
                    userRepository.findById(adminUserId).ifPresent(open::setUpdatedByUser);
                EmployeeShift saved = employeeShiftRepository.save(open);
                out.put("created", true);
                out.put("employeeShiftId", saved.getEmployeeShiftId());
                // include employee info for frontend convenience
                out.put("employeeName",
                        employee.getFullName() != null ? employee.getFullName() : employee.getUsername());
                out.put("employeePositionName",
                        employee.getPosition() != null ? employee.getPosition().getPositionName() : null);
                return out;
            }
        }

        // find work schedule covering date if any
        var wsOpt = workScheduleRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        WorkSchedule ws = wsOpt.isPresent() ? wsOpt.get() : null;

        EmployeeShift es = EmployeeShift.builder()
                .workSchedule(ws)
                .employee(employee)
                .shift(shift)
                .shiftDate(date)
                .status("assigned")
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .build();
        if (adminUserId != null)
            userRepository.findById(adminUserId).ifPresent(es::setCreatedByUser);
        EmployeeShift saved = employeeShiftRepository.save(es);

        out.put("created", true);
        out.put("employeeShiftId", saved.getEmployeeShiftId());
        out.put("employeeName", employee.getFullName() != null ? employee.getFullName() : employee.getUsername());
        out.put("employeePositionName",
                employee.getPosition() != null ? employee.getPosition().getPositionName() : null);
        return out;
    }

    @Override
    @Transactional
    public Map<String, Object> removeEmployeeFromShift(Integer employeeShiftId, Integer adminUserId) {
        Map<String, Object> out = new HashMap<>();
        var opt = employeeShiftRepository.findById(employeeShiftId);
        if (opt.isEmpty())
            throw new IllegalArgumentException("EmployeeShift not found");
        EmployeeShift es = opt.get();
        java.time.LocalDate today = java.time.LocalDate.now();
        if (es.getShiftDate().isBefore(today))
            throw new IllegalStateException("Cannot remove assignment from past date");
        // Determine how many assigned employees remain for this shift/date
        Integer shiftId = es.getShift() != null ? es.getShift().getShiftId() : null;
        java.time.LocalDate date = es.getShiftDate();

        int assignedCount = 0;
        if (shiftId != null) {
            List<EmployeeShift> allForShift = employeeShiftRepository.findByShiftShiftIdAndShiftDate(shiftId, date);
            for (EmployeeShift e : allForShift) {
                if (e.getEmployee() != null && (e.getStatus() == null || !"canceled".equalsIgnoreCase(e.getStatus())))
                    assignedCount++;
            }
        }

        // If this is the last assigned employee for the shift/date, convert the row
        // into an open slot (set employee = null) instead of deleting the record.
        if (es.getEmployee() != null && assignedCount <= 1) {
            es.setEmployee(null);
            es.setNotes(null);
            es.setStatus("assigned");
            es.setUpdatedAt(java.time.LocalDateTime.now());
            if (adminUserId != null)
                userRepository.findById(adminUserId).ifPresent(es::setUpdatedByUser);
            employeeShiftRepository.save(es);
            out.put("removed", true);
            out.put("employeeShiftId", employeeShiftId);
            return out;
        }

        // Otherwise delete the assignment row
        Integer wsId = es.getWorkSchedule() != null ? es.getWorkSchedule().getWorkScheduleId() : null;
        employeeShiftRepository.delete(es);

        // if the work schedule was auto-created and now has no remaining occurrences,
        // delete it
        if (wsId != null) {
            boolean anyLeft = employeeShiftRepository.existsByWorkScheduleWorkScheduleId(wsId);
            if (!anyLeft) {
                workScheduleRepository.findById(wsId).ifPresent(ws -> {
                    String name = ws.getName();
                    if (name != null && name.startsWith("Auto:")) {
                        workScheduleRepository.delete(ws);
                    }
                });
            }
        }

        out.put("removed", true);
        out.put("employeeShiftId", employeeShiftId);
        return out;
    }
}
