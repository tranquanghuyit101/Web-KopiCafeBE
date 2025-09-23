package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Integer orderId;

	@Column(name = "order_code", nullable = false, length = 30)
	private String orderCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	@ToString.Exclude
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "address_id")
	@ToString.Exclude
	private Address address;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id")
	@ToString.Exclude
	private User createdBy;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "close_reason", length = 500)
	private String closeReason;

	@Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal subtotalAmount;

	@Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal discountAmount;

	@Column(name = "total_amount", precision = 18, scale = 2, insertable = false, updatable = false)
	private BigDecimal totalAmount;

	@Column(name = "note", length = 500)
	private String note;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<OrderDetail> orderDetails = new ArrayList<>();

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<Payment> payments = new ArrayList<>();

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<InventoryLog> inventoryLogs = new ArrayList<>();

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<DiscountCodeRedemption> discountCodeRedemptions = new ArrayList<>();
} 