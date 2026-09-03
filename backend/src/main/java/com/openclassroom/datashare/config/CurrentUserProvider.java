package com.openclassroom.datashare.config;

import com.openclassroom.datashare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the database identifier of the authenticated user, so controllers do
 * not have to repeat the lookup.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        String login = authentication.getName();
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + login))
                .getId();
    }
}
