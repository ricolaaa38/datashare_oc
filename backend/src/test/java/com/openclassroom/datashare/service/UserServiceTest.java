package com.openclassroom.datashare.service;

import com.openclassroom.datashare.entity.User;
import com.openclassroom.datashare.exception.BadRequestException;
import com.openclassroom.datashare.exception.ConflictException;
import com.openclassroom.datashare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void aPasswordShorterThanTheMinimumIsRejected() {
        String tooShort = "a".repeat(UserService.MIN_PASSWORD_LENGTH - 1);

        assertThatThrownBy(() -> userService.registerUser(user("bob@datashare.test", tooShort)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(String.valueOf(UserService.MIN_PASSWORD_LENGTH));

        verify(userRepository, never()).save(any());
    }

    @Test
    void aMissingPasswordIsRejected() {
        assertThatThrownBy(() -> userService.registerUser(user("bob@datashare.test", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aPasswordAtTheMinimumLengthIsAcceptedAndStoredHashed() {
        String password = "a".repeat(UserService.MIN_PASSWORD_LENGTH);
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User saved = userService.registerUser(user("bob@datashare.test", password));

        assertThat(saved.getPassword()).isEqualTo("hashed");
    }

    @Test
    void anAlreadyRegisteredLoginIsAConflict() {
        when(userRepository.findByLogin("bob@datashare.test")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser(user("bob@datashare.test", "longenoughpassword")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    private User user(String login, String password) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        return user;
    }
}
