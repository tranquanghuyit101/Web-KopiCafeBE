package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sizes", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Size {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "size_id")
	private Integer sizeId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "code", length = 20)
	private String code;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}


