package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_details", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_detail_id")
	private Integer orderDetailId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	@ToString.Exclude
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	@ToString.Exclude
	private Product product;

	@Column(name = "product_name_snapshot", nullable = false, length = 150)
	private String productNameSnapshot;

	@Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "line_total", precision = 18, scale = 2, insertable = false, updatable = false)
	private BigDecimal lineTotal;

	@Column(name = "note", length = 255)
	private String note;
} 