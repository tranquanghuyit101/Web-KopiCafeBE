package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addresses", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "address_id")
	private Integer addressId;

	@Column(name = "address_line", nullable = false, length = 255)
	private String addressLine;

	@Column(name = "ward", length = 100)
	private String ward;

	@Column(name = "district", length = 100)
	private String district;

	@Column(name = "city", length = 100)
	private String city;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "address", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<UserAddress> userAddresses = new ArrayList<>();

	@OneToMany(mappedBy = "address", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<OrderEntity> orders = new ArrayList<>();
} 