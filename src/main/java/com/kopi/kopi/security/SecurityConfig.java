package com.kopi.kopi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

			@Bean
		public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
					.requestMatchers("/", "/index.html", "/static/**", "/app.js").permitAll()
					.requestMatchers("/menu", "/menu.html", "/menu.js").permitAll()
					.requestMatchers("/profile", "/profile.html", "/profile.js").permitAll()
					.requestMatchers("/api/auth/login", "/api/auth/me").permitAll()
					.requestMatchers("/api/menu", "/v1/**").permitAll()
					.anyRequest().authenticated()
				)
				.formLogin(form -> form.disable());
			return http.build();
		}
} 