package com.kopi.kopi.dto;

import java.time.LocalDateTime;

public class ProfileResponse {
	private String username;
	private String fullName;
	private String email;
	private String phone;
	private String role;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public ProfileResponse(String username, String fullName, String email, String phone, String role, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.username = username;
		this.fullName = fullName;
		this.email = email;
		this.phone = phone;
		this.role = role;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String getUsername() { return username; }
	public String getFullName() { return fullName; }
	public String getEmail() { return email; }
	public String getPhone() { return phone; }
	public String getRole() { return role; }
	public String getStatus() { return status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
} 