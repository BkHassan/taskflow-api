package com.taskflow.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

	private final JwtProperties properties;
	private SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	void init() {
		byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits) for HS256");
		}
		this.key = Keys.hmacShaKeyFor(secretBytes);
	}

	public String generateToken(UserDetails user) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + properties.expirationMs());
		String role = user.getAuthorities().iterator().next().getAuthority();
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("role", role)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	public String extractUsername(String token) {
		return parse(token).getSubject();
	}

	public boolean isValid(String token, UserDetails user) {
		String username = extractUsername(token);
		return username.equals(user.getUsername()) && parse(token).getExpiration().after(new Date());
	}

	private Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
