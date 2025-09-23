package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Integer productId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	@ToString.Exclude
	private Category category;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "img_url", length = 255)
	private String imgUrl;

	@Column(name = "sku", length = 50)
	private String sku;

	@Column(name = "price", nullable = false, precision = 18, scale = 2)
	private BigDecimal price;

	@Column(name = "is_available", nullable = false)
	private Boolean available;

	@Column(name = "stock_qty", nullable = false)
	private Integer stockQty;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<OrderDetail> orderDetails = new ArrayList<>();

	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<DiscountEventProduct> discountEventProducts = new ArrayList<>();
} 