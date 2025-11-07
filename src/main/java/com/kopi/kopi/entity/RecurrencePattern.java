package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recurrence_patterns", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurrencePattern {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_id")
    private Integer recurrenceId;

    @Column(name = "recurrence_type", nullable = false, length = 20)
    private String recurrenceType; // DAILY, WEEKLY, MONTHLY, YEARLY

    @Column(name = "day_of_week", length = 10)
    private String dayOfWeek; // CSV like MON,WED,FRI

    @Column(name = "interval_days")
    private Integer intervalDays;
}
