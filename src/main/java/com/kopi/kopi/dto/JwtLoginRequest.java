package com.kopi.kopi.dto;

public class JwtLoginRequest {
	private String email;
	private String password;
	private boolean rememberMe;

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public boolean isRememberMe() { return rememberMe; }
	public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }
} 