package com.kopi.kopi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kopi.kopi.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
	Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
	Page<Product> findByCategory_CategoryIdAndNameContainingIgnoreCase(Integer categoryId, String name, Pageable pageable);
} 