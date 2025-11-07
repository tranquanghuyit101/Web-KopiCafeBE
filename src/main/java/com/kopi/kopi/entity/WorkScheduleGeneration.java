package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_schedule_generations", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkScheduleGeneration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "generation_id")
    private Integer generationId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "anchor_date")
    private LocalDate anchorDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "recurrence_id")
    private Integer recurrenceId;

    @Column(name = "type")
    private String type;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "created_by_user_id")
    private Integer createdByUserId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_count")
    private Integer createdCount;

    @Column(name = "updated_count")
    private Integer updatedCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    @Column(name = "conflicts")
    private String conflicts; // comma-separated dates or JSON
}
