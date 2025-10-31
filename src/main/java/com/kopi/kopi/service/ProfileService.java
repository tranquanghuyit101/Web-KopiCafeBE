package com.kopi.kopi.service;

import com.kopi.kopi.controller.ProfileController;
import com.kopi.kopi.dto.ProfileResponse;
import org.springframework.http.ResponseEntity;

public interface ProfileService {
    ResponseEntity<ProfileResponse> getProfile(Integer userId);
    ResponseEntity<?> patchProfile(Integer userId, String displayName, String email, String phoneNumber, String address, String birthdate, String gender);
    ResponseEntity<?> changePassword(Integer userId, String currentPassword, String newPassword);
    ResponseEntity<?> saveDefaultAddress(Integer userId, ProfileController.AddressPayload payload);
    ResponseEntity<?> listAddresses(Integer userId);
    ResponseEntity<?> createAddress(Integer userId, ProfileController.AddressPayload payload, boolean setDefault);
    ResponseEntity<?> setDefaultAddress(Integer userId, Integer userAddressId);
}

