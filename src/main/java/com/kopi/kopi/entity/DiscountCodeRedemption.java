package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "discount_code_redemptions", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCodeRedemption {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "discount_code_redemption_id")
	private Integer discountCodeRedemptionId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "discount_code_id", nullable = false)
	@ToString.Exclude
	private DiscountCode discountCode;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	@ToString.Exclude
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	@ToString.Exclude
	private User user;

	@Column(name = "redeemed_at", nullable = false)
	private LocalDateTime redeemedAt;
} 