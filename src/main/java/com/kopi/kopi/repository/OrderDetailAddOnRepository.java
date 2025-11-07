package com.kopi.kopi.repository;

import com.kopi.kopi.entity.OrderDetailAddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderDetailAddOnRepository extends JpaRepository<OrderDetailAddOn, Integer> {
    List<OrderDetailAddOn> findByOrderDetail_OrderDetailId(Integer orderDetailId);
}


