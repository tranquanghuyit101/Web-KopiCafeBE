package com.kopi.kopi.dto;

public class LoginResponse {
	private String username;
	private String role;
	private String fullName;

	public LoginResponse(String username, String role, String fullName) {
		this.username = username;
		this.role = role;
		this.fullName = fullName;
	}

	public String getUsername() {
		return username;
	}

	public String getRole() {
		return role;
	}

	public String getFullName() {
		return fullName;
	}
} 