package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_log", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inventory_log_id")
	private Integer inventoryLogId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "inventory_item_id", nullable = false)
	@ToString.Exclude
	private InventoryItem inventoryItem;

	@Column(name = "change_type", nullable = false, length = 10)
	private String changeType;

	@Column(name = "quantity_change", nullable = false, precision = 18, scale = 3)
	private BigDecimal quantityChange;

	@Column(name = "reason", length = 255)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	@ToString.Exclude
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id")
	@ToString.Exclude
	private User createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
} 