package com.kopi.kopi.service;

import java.time.LocalDate;

public interface EmployeeShiftService {
        /**
         * Add open slot(s) for a given shift on a specific date. Because the DB schema
         * only allows one employee_shifts row per (shift,date), this will create a
         * single
         * employee_shifts row if missing, and return info about creation.
         */
        java.util.Map<String, Object> addOpenSlots(LocalDate date, Integer shiftId, Integer adminUserId,
                        boolean createWorkScheduleIfMissing, boolean mergeIfExists);

        /**
         * Remove or cancel employee shift slot(s) for a given shift on a specific date.
         * Behavior:
         * - If date < today: reject (do not allow deleting past occurrences)
         * - If date == today and current time within shift start/end: mark status
         * "canceled"
         * - If date > today: delete the employee_shifts row(s)
         */
        java.util.Map<String, Object> removeSlots(LocalDate date, Integer shiftId, Integer adminUserId);

        /**
         * When a shift template is deactivated (or deleted) by admin, reconcile all
         * existing employee_shift occurrences for that shift across dates:
         * - past dates: leave intact (history)
         * - today and within active window: mark as canceled
         * - future dates: delete the occurrence
         */
        java.util.Map<String, Object> handleTemplateDeactivation(Integer shiftId, Integer adminUserId);

        /**
         * Add a specific employee to a shift occurrence (shift/date).
         * Performs validations: no past assignments, position capacity, and basic
         * overlap check.
         */
        java.util.Map<String, Object> addEmployeeToShift(LocalDate date, Integer shiftId,
                                                         Integer employeeId, String notes, Integer adminUserId);

        /**
         * Remove a specific employee assignment by employeeShiftId. Will reject past
         * removals.
         */
        java.util.Map<String, Object> removeEmployeeFromShift(Integer employeeShiftId, Integer adminUserId);

        /**
         * Cancel an employee assignment (employee_shifts row). The acting user must be
         * either the assigned employee or an admin. A non-empty reason is required.
         */
        java.util.Map<String, Object> cancelEmployeeShift(Integer employeeShiftId, Integer actingUserId,
                        boolean actingIsAdmin, String reason);

        /**
         * Restore a previously canceled employee assignment back to ASSIGNED and clear
         * the reason. Acting user must be assigned employee or admin.
         */
        java.util.Map<String, Object> restoreEmployeeShift(Integer employeeShiftId, Integer actingUserId,
                        boolean actingIsAdmin);

        /**
         * Check in the assigned employee for an employee_shift. Only allowed while the
         * shift is active (start <= now <= end). Returns actual_check_in timestamp.
         */
        java.util.Map<String, Object> checkinEmployeeShift(Integer employeeShiftId, Integer actingUserId,
                        boolean actingIsAdmin);

        /**
         * Check out the assigned employee for an employee_shift. Only allowed after
         * the shift end and only if there was a check-in. Sets actual_check_out and
         * updates status to COMPLETED.
         */
        java.util.Map<String, Object> checkoutEmployeeShift(Integer employeeShiftId, Integer actingUserId,
                        boolean actingIsAdmin);
}
