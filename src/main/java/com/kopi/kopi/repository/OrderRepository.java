package com.kopi.kopi.repository;

import com.kopi.kopi.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    Page<OrderEntity> findByCustomer_UserId(Integer userId, Pageable pageable);
    Page<OrderEntity> findByStatus(String status, Pageable pageable);
    Page<OrderEntity> findByStatusAndTableIsNotNull(String status, Pageable pageable);
    Page<OrderEntity> findByStatusAndAddressIsNotNull(String status, Pageable pageable);
    long countByTable_TableIdAndStatus(Integer tableId, String status);
}


