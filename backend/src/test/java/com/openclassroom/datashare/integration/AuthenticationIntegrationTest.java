package com.openclassroom.datashare.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critical path: register, login and use the token against a protected route,
 * through the real security filter chain and exception handling.
 */
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registeringThenLoggingInGrantsAccessToProtectedResources() throws Exception {
        String token = registerAndLogin("sam@datashare.test", "longenoughpassword");

        mockMvc.perform(get("/files").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void aProtectedResourceIsRejectedWithoutACredential() throws Exception {
        mockMvc.perform(get("/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggingInWithTheWrongPasswordIsRejected() throws Exception {
        registerUser("bob@datashare.test", "longenoughpassword");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"bob@datashare.test\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeringTheSameLoginTwiceIsAConflict() throws Exception {
        registerUser("dup@datashare.test", "longenoughpassword");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"dup@datashare.test\",\"password\":\"longenoughpassword\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registeringWithATooShortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"short@datashare.test\",\"password\":\"1234567\"}"))
                .andExpect(status().isBadRequest());
    }
}
