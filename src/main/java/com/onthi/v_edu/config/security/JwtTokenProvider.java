package com.onthi.v_edu.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtTokenProvider {

    private final com.onthi.v_edu.common.setting.SystemSettingService systemSettingService;

    @Value("${app.jwt-secret}")
    private String defaultJwtSecret;

    @Value("${app.jwt-expiration-milliseconds}")
    private long defaultJwtExpiration;

    public JwtTokenProvider(com.onthi.v_edu.common.setting.SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    // Generate JWT token
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        long expirationMs = systemSettingService.getSettingValueAsLong("JWT_EXPIRATION_MS", defaultJwtExpiration);
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + expirationMs);

        var builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expireDate);

        if (!roles.isEmpty()) {
            builder.claim("roles", roles);
            builder.claim("role", roles.get(0));
        }

        if (authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            builder.claim("userId", userDetails.getId());
        }

        return builder.signWith(key()).compact();
    }

    private Key key() {
        String secret = systemSettingService.getSettingValue("JWT_SECRET", defaultJwtSecret);
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim()));
    }

    // Get username from JWT token
    public String getUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("roles", List.class);
    }

    public String getRole(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    public Integer getUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                log.warn("❌ userId claim is missing in token");
                return null;
            }
            
            if (userIdObj instanceof Integer) {
                return (Integer) userIdObj;
            } else if (userIdObj instanceof Long) {
                return ((Long) userIdObj).intValue();
            } else {
                log.warn("❌ userId claim has unexpected type: {}", userIdObj.getClass().getName());
                return Integer.parseInt(userIdObj.toString());
            }
        } catch (Exception e) {
            log.error("❌ Error extracting userId from token: {}", e.getMessage());
            return null;
        }
    }

    // Validate JWT token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parse(token);
            return true;
        } catch (Exception ex) {
            log.error("❌ Token validation failed: {}", ex.getMessage());
            return false;
        }
    }
}
