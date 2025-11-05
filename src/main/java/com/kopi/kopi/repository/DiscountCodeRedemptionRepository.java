package com.kopi.kopi.repository;

import com.kopi.kopi.entity.DiscountCodeRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountCodeRedemptionRepository extends JpaRepository<DiscountCodeRedemption, Integer> {
    int countByDiscountCode_DiscountCodeId(Integer discountCodeId);
    int countByDiscountCode_DiscountCodeIdAndUser_UserId(Integer discountCodeId, Integer userId);
}


