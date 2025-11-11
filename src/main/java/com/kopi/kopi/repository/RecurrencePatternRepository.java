package com.kopi.kopi.repository;

import com.kopi.kopi.entity.RecurrencePattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrencePatternRepository extends JpaRepository<RecurrencePattern, Integer> {

    boolean existsByRecurrenceTypeAndIntervalDays(String recurrenceType, Integer intervalDays);

    boolean existsByRecurrenceTypeAndDayOfWeek(String recurrenceType, String dayOfWeek);

    java.util.Optional<RecurrencePattern> findFirstByRecurrenceTypeAndIntervalDays(String recurrenceType, Integer intervalDays);

    java.util.Optional<RecurrencePattern> findFirstByRecurrenceTypeAndDayOfWeek(String recurrenceType, String dayOfWeek);

}
