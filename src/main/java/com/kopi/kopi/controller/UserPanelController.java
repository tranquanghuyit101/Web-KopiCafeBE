package com.kopi.kopi.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.security.UserPrincipal;

@RestController
@RequestMapping("/apiv1/userPanel")
public class UserPanelController {
	private final UserRepository userRepository;

	public UserPanelController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping("/profile")
	public Map<String, Object> profile() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
		var u = principal.getUser();
		var data = List.of(Map.of(
			"user_id", u.getUserId(),
			"display_name", u.getFullName(),
			"first_name", "",
			"last_name", "",
			"address", "",
			"birthdate", null,
			"img", null,
			"created_at", u.getCreatedAt(),
			"email", u.getEmail(),
			"phone_number", u.getPhone()
		));
		return Map.of("data", data);
	}

	@PatchMapping("/profile")
	public ResponseEntity<?> updateProfile(
		@RequestPart(value = "image", required = false) MultipartFile image,
		@RequestPart(value = "display_name", required = false) String displayName,
		@RequestPart(value = "address", required = false) String address,
		@RequestPart(value = "birthdate", required = false) String birthdate,
		@RequestPart(value = "gender", required = false) String gender,
		@RequestPart(value = "email", required = false) String email,
		@RequestPart(value = "phone_number", required = false) String phoneNumber,
		@RequestPart(value = "first_name", required = false) String firstName,
		@RequestPart(value = "last_name", required = false) String lastName
	) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
		User u = principal.getUser();

		if (displayName != null) u.setFullName(displayName);
		if (email != null) u.setEmail(email);
		if (phoneNumber != null) u.setPhone(phoneNumber);
		// address/first/last/gender/birthdate/img ignored in current schema; placeholders
		// Optionally parse birthdate if needed
		try {
			userRepository.save(u);
			return ResponseEntity.ok(Map.of("message", "Profile updated"));
		} catch (Exception ex) {
			return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
		}
	}
} 