package com.kopi.kopi.security;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
	@Value("${app.jwt.secret}")
	private String jwtSecret;

	@Value("${app.jwt.expiration-ms}")
	private long jwtExpirationMs;

	@Value("${app.jwt.remember-expiration-ms}")
	private long jwtRememberExpirationMs;

	private Key getSigningKey() {
		byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes()));
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(String subject, int roleNumber, Integer userId, String fullName, String email, boolean rememberMe) {
		long now = System.currentTimeMillis();
		long exp = now + (rememberMe ? jwtRememberExpirationMs : jwtExpirationMs);

		return Jwts.builder()
			.setSubject(subject)
			.addClaims(Map.of(
				"role", roleNumber,
				"userId", userId,
				"fullName", fullName,
				"email", email
			))
			.setIssuedAt(new Date(now))
			.setExpiration(new Date(exp))
			.signWith(getSigningKey(), SignatureAlgorithm.HS256)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Claims parseClaims(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(getSigningKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}
} 