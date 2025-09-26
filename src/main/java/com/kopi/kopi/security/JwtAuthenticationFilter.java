package com.kopi.kopi.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider tokenProvider;
	private final CustomUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
		this.tokenProvider = tokenProvider;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);
			if (tokenProvider.validateToken(token)) {
				Claims claims = tokenProvider.parseClaims(token);
				String subject = claims.getSubject();
				Object roleClaim = claims.get("role");
				Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
				try {
					int roleNumber = Integer.parseInt(String.valueOf(roleClaim));
					String roleName = roleNumber >= 2 ? "ADMIN" : "CUSTOMER";
					authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
				} catch (Exception ignored) {}

				UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, authorities.isEmpty() ? userDetails.getAuthorities() : authorities);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		filterChain.doFilter(request, response);
	}
} 