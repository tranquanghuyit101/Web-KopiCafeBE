package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_items", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "inventory_item_id")
	private Integer inventoryItemId;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "unit", nullable = false, length = 20)
	private String unit;

	@Column(name = "quantity_on_hand", nullable = false, precision = 18, scale = 3)
	private BigDecimal quantityOnHand;

	@Column(name = "reorder_level", nullable = false, precision = 18, scale = 3)
	private BigDecimal reorderLevel;

	@Column(name = "is_active", nullable = false)
	private Boolean active;

	@Column(name = "notes", length = 255)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "inventoryItem", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<InventoryLog> logs = new ArrayList<>();
} 