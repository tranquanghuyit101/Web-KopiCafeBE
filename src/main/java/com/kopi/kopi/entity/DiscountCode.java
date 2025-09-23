package com.kopi.kopi.entity;

import com.kopi.kopi.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discount_codes", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCode {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "discount_code_id")
	private Integer discountCodeId;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "description", length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 10)
	private DiscountType discountType;

	@Column(name = "discount_value", nullable = false, precision = 9, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "min_order_amount", precision = 18, scale = 2)
	private BigDecimal minOrderAmount;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(name = "ends_at", nullable = false)
	private LocalDateTime endsAt;

	@Column(name = "total_usage_limit")
	private Integer totalUsageLimit;

	@Column(name = "per_user_limit")
	private Integer perUserLimit;

	@Column(name = "is_active", nullable = false)
	private Boolean active;

	@Column(name = "usage_count", nullable = false)
	private Integer usageCount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "discountCode", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<DiscountCodeRedemption> redemptions = new ArrayList<>();
} 