package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discount_event_products", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountEventProduct {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "discount_event_product_id")
	private Integer discountEventProductId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "discount_event_id", nullable = false)
	@ToString.Exclude
	private DiscountEvent discountEvent;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	@ToString.Exclude
	private Product product;
} 