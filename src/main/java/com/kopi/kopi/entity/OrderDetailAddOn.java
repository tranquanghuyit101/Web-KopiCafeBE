package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_detail_add_ons", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailAddOn {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_detail_add_on_id")
	private Integer orderDetailAddOnId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_detail_id", nullable = false)
	@ToString.Exclude
	private OrderDetail orderDetail;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "add_on_id", nullable = false)
	@ToString.Exclude
	private AddOn addOn;

	@Column(name = "unit_price_snapshot", nullable = false, precision = 18, scale = 2)
	private BigDecimal unitPriceSnapshot;
}


