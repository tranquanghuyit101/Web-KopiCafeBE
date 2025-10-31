package com.kopi.kopi.controller;

import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apiv1/shipping")
public class ShippingController {
    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    public record LocationPayload(Double lat, Double lng) {}

    @PostMapping("/{orderId}/location")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','STAFF')")
    public ResponseEntity<?> updateLocation(@PathVariable("orderId") Integer orderId, @RequestBody LocationPayload payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return shippingService.updateLocation(orderId, userId, payload.lat(), payload.lng());
    }

    @GetMapping("/{orderId}/location")
    public ResponseEntity<?> getLocation(@PathVariable("orderId") Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String roleName = principal.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_CUSTOMER");
        Integer userId = principal.getUser().getUserId();
        return shippingService.getLocation(orderId, roleName, userId);
    }

    @GetMapping("/{orderId}/info")
    public ResponseEntity<?> getOrderShippingInfo(@PathVariable("orderId") Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String roleName = principal.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_CUSTOMER");
        Integer userId = principal.getUser().getUserId();
        return shippingService.getOrderShippingInfo(orderId, roleName, userId);
    }

    @PostMapping("/{orderId}/claim")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','STAFF')")
    public ResponseEntity<?> claimOrder(@PathVariable("orderId") Integer orderId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return shippingService.claimOrder(orderId, userId);
    }

    @GetMapping("/estimate")
    public ResponseEntity<?> estimate(@RequestParam(value = "address_id", required = false) Integer addressId,
                                      @RequestParam(value = "address", required = false) String addressLine) {
        return shippingService.estimateFee(addressId, addressLine);
    }
}


