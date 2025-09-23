package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_address_id")
	private Integer userAddressId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "address_id", nullable = false)
	@ToString.Exclude
	private Address address;

	@Column(name = "recipient_name", length = 150)
	private String recipientName;

	@Column(name = "is_default", nullable = false)
	private Boolean defaultAddress;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
} 