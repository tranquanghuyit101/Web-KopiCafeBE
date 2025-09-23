package com.kopi.kopi.entity;

import com.kopi.kopi.entity.converter.PaymentMethodConverter;
import com.kopi.kopi.entity.converter.PaymentStatusConverter;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Integer paymentId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	@ToString.Exclude
	private OrderEntity order;

	@Column(name = "amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Convert(converter = PaymentMethodConverter.class)
	@Column(name = "method", nullable = false, length = 20)
	private PaymentMethod method;

	@Convert(converter = PaymentStatusConverter.class)
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	@Column(name = "txn_ref", length = 100)
	private String txnRef;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
} 