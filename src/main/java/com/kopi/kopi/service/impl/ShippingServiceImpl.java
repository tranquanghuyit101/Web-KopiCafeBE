package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.ShippingLocationStore;
import com.kopi.kopi.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class ShippingServiceImpl implements ShippingService {
    private final ShippingLocationStore store;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ShippingServiceImpl(ShippingLocationStore store, OrderRepository orderRepository, UserRepository userRepository) {
        this.store = store;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<?> updateLocation(Integer orderId, Integer userId, Double lat, Double lng) {
        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "lat,lng required"));
        }
        store.put(orderId, lat, lng, userId);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @Override
    public ResponseEntity<?> getLocation(Integer orderId, String roleName, Integer userId) {
        // If customer, ensure the order belongs to them (when possible)
        if ("ROLE_CUSTOMER".equals(roleName)) {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
            Integer customerId = orderOpt.get().getCustomer() != null ? orderOpt.get().getCustomer().getUserId() : null;
            if (customerId == null || !customerId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
            }
        }
        ShippingLocationStore.Location loc = store.get(orderId);
        if (loc == null) return ResponseEntity.ok(Map.of("data", null));
        return ResponseEntity.ok(Map.of("data", Map.of("lat", loc.lat, "lng", loc.lng)));
    }

    @Override
    public ResponseEntity<?> getOrderShippingInfo(Integer orderId, String roleName, Integer userId) {
        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        OrderEntity o = orderOpt.get();
        if ("ROLE_CUSTOMER".equals(roleName)) {
            Integer customerId = o.getCustomer() != null ? o.getCustomer().getUserId() : null;
            if (customerId == null || !customerId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
            }
        }
        String addr = o.getAddress() != null ? o.getAddress().getAddressLine() : null;
        Double lat = o.getAddress() != null ? o.getAddress().getLatitude() : null;
        Double lng = o.getAddress() != null ? o.getAddress().getLongitude() : null;
        return ResponseEntity.ok(Map.of("data", Map.of(
                "address", addr,
                "lat", lat,
                "lng", lng
        )));
    }

    @Override
    @Transactional
    public ResponseEntity<?> claimOrder(Integer orderId, Integer userId) {
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        var o = orderOpt.get();
        if (o.getAddress() == null) return ResponseEntity.badRequest().body(Map.of("message", "Order has no address"));
        
        User user = userRepository.findById(userId).orElseThrow();
        if (o.getShipper() != null && !o.getShipper().getUserId().equals(userId)) {
            return ResponseEntity.status(409).body(Map.of("message", "Order already claimed"));
        }
        // Prevent multiple claims until PAID/COMPLETED/REJECTED/CANCELLED
        long active = orderRepository.countByShipper_UserIdAndStatusNotIn(userId, java.util.List.of("PAID", "COMPLETED", "CANCELLED", "REJECTED"));
        if (active > 0 && (o.getShipper() == null || !o.getShipper().getUserId().equals(userId))) {
            return ResponseEntity.status(409).body(Map.of("message", "You have an active claimed order"));
        }
        
        o.setShipper(user);
        orderRepository.save(o);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }
}

