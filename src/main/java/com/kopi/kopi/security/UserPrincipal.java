package com.kopi.kopi.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.kopi.kopi.entity.User;

public class UserPrincipal implements UserDetails {
	private final User user;

	public UserPrincipal(User user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		String roleName = (user.getRole() != null && user.getRole().getName() != null) ? user.getRole().getName() : "CUSTOMER";
		return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
	}

	@Override
	public String getPassword() {
		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return user.getStatus() != null && user.getStatus().name().equalsIgnoreCase("active");
	}

	public User getUser() {
		return user;
	}
} 