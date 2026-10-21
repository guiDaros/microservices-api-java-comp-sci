package br.edu.atitus.authapi.components;

import br.edu.atitus.authapi.entities.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    // A chave deve ser a mesma configurada no gateway-api, que valida a assinatura
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Claims id, type e email sao lidos pelo gateway e propagados como X-User-Id, X-User-Type e X-User-Email
    public String generateToken(UserEntity user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("type", user.getType().ordinal())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }
}
