package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_schedules", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_schedule_id")
    private Integer workScheduleId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description")
    private String description;
    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_id")
    @ToString.Exclude
    private RecurrencePattern recurrencePattern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @ToString.Exclude
    private User createdByUser;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    @ToString.Exclude
    private User updatedByUser;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
