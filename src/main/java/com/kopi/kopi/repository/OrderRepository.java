package com.kopi.kopi.repository;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.enums.OrderStatus;
import com.kopi.kopi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Integer> {

    // Lấy order theo user + status
    Optional<OrderEntity> findByCustomerAndStatus(User customer, OrderStatus status);

    // Lấy tất cả order của user
    List<OrderEntity> findByCustomer(User customer);

    // Lấy tất cả order theo status
    List<OrderEntity> findByStatus(OrderStatus status);
}
