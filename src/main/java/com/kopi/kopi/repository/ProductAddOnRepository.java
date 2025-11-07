package com.kopi.kopi.repository;

import com.kopi.kopi.entity.ProductAddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAddOnRepository extends JpaRepository<ProductAddOn, Integer> {
    List<ProductAddOn> findByProduct_ProductIdAndAvailableTrue(Integer productId);
    Optional<ProductAddOn> findByProduct_ProductIdAndAddOn_AddOnId(Integer productId, Integer addOnId);
}


