package com.kopi.kopi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kopi.kopi.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
	Optional<Category> findByName(String name);
} 