package com.jorge.customers.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    @Value("${application.jwt.key}")
    private String SECRET_KEY;
    @Value("${application.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(String dni) {
        log.debug("Generating token");
        return buildToken(new HashMap<>(), dni, jwtExpiration);
    }

    private String buildToken(Map<String, Object> claims, String dni, long expiration) {
        log.debug("Building token");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(dni)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getKey() {
        log.debug("Decoding secret key");
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
