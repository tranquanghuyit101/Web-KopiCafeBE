package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "role_id")
	private Integer roleId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<User> users = new ArrayList<>();
} 