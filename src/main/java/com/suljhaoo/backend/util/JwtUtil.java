package com.suljhaoo.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${jwt.secret:your-secret-key-change-in-production}")
  private String secret;

  @Value("${jwt.expiry:7d}")
  private String expiry;

  @Value("${jwt.kid:suljhaoo-dev-jwt}")
  private String kid;

  @Value("${jwt.audience:}")
  private String audience;

  private SecretKey getSigningKey() {
    // For HS256, we need exactly 256 bits (32 bytes)
    // Truncate or pad the secret to exactly 32 bytes
    // IMPORTANT: PowerSync must be configured with the same 32-byte key
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    byte[] keyBytes = new byte[32];

    if (secretBytes.length >= 32) {
      // Use first 32 bytes (truncate)
      System.arraycopy(secretBytes, 0, keyBytes, 0, 32);
    } else {
      // Pad with repetition of the secret
      for (int i = 0; i < 32; i++) {
        keyBytes[i] = secretBytes[i % secretBytes.length];
      }
    }

    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(String id, String phoneNumber, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("id", id);
    claims.put("phoneNumber", phoneNumber);
    claims.put("role", role);

    // Add audience claim for PowerSync compatibility
    // PowerSync requires "aud" claim in the JWT payload
    // Note: "aud" should be a string, not an array
    if (audience != null && !audience.isEmpty()) {
      claims.put("aud", audience); // String, not array
    }

    return createToken(claims, id);
  }

  private String createToken(Map<String, Object> claims, String subject) {
    long expirationTime = parseExpiry(expiry);

    // PowerSync requires tokens to expire within 24 hours (86400 seconds = 86400000 ms)
    // If the configured expiry is longer, cap it at 23 hours for PowerSync compatibility
    long maxPowerSyncExpiry = 23 * 60 * 60 * 1000L; // 23 hours in milliseconds (82800000 ms)
    if (expirationTime > maxPowerSyncExpiry) {
      expirationTime = maxPowerSyncExpiry;
    }

    Date expirationDate = new Date(System.currentTimeMillis() + expirationTime);

    // Build header with KID for PowerSync compatibility
    // PowerSync requires the KID header to match the configured key ID
    // Note: The algorithm must match what signWith() will use
    // For HS256, we ensure the key is exactly 32 bytes (256 bits)
    Map<String, Object> header = new HashMap<>();
    header.put("kid", kid); // Key ID for PowerSync (default: "suljhaoo-dev-jwt")
    header.put("typ", "JWT"); // Type
    // Don't set alg in header - let signWith() set it based on key size
    // But we ensure key is 32 bytes so it uses HS256

    return Jwts.builder()
        .header()
        .add(header)
        .and()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(expirationDate)
        .signWith(getSigningKey()) // This will use HS256 since key is 32 bytes
        .compact();
  }

  private long parseExpiry(String expiry) {
    if (expiry == null || expiry.isEmpty()) {
      return 7 * 24 * 60 * 60 * 1000L; // Default 7 days
    }

    expiry = expiry.toLowerCase().trim();
    long multiplier = 1;

    if (expiry.endsWith("d")) {
      multiplier = 24 * 60 * 60 * 1000L; // days to milliseconds
      expiry = expiry.substring(0, expiry.length() - 1);
    } else if (expiry.endsWith("h")) {
      multiplier = 60 * 60 * 1000L; // hours to milliseconds
      expiry = expiry.substring(0, expiry.length() - 1);
    } else if (expiry.endsWith("m")) {
      multiplier = 60 * 1000L; // minutes to milliseconds
      expiry = expiry.substring(0, expiry.length() - 1);
    } else if (expiry.endsWith("s")) {
      multiplier = 1000L; // seconds to milliseconds
      expiry = expiry.substring(0, expiry.length() - 1);
    }

    try {
      long value = Long.parseLong(expiry);
      return value * multiplier;
    } catch (NumberFormatException e) {
      return 7 * 24 * 60 * 60 * 1000L; // Default 7 days
    }
  }

  public String extractId(String token) {
    return extractClaim(token, claims -> claims.get("id", String.class));
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  public Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public Boolean validateToken(String token) {
    try {
      return !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }
}
