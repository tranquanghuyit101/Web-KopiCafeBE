package com.kopi.kopi.repository;

import com.kopi.kopi.entity.OrderDetailAddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailAddOnRepository extends JpaRepository<OrderDetailAddOn, Integer> {
}


