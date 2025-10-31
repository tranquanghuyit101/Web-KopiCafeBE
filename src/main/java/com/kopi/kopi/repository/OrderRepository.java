package com.kopi.kopi.repository;

import com.kopi.kopi.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {
    Page<OrderEntity> findByCustomer_UserId(Integer userId, Pageable pageable);
    Page<OrderEntity> findByStatus(String status, Pageable pageable);
    Page<OrderEntity> findByStatusAndTableIsNotNull(String status, Pageable pageable);
    Page<OrderEntity> findByStatusAndAddressIsNotNull(String status, Pageable pageable);
    Page<OrderEntity> findByStatusNotInAndAddressIsNotNull(List<String> statuses, Pageable pageable);
    Page<OrderEntity> findByStatusNotInAndAddressIsNull(List<String> statuses, Pageable pageable);
    long countByShipper_UserIdAndStatusNotIn(Integer userId, List<String> statuses);
    long countByTable_TableIdAndStatus(Integer tableId, String status);
}


