package com.kopi.kopi.repository;

import com.kopi.kopi.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Integer> {
    List<ProductSize> findByProduct_ProductIdAndAvailableTrue(Integer productId);
    Optional<ProductSize> findByProduct_ProductIdAndSize_SizeId(Integer productId, Integer sizeId);
}


