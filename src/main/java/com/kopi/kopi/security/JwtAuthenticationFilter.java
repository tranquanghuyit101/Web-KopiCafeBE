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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();        // ví dụ: /Kopi/apiv1/auth/forgotPass
        String ctx = request.getContextPath();       // "/Kopi"
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());       // còn: /apiv1/auth/forgotPass
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // CHỈ bỏ qua filter cho các endpoint công khai này:
        if (uri.equals("/apiv1/auth/login")) return true;
        if (uri.equals("/apiv1/auth/forgotPass") || uri.equals("/apiv1/auth/forgot-password")) return true;

        // Còn lại (kể cả /apiv1/auth/change-password) phải qua filter
        return false;
    }

    @Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
        if (shouldNotFilter(request)) {               // SKIP hẳn cho auth endpoints
            filterChain.doFilter(request, response);
            return;
        }
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
					String roleName;
					switch (roleNumber) {
						case 1 -> roleName = "ADMIN";
						case 2 -> roleName = "STAFF";
						default -> roleName = "CUSTOMER";
					}
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