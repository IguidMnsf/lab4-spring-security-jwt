package com.example.lab4.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret-base64}") String secretBase64,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        if (secretBase64.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 doit contenir une clé secrète base64.");
        }
        if (expirationSeconds < 60 || expirationSeconds > 86_400) {
            throw new IllegalStateException("La durée du JWT doit être comprise entre 60 et 86400 secondes.");
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("La clé JWT doit contenir au moins 32 octets.");
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 doit être une clé base64 valide d'au moins 32 octets.",
                    exception);
        }
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
