package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "add_ons", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddOn {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "add_on_id")
	private Integer addOnId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}


