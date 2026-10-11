package com.example.employee.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Generate and Validate valid token successfully")
    void testGenerateAndValidateValidToken() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser", "pass", "test@ems.local", com.example.employee.entity.Role.ADMIN, null, true);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String token = jwtTokenProvider.generateToken(auth);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Validate token should return false for malformed token string")
    void testValidateMalformedToken() {
        String malformedToken = "not.a.valid.jwt.token.string";
        assertFalse(jwtTokenProvider.validateToken(malformedToken));
    }

    @Test
    @DisplayName("Validate token should return false for invalid signature")
    void testValidateInvalidSignatureToken() {
        // Create token with different signing key
        String differentSecret = "515E635266556A586E3272357538782F413F4428472B4B6250645367566B5971";
        byte[] keyBytes = Decoders.BASE64.decode(differentSecret);
        var key = Keys.hmacShaKeyFor(keyBytes);

        String invalidSigToken = Jwts.builder()
                .subject("tamperedUser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(invalidSigToken));
    }

    @Test
    @DisplayName("Validate token should return false for expired token")
    void testValidateExpiredToken() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        var key = Keys.hmacShaKeyFor(keyBytes);

        // Build token that expired 1 hour ago
        String expiredToken = Jwts.builder()
                .subject("expiredUser")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Validate token should return false for empty or null token")
    void testValidateEmptyOrNullToken() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
