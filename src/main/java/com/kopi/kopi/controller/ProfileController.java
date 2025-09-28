package com.kopi.kopi.controller;

import com.kopi.kopi.dto.ProfileResponse;
import com.kopi.kopi.entity.Address;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.UserAddress;
import com.kopi.kopi.repository.UserAddressRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/apiv1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User u = ((UserPrincipal) auth.getPrincipal()).getUser();

        // ⭐ Lấy địa chỉ bằng userId + JOIN FETCH -> chắc chắn có Address nếu tồn tại trong DB
        List<UserAddress> list = userAddressRepository.findAllWithAddressByUserId(u.getUserId());

        String addressStr = null;
        if (list != null && !list.isEmpty()) {
            // list đã order defaultAddress DESC, createdAt ASC -> lấy phần tử đầu tiên
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
                .address(addressStr)             // ✅ trả về cho FE
                .role(u.getRole() != null ? u.getRole().getName() : null)
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();

        return ResponseEntity.ok(dto);
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
        User u = ((UserPrincipal) auth.getPrincipal()).getUser();

        if (displayName != null) u.setFullName(displayName);
        if (email != null) u.setEmail(email);
        if (phoneNumber != null) u.setPhone(phoneNumber);

        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
        return ResponseEntity.ok().build();
    }

    public record PasswordChangePayload(String current_password, String new_password) {}

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangePayload body) {
        if (body == null || body.new_password() == null || body.new_password().length() < 6) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", "New password must be at least 6 characters")
            );
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User u = ((UserPrincipal) auth.getPrincipal()).getUser();

        if (body.current_password() == null || !passwordEncoder.matches(body.current_password(), u.getPasswordHash())) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", "Current password is incorrect")
            );
        }

        u.setPasswordHash(passwordEncoder.encode(body.new_password()));
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
        return ResponseEntity.noContent().build();
    }

    private void append(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
    }
}
