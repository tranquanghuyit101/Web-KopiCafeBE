package com.kopi.kopi.entity;

import com.kopi.kopi.entity.converter.ScheduleStatusConverter;
import com.kopi.kopi.entity.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_id")
	private Integer scheduleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id")
	@ToString.Exclude
	private User employee;

	@Column(name = "shift_date", nullable = false)
	private LocalDate shiftDate;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Convert(converter = ScheduleStatusConverter.class)
	@Column(name = "status", nullable = false, length = 20)
	private ScheduleStatus status;

	@Column(name = "notes", length = 255)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
} 