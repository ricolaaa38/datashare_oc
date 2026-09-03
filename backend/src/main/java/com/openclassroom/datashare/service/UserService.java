package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.User;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ConflictException;
import com.openclassroom.datashare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    /** Kept in sync with the {@code minLength} declared on UserCreateRequest in openapi.yaml. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User registerUser(User user) {
        String rawPassword = user.getPassword();
        // the contract already enforces this at the web layer; repeating it here keeps
        // the rule true for any other caller of the service
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BadRequestException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        userRepository.findByLogin(user.getLogin()).ifPresent(u -> {
            throw new ConflictException("User with login " + user.getLogin() + " already exists");
        });
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    public LoginResult loginUser(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        user.setLastLogin(java.time.OffsetDateTime.now());
        user = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(user.getPassword())
                .authorities(java.util.Collections.emptyList())
                .build();

        String token = jwtService.generateToken(userDetails);
        return new LoginResult(token, user);
    }

    public record LoginResult(String token, User user) {}
}
