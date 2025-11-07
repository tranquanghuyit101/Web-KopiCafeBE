package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_add_ons", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAddOn {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_add_on_id")
	private Integer productAddOnId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	@ToString.Exclude
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "add_on_id", nullable = false)
	@ToString.Exclude
	private AddOn addOn;

	@Column(name = "price", nullable = false, precision = 18, scale = 2)
	private BigDecimal price;

	@Column(name = "is_available", nullable = false)
	private Boolean available;
}


