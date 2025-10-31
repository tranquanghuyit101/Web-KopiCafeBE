package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.ProfileController;
import com.kopi.kopi.dto.ProfileResponse;
import com.kopi.kopi.entity.Address;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.UserAddress;
import com.kopi.kopi.repository.AddressRepository;
import com.kopi.kopi.repository.UserAddressRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(UserRepository userRepository, UserAddressRepository userAddressRepository, 
                              AddressRepository addressRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<ProfileResponse> getProfile(Integer userId) {
        User u = userRepository.findById(userId).orElseThrow();

        List<UserAddress> list = userAddressRepository.findAllWithAddressByUserId(userId);

        String addressStr = null;
        if (list != null && !list.isEmpty()) {
            UserAddress chosen = list.get(0);
            Address a = chosen.getAddress();
            if (a != null) {
                StringBuilder sb = new StringBuilder();
                append(sb, a.getAddressLine());
                append(sb, a.getWard());
                append(sb, a.getDistrict());
                append(sb, a.getCity());
                addressStr = sb.length() == 0 ? null : sb.toString();
            }
        }

        ProfileResponse dto = ProfileResponse.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .displayName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhone())
                .address(addressStr)
                .role(u.getRole() != null ? u.getRole().getName() : null)
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .positionId(u.getPosition() != null ? u.getPosition().getPositionId() : null)
                .positionName(u.getPosition() != null ? u.getPosition().getPositionName() : null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();

        return ResponseEntity.ok(dto);
    }

    @Override
    @Transactional
    public ResponseEntity<?> patchProfile(Integer userId, String displayName, String email, String phoneNumber, String address, String birthdate, String gender) {
        User u = userRepository.findById(userId).orElseThrow();
        if (displayName != null) u.setFullName(displayName);
        if (email != null) u.setEmail(email);
        if (phoneNumber != null) u.setPhone(phoneNumber);
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> changePassword(Integer userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", "New password must be at least 6 characters")
            );
        }

        User u = userRepository.findById(userId).orElseThrow();

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, u.getPasswordHash())) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", "Current password is incorrect")
            );
        }

        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> saveDefaultAddress(Integer userId, ProfileController.AddressPayload payload) {
        User u = userRepository.findById(userId).orElseThrow();

        // turn off existing default addresses
        List<UserAddress> existing = userAddressRepository.findAllWithAddressByUserId(userId);
        if (existing != null) {
            for (UserAddress ua : existing) {
                if (Boolean.TRUE.equals(ua.getDefaultAddress())) {
                    ua.setDefaultAddress(false);
                    userAddressRepository.save(ua);
                }
            }
        }

        String inferredCity = extractCityFromAddressLine(payload.address_line());
        String cityToUse = isBlank(inferredCity) ? (isBlank(payload.city()) ? null : payload.city().trim()) : inferredCity;

        Address a = Address.builder()
                .addressLine(payload.address_line())
                .ward(payload.ward())
                .district(payload.district())
                .city(cityToUse)
                .latitude(payload.latitude())
                .longitude(payload.longitude())
                .createdAt(LocalDateTime.now())
                .build();
        a = addressRepository.save(a);

        UserAddress ua = UserAddress.builder()
                .user(u)
                .address(a)
                .defaultAddress(true)
                .createdAt(LocalDateTime.now())
                .build();
        userAddressRepository.save(ua);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<?> listAddresses(Integer userId) {
        List<UserAddress> existing = userAddressRepository.findAllWithAddressByUserId(userId);
        java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        for (UserAddress ua : existing) {
            Address a = ua.getAddress();
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("user_address_id", ua.getUserAddressId());
            m.put("is_default", Boolean.TRUE.equals(ua.getDefaultAddress()));
            if (a != null) {
                m.put("address_id", a.getAddressId());
                m.put("address_line", a.getAddressLine());
                m.put("ward", a.getWard());
                m.put("district", a.getDistrict());
                m.put("city", a.getCity());
                m.put("latitude", a.getLatitude());
                m.put("longitude", a.getLongitude());
            }
            data.add(m);
        }
        return ResponseEntity.ok(java.util.Map.of("data", data));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createAddress(Integer userId, ProfileController.AddressPayload payload, boolean setDefault) {
        User u = userRepository.findById(userId).orElseThrow();

        if (setDefault) {
            List<UserAddress> existing = userAddressRepository.findAllWithAddressByUserId(userId);
            if (existing != null) {
                for (UserAddress ua : existing) {
                    if (Boolean.TRUE.equals(ua.getDefaultAddress())) {
                        ua.setDefaultAddress(false);
                        userAddressRepository.save(ua);
                    }
                }
            }
        }

        String inferredCity = extractCityFromAddressLine(payload.address_line());
        String cityToUse = isBlank(inferredCity) ? (isBlank(payload.city()) ? null : payload.city().trim()) : inferredCity;

        Address a = Address.builder()
                .addressLine(payload.address_line())
                .ward(payload.ward())
                .district(payload.district())
                .city(cityToUse)
                .latitude(payload.latitude())
                .longitude(payload.longitude())
                .createdAt(LocalDateTime.now())
                .build();
        a = addressRepository.save(a);

        UserAddress ua = UserAddress.builder()
                .user(u)
                .address(a)
                .defaultAddress(setDefault)
                .createdAt(LocalDateTime.now())
                .build();
        ua = userAddressRepository.save(ua);

        return ResponseEntity.ok(java.util.Map.of(
                "user_address_id", ua.getUserAddressId(),
                "address_id", a.getAddressId(),
                "is_default", ua.getDefaultAddress()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> setDefaultAddress(Integer userId, Integer userAddressId) {
        List<UserAddress> existing = userAddressRepository.findAllWithAddressByUserId(userId);
        if (existing != null) {
            for (UserAddress ua : existing) {
                boolean makeDefault = java.util.Objects.equals(ua.getUserAddressId(), userAddressId);
                ua.setDefaultAddress(makeDefault);
                userAddressRepository.save(ua);
            }
        }
        return ResponseEntity.ok().build();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Heuristic extractor: take the last non-numeric token before country from a comma-separated address line
    private String extractCityFromAddressLine(String addressLine) {
        if (addressLine == null) return null;
        String[] rawParts = addressLine.split(",");
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String p : rawParts) {
            if (p != null) {
                String t = p.trim();
                if (!t.isEmpty()) parts.add(t);
            }
        }
        for (int i = parts.size() - 1; i >= 0; i--) {
            String token = parts.get(i);
            String lc = token.toLowerCase();
            if ("vietnam".equals(lc) || "việt nam".equals(lc)) continue;
            if (token.matches("\\d{3,}")) continue; // skip postal code
            // Prefer the token immediately before the country when present
            return token;
        }
        return null;
    }

    private void append(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
    }
}

