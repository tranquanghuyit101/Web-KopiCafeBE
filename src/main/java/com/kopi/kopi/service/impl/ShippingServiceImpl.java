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
    private final com.kopi.kopi.repository.AddressRepository addressRepository;
    private final MapboxService mapbox;

    public ShippingServiceImpl(ShippingLocationStore store, OrderRepository orderRepository, UserRepository userRepository,
                               com.kopi.kopi.repository.AddressRepository addressRepository,
                               MapboxService mapbox) {
        this.store = store;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.mapbox = mapbox;
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
        // Allow only the customer of the order OR the assigned shipper to read location
        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Order not found"));
        OrderEntity order = orderOpt.get();
        Integer customerId = order.getCustomer() != null ? order.getCustomer().getUserId() : null;
        Integer shipperId = order.getShipper() != null ? order.getShipper().getUserId() : null;
        boolean isAllowed = (customerId != null && customerId.equals(userId)) || (shipperId != null && shipperId.equals(userId));
        if (!isAllowed) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
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
        Integer customerId = o.getCustomer() != null ? o.getCustomer().getUserId() : null;
        Integer shipperId = o.getShipper() != null ? o.getShipper().getUserId() : null;
        boolean isAllowed = (customerId != null && customerId.equals(userId)) || (shipperId != null && shipperId.equals(userId));
        if (!isAllowed) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
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

    @Override
    public ResponseEntity<?> estimateFee(Integer addressId, String addressLine) {
        try {
            com.kopi.kopi.entity.Address addr = null;
            if (addressId != null) {
                addr = addressRepository.findById(addressId).orElse(null);
            }
            if (addr == null && (addressLine != null && !addressLine.isBlank())) {
                var geo = mapbox.geocodeAddress(addressLine);
                if (geo == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Không xác định được địa chỉ"));
                }
                addr = com.kopi.kopi.entity.Address.builder()
                        .addressLine(addressLine)
                        .city(geo.city())
                        .latitude(geo.lat())
                        .longitude(geo.lng())
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
            }
            if (addr == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "address_id hoặc address_line là bắt buộc"));
            }

            String city = normalize(addr.getCity());
            if (city == null || !city.equals("da nang")) {
                return ResponseEntity.status(400).body(Map.of("message", "Chỉ có thể ship nội tỉnh (Đà Nẵng)"));
            }
            if (addr.getLatitude() == null || addr.getLongitude() == null) {
                var geo = mapbox.geocodeAddress(addr.getAddressLine());
                if (geo == null) return ResponseEntity.badRequest().body(Map.of("message", "Không xác định được toạ độ"));
                addr.setLatitude(geo.lat());
                addr.setLongitude(geo.lng());
            }

            double meters = mapbox.getDrivingDistanceMeters(mapbox.getShopLat(), mapbox.getShopLng(), addr.getLatitude(), addr.getLongitude());
            if (meters < 0) return ResponseEntity.status(502).body(Map.of("message", "Không tính được khoảng cách"));
            long shippingFee = computeShippingFeeVnd(meters);
            return ResponseEntity.ok(Map.of(
                    "data", Map.of(
                            "distance_meters", meters,
                            "shipping_fee", shippingFee
                    )
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("message", "Server error"));
        }
    }

    private String normalize(String s) {
        if (s == null) return null;
        String lower = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        return lower.trim();
    }

    private long computeShippingFeeVnd(double distanceMeters) {
        double km = distanceMeters / 1000.0;
        if (km < 1.0) return 0L;
        if (km <= 3.0) return 30000L;
        return 50000L;
    }
}

