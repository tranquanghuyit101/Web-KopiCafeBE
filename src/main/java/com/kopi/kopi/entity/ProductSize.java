package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_sizes", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSize {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_size_id")
	private Integer productSizeId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	@ToString.Exclude
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "size_id", nullable = false)
	@ToString.Exclude
	private Size size;

	@Column(name = "price", nullable = false, precision = 18, scale = 2)
	private BigDecimal price;

	@Column(name = "is_available", nullable = false)
	private Boolean available;
}


