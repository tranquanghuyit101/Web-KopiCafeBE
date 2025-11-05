package com.kopi.kopi.repository;

import com.kopi.kopi.entity.ProductAddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAddOnRepository extends JpaRepository<ProductAddOn, Integer> {
}


