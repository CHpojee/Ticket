package com.standardinsurance.itsupport.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues and validates HS256 JWTs. See docs/specs/01-user-login.md. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-seconds}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String generate(String userId, String name, String role, boolean approver,
                           Integer approverLevel) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("name", name)
                .claim("role", role)
                .claim("approver", approver)
                .claim("approverLevel", approverLevel)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    /** Parses and validates a token, returning the authenticated principal. */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("name", String.class),
                claims.get("role", String.class),
                Boolean.TRUE.equals(claims.get("approver", Boolean.class)),
                claims.get("approverLevel", Integer.class));
    }
}
