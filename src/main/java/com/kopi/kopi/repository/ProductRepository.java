package com.kopi.kopi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kopi.kopi.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
} 