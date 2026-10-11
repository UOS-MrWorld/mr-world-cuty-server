package com.mrworld.yaho.security;

import com.mrworld.yaho.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private SecretKey key;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationTime;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshAccessExpirationTime;

    @PostConstruct
    protected void init() {
        byte[] secretKeyBytes = Decoders.BASE64.decode(secretKey);
        key = Keys.hmacShaKeyFor(secretKeyBytes);
    }

    // 접근 토큰: 회원 ID와 역할(CUSTOMER/STAFF)을 담는다.
    public String createToken(Long memberId, Role role) {
        return buildToken(memberId, role, "access", accessExpirationTime);
    }

    // 재발급용 토큰: 접근 토큰 발급에만 쓰이므로 역할은 담지 않는다.
    public String createRefreshToken(Long memberId) {
        return buildToken(memberId, null, "refresh", refreshAccessExpirationTime);
    }

    private String buildToken(Long memberId, Role role, String type, long expirationTime) {
        Date now = new Date();

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationTime));

        if (role != null) {
            builder.claim("role", role.name());
        }

        return builder.signWith(key).compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        UserDetails userDetails = User.builder()
                .username(claims.getSubject())
                .password("")
                .authorities("ROLE_" + claims.get("role", String.class))
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public LocalDateTime getExpiresAt(String token) {
        return parseClaims(token).getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
