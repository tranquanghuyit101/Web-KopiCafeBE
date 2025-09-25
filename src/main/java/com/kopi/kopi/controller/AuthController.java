package com.kopi.kopi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.dto.JwtLoginRequest;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.security.JwtTokenProvider;

import java.util.Map;

@RestController
@RequestMapping("/apiv1/auth")
public class AuthController {
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody JwtLoginRequest request) {
		try {
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
			int roleNumber = principal.getUser().getRole() != null && principal.getUser().getRole().getRoleId() != null
				? principal.getUser().getRole().getRoleId()
				: 1;
			String token = jwtTokenProvider.generateToken(
				principal.getUsername(),
				roleNumber,
				principal.getUser().getUserId(),
				principal.getUser().getFullName(),
				principal.getUser().getEmail(),
				request.isRememberMe()
			);

			return ResponseEntity.ok(Map.of(
				"data", Map.of("token", token)
			));
		} catch (BadCredentialsException ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tài khoản hoặc mật khẩu");
		}
	}

	@DeleteMapping("/logout")
	public ResponseEntity<?> logout() {
		return ResponseEntity.ok(Map.of("message", "logged out"));
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logoutPost() {
		return ResponseEntity.ok(Map.of("message", "logged out"));
	}
} 