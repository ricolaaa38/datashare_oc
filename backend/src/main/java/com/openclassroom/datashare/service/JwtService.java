package com.openclassroom.datashare.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Service for handling JWT (JSON Web Token) operations such as token
 * generation, validation, and extraction of user information.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    /**
     * Generates a JWT token for the given user details.
     *
     * @param userDetails The user details for which the token is generated.
     * @return A JWT token as a String.
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Retrieves the signing key used for JWT operations.
     *
     * @return A SecretKey for signing and verifying JWT tokens.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extracts the login (username) from the given JWT token.
     *
     * @param token The JWT token from which to extract the login.
     * @return The login (username) contained in the token.
     */
    public String getLoginFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates the given JWT token.
     *
     * @param token The JWT token to validate.
     * @return true if the token is valid; false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}