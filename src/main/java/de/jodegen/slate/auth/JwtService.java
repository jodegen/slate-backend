package de.jodegen.slate.auth;

import de.jodegen.slate.config.JwtConfig;
import de.jodegen.slate.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiryMs;
    private final long refreshExpiryMs;

    public JwtService(JwtConfig config) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(config.getSecret()));
        this.expiryMs = config.getExpiryMs();
        this.refreshExpiryMs = config.getRefreshExpiryMs();
    }

    public String generateAccessToken(User user) {
        return buildToken(user, expiryMs, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpiryMs, "refresh");
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);
            return expectedType.equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(User user, long ttl, String tokenType) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", tokenType)
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
