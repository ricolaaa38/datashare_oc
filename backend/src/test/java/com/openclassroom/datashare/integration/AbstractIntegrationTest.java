package com.openclassroom.datashare.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import software.amazon.awssdk.services.s3.S3Client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full Spring context (security filter chain, controllers, exception
 * handling) against a real PostgreSQL instance. Only the S3 client is mocked:
 * object storage behaviour is already covered by FileStorageServiceTest, and
 * running MinIO here would only slow the suite down without adding coverage
 * of our own code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // Reuses the postgres-test service already provided by tests/docker-compose.yml
    // when running there (no Docker-in-Docker needed); falls back to a disposable
    // Testcontainers instance for local runs (IDE, plain `mvn verify`).
    private static final String EXTERNAL_DB_HOST = System.getenv("DB_HOST");

    private static final PostgreSQLContainer<?> POSTGRES = EXTERNAL_DB_HOST == null
            ? new PostgreSQLContainer<>("postgres:16-alpine")
            : null;

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        if (EXTERNAL_DB_HOST != null) {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + EXTERNAL_DB_HOST + ":"
                    + System.getenv().getOrDefault("DB_PORT", "5432") + "/"
                    + System.getenv().getOrDefault("DB_NAME", "datashare_test"));
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USER", "datashare_test"));
            registry.add("spring.datasource.password",
                    () -> System.getenv().getOrDefault("DB_PASSWORD", "datashare_test"));
            return;
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    protected S3Client s3Client;

    protected void registerUser(String login, String password) throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {
                    {
                        put("login", login);
                        put("password", password);
                    }
                })))
                .andExpect(status().isCreated());
    }

    protected String login(String login, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {
                    {
                        put("login", login);
                        put("password", password);
                    }
                })))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    protected String registerAndLogin(String login, String password) throws Exception {
        registerUser(login, password);
        return login(login, password);
    }
}
