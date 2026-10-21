package br.edu.atitus.gatewayapi.components;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey signingKey;

    // A chave e externalizada (application.properties / Consul) e deve ser a mesma usada pelo auth-api para assinar
    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Retorna os claims se a assinatura e a expiracao forem validas; caso contrario, null
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey) // Define a chave para verificacao
                    .build()
                    .parseSignedClaims(token) // Faz o parsing do token
                    .getPayload(); // Retorna as informacoes do token
        } catch (Exception e) {
            return null;
        }
    }
}
