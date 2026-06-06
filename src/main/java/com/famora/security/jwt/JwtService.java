package com.famora.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  
  @Value("${app.security.jwt.secret}")
  private String jwtSecret;
  @Value("${app.security.jwt.issuer}")
  private String jwtIssuer;
  @Value("${app.security.jwt.access-token-expiration-minutes}")
  private long accessTokenExpirationMinutes;
  private SecretKey signingKey;
  
  @PostConstruct
  void init() {
    byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }
  
  public String generateAccessToken(UUID userId, String email) {
    Instant now = Instant.now();
    return Jwts.builder().subject(userId.toString()).claim("email", email)
        .issuedAt(Date.from(now))
        .issuer(jwtIssuer)
        .expiration(Date.from(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
        .signWith(signingKey).compact();
  }
  
  public UUID extractUserId(String token) {
    return UUID.fromString(parseClaims(token).getSubject());
  }
  
  public String extractEmail(String token) {
    return parseClaims(token).get("email", String.class);
  }
  
  public boolean isTokenValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
  
  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
