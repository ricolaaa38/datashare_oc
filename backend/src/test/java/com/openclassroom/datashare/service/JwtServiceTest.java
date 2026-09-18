package com.openclassroom.datashare.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 60_000);
    }

    @Test
    void generatedTokenCanBeValidatedAndReadBack() {
        UserDetails user = new User("sam@datashare.test", "hashed", List.of());

        String token = jwtService.generateToken(user);

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getLoginFromToken(token)).isEqualTo("sam@datashare.test");
    }

    @Test
    void malformedTokenIsNotValid() {
        assertThat(jwtService.validateToken("not-a-jwt")).isFalse();
    }
}