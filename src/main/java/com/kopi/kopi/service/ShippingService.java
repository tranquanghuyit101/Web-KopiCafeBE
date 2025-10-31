package com.kopi.kopi.service;

import org.springframework.http.ResponseEntity;

public interface ShippingService {
    ResponseEntity<?> updateLocation(Integer orderId, Integer userId, Double lat, Double lng);
    ResponseEntity<?> getLocation(Integer orderId, String roleName, Integer userId);
    ResponseEntity<?> getOrderShippingInfo(Integer orderId, String roleName, Integer userId);
    ResponseEntity<?> claimOrder(Integer orderId, Integer userId);

    // Estimate fee based on address_id or address_line (one of them required)
    ResponseEntity<?> estimateFee(Integer addressId, String addressLine);
}

