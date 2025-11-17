package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "employee_shifts", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_shift_id")
    private Integer employeeShiftId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_schedule_id")
    @ToString.Exclude
    private WorkSchedule workSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @ToString.Exclude
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    @ToString.Exclude
    private Shift shift;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "override_start_time")
    private LocalTime overrideStartTime;

    @Column(name = "override_end_time")
    private LocalTime overrideEndTime;

    @Column(name = "actual_check_in")
    private LocalDateTime actualCheckIn;

    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes;

    @Column(name = "reason")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @ToString.Exclude
    private User createdByUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    @ToString.Exclude
    private User updatedByUser;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
