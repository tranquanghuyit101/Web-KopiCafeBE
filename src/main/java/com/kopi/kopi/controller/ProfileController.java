package com.kopi.kopi.controller;

import com.kopi.kopi.dto.ProfileResponse;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apiv1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return profileService.getProfile(userId);
    }

    // Nhận multipart/form-data từ FE (display_name, email, phone_number ...)
    @PatchMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> patchProfile(
            @RequestPart(value = "display_name", required = false) String displayName,
            @RequestPart(value = "email", required = false) String email,
            @RequestPart(value = "phone_number", required = false) String phoneNumber,
            @RequestPart(value = "address", required = false) String address,
            @RequestPart(value = "birthdate", required = false) String birthdate,
            @RequestPart(value = "gender", required = false) String gender
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return profileService.patchProfile(userId, displayName, email, phoneNumber, address, birthdate, gender);
    }

    public record PasswordChangePayload(String current_password, String new_password) {}

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangePayload body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return profileService.changePassword(userId, body.current_password(), body.new_password());
    }

    public record AddressPayload(String address_line, String ward, String district, String city, Double latitude, Double longitude) {}

    @PostMapping("/address")
    public ResponseEntity<?> saveDefaultAddress(@RequestBody AddressPayload payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return profileService.saveDefaultAddress(userId, payload);
    }

    @GetMapping("/addresses")
    public ResponseEntity<?> listAddresses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return profileService.listAddresses(userId);
    }

    public record CreateAddressPayload(String address_line, String ward, String district, String city, Double latitude, Double longitude, Boolean set_default) {}

    @PostMapping("/addresses")
    public ResponseEntity<?> createAddress(@RequestBody CreateAddressPayload payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        boolean setDefault = payload.set_default() != null && payload.set_default();
        AddressPayload core = new AddressPayload(payload.address_line(), payload.ward(), payload.district(), payload.city(), payload.latitude(), payload.longitude());
        return profileService.createAddress(userId, core, setDefault);
    }

    @PutMapping("/addresses/{userAddressId}/default")
    public ResponseEntity<?> setDefault(@PathVariable("userAddressId") Integer userAddressId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return profileService.setDefaultAddress(userId, userAddressId);
    }
}
