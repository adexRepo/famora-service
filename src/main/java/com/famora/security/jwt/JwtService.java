package com.famora.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final String jwtIssuer;
  private final long accessTokenExpirationMinutes;
  private final SecretKey signingKey;

  public JwtService(JwtProperties properties) {
    this.jwtIssuer = properties.issuer();
    this.accessTokenExpirationMinutes = properties.accessTokenExpirationMinutes();
    this.signingKey = Keys.hmacShaKeyFor(properties.decodedSecret());
  }

  public String generateAccessToken(UUID userId, String email) {
    return generateAccessTokenDetails(userId, email).token();
  }

  public GeneratedToken generateAccessTokenDetails(UUID userId, String email) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
    String token = Jwts.builder().subject(userId.toString()).claim("email", email)
        .issuedAt(Date.from(now))
        .issuer(jwtIssuer)
        .expiration(Date.from(expiresAt))
        .signWith(signingKey).compact();
    return new GeneratedToken(token, expiresAt);
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

  public boolean isTokenIssuedBefore(String token, OffsetDateTime timestamp) {
    if (timestamp == null) {
      return false;
    }
    Date issuedAt = parseClaims(token).getIssuedAt();
    return issuedAt == null || issuedAt.toInstant().isBefore(timestamp.toInstant());
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public record GeneratedToken(String token, Instant expiresAt) {

  }
}
