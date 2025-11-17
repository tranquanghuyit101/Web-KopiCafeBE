package com.kopi.kopi.repository;

import com.kopi.kopi.entity.EmployeeShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, Integer> {
    List<EmployeeShift> findByShiftShiftIdAndShiftDate(Integer shiftId, LocalDate date);

    List<EmployeeShift> findByShiftDate(LocalDate date);

    // Find assignments between two dates (inclusive)
    List<EmployeeShift> findByShiftDateBetween(LocalDate start, LocalDate end);

    // Find assignments between two dates filtered by shiftId
    List<EmployeeShift> findByShiftShiftIdAndShiftDateBetween(Integer shiftId, LocalDate start,
            LocalDate end);

    // Count occurrences across any date for a shift
    long countByShiftShiftId(Integer shiftId);

    // Find assignments for a given employee on a specific date
    List<EmployeeShift> findByEmployeeUserIdAndShiftDate(Integer userId, LocalDate date);

    // Find assignment for a specific shift/date/employee (if present)
    List<EmployeeShift> findByShiftShiftIdAndShiftDateAndEmployeeUserId(Integer shiftId, LocalDate date,
            Integer userId);

    // Helper to check if any employee_shifts reference a given work_schedule
    boolean existsByWorkScheduleWorkScheduleId(Integer workScheduleId);

    // Find all employee_shifts that reference a given work_schedule, ordered by
    // date
    List<EmployeeShift> findByWorkScheduleWorkScheduleIdOrderByShiftDateAsc(
            Integer workScheduleId);
}
