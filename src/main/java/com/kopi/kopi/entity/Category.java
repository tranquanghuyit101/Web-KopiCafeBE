package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_id")
	private Integer categoryId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "is_active", nullable = false)
	private Boolean active;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@Builder.Default
	private List<Product> products = new ArrayList<>();
} 