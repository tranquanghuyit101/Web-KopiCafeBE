package com.kopi.kopi.repository;

import com.kopi.kopi.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Integer> {
    Optional<WorkSchedule> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate start, LocalDate end);

    // Find work schedules linked to a recurrence pattern
    List<WorkSchedule> findByRecurrencePatternRecurrenceId(Integer recurrenceId);

    // Find all work schedules that have a recurrence attached
    List<WorkSchedule> findByRecurrencePatternIsNotNull();

    // Find all work schedules with recurrence, ordered by creation time (newest
    // first)
    List<WorkSchedule> findByRecurrencePatternIsNotNullOrderByCreatedAtDesc();
}
