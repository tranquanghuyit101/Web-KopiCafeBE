package com.kopi.kopi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.dto.ProfileResponse;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.security.UserPrincipal;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
	@GetMapping
	public ProfileResponse me() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
		User u = principal.getUser();
		String roleName = u.getRole() != null ? u.getRole().getName() : "";
		String status = u.getStatus() != null ? u.getStatus().name() : "";
		return new ProfileResponse(
			u.getUsername(),
			u.getFullName(),
			u.getEmail(),
			u.getPhone(),
			roleName,
			status,
			u.getCreatedAt(),
			u.getUpdatedAt()
		);
	}
} 