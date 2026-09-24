package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.User;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ConflictException;
import com.openclassroom.datashare.exception.InvalidCredentialsException;
import com.openclassroom.datashare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing user registration and authentication.
 * Provides methods for registering new users and logging in existing users.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    /**
     * Kept in sync with the {@code minLength} declared on UserCreateRequest in
     * openapi.yaml.
     */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user in the system.
     * Validates the password length and checks for existing users with the same login.
     *
     * @param user The user entity containing registration details.
     * @return The registered user entity.
     * @throws BadRequestException if the password is too short.
     * @throws ConflictException if a user with the same login already exists.
     */
    public User registerUser(User user) {
        String rawPassword = user.getPassword();
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

    /**
     * Authenticates a user with the provided login and password.
     * If authentication is successful, generates a JWT token for the user.
     *
     * @param login    The login of the user.
     * @param password The password of the user.
     * @return A LoginResult containing the JWT token and user details.
     * @throws InvalidCredentialsException if the login or password is incorrect.
     */
    public LoginResult loginUser(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
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

    /**
     * A record representing the result of a successful login operation.
     * Contains the generated JWT token and the authenticated user details.
     */
    public record LoginResult(String token, User user) {
    }
}
