package com.kopi.kopi.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.kopi.kopi.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		return userRepository.findByUsername(usernameOrEmail)
			.map(UserPrincipal::new)
			.or(() -> userRepository.findByEmail(usernameOrEmail).map(UserPrincipal::new))
			.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
} 