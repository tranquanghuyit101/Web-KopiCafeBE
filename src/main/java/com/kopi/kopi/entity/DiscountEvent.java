package com.kopi.kopi.entity;

import com.kopi.kopi.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discount_events", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "discount_event_id")
	private Integer discountEventId;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 10)
	private DiscountType discountType;

	@Column(name = "discount_value", nullable = false, precision = 9, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(name = "ends_at", nullable = false)
	private LocalDateTime endsAt;

	@Column(name = "is_active", nullable = false)
	private Boolean active;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "discountEvent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<DiscountEventProduct> products = new ArrayList<>();
} 