package com.kopi.kopi.controller;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.service.ShippingLocationStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/apiv1/shipping")
public class ShippingController {
    private final ShippingLocationStore store;
    private final OrderRepository orderRepository;

    public ShippingController(ShippingLocationStore store, OrderRepository orderRepository) {
        this.store = store;
        this.orderRepository = orderRepository;
    }

    public record LocationPayload(Double lat, Double lng) {}

    @PostMapping("/{orderId}/location")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','STAFF')")
    public ResponseEntity<?> updateLocation(@PathVariable("orderId") Integer orderId, @RequestBody LocationPayload payload) {
        if (payload == null || payload.lat() == null || payload.lng() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "lat,lng required"));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        store.put(orderId, payload.lat(), payload.lng(), userId);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @GetMapping("/{orderId}/location")
    public ResponseEntity<?> getLocation(@PathVariable("orderId") Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String roleName = principal.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_CUSTOMER");
        // If customer, ensure the order belongs to them (when possible)
        if ("ROLE_CUSTOMER".equals(roleName)) {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
            Integer customerId = orderOpt.get().getCustomer() != null ? orderOpt.get().getCustomer().getUserId() : null;
            if (customerId == null || !customerId.equals(principal.getUser().getUserId())) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
            }
        }
        ShippingLocationStore.Location loc = store.get(orderId);
        if (loc == null) return ResponseEntity.ok(Map.of("data", null));
        return ResponseEntity.ok(Map.of("data", Map.of("lat", loc.lat, "lng", loc.lng)));
    }

    @GetMapping("/{orderId}/info")
    public ResponseEntity<?> getOrderShippingInfo(@PathVariable("orderId") Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String roleName = principal.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_CUSTOMER");
        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        OrderEntity o = orderOpt.get();
        if ("ROLE_CUSTOMER".equals(roleName)) {
            Integer customerId = o.getCustomer() != null ? o.getCustomer().getUserId() : null;
            if (customerId == null || !customerId.equals(principal.getUser().getUserId())) {
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

    @PostMapping("/{orderId}/claim")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','STAFF')")
    public ResponseEntity<?> claimOrder(@PathVariable("orderId") Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        var user = principal.getUser();

        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        var o = orderOpt.get();
        if (o.getAddress() == null) return ResponseEntity.badRequest().body(Map.of("message", "Order has no address"));
        if (o.getShipper() != null && !o.getShipper().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(409).body(Map.of("message", "Order already claimed"));
        }
        // Prevent multiple claims until PAID/COMPLETED/REJECTED/CANCELLED
        long active = orderRepository.countByShipper_UserIdAndStatusNotIn(user.getUserId(), java.util.List.of("PAID", "COMPLETED", "CANCELLED", "REJECTED"));
        if (active > 0 && (o.getShipper() == null || !o.getShipper().getUserId().equals(user.getUserId()))) {
            return ResponseEntity.status(409).body(Map.of("message", "You have an active claimed order"));
        }
        o.setShipper(user);
        orderRepository.save(o);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }
}


