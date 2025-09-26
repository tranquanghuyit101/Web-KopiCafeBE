package com.kopi.kopi.entity;

import com.kopi.kopi.entity.converter.UserStatusConverter;
import com.kopi.kopi.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "username", nullable = false, length = 100)
	private String username;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "phone", nullable = false, length = 20)
	private String phone;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	@ToString.Exclude
	private Role role;

	@Convert(converter = UserStatusConverter.class)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<UserAddress> userAddresses = new ArrayList<>();

	@OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<OrderEntity> ordersAsCustomer = new ArrayList<>();

	@OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<OrderEntity> ordersCreated = new ArrayList<>();
} 