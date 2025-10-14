package com.kopi.kopi.repository;

import com.kopi.kopi.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Integer> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);
}


