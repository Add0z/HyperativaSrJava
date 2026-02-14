package com.hyperativa.javaEspecialista.auth.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserRoleEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repo.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = {
                "ENCRYPTION_KEY=12345678901234567890123456789012",
                "HASH_KEY=12345678901234567890123456789012",
                "JWT_PUBLIC_KEY=classpath:public.pem",
                "JWT_PRIVATE_KEY=classpath:private.pem"
})
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {

        @Container
        @ServiceConnection
        @SuppressWarnings("resource")
        static MySQLContainer<?> mysql = new MySQLContainer<>(
                        "mysql:8.0");

        @Container
        @ServiceConnection
        @SuppressWarnings("resource")
        static GenericContainer<?> redis = new GenericContainer<>(
                        "redis:7.0").withExposedPorts(6379);

        @DynamicPropertySource
        static void redisProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }

        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        private ObjectMapper objectMapper = new ObjectMapper();

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                var user = userRepository.findByUsername("admin").orElse(null);
                if (user == null) {
                        var roles = java.util.Set.of(
                                        new UserRoleEntity(
                                                        "ADMIN"));
                        user = new UserEntity(
                                        java.util.UUID.randomUUID().toString(), "admin",
                                        passwordEncoder.encode("admin"), roles);
                } else {
                        user.setPassword(passwordEncoder.encode("admin"));
                }
                userRepository.save(user);
        }

        @AfterEach
        void tearDown() {
                userRepository.deleteAll();
        }

        @Test
        void shouldLoginSuccessfully() throws Exception {
                // Arrange
                var request = new AuthController.AuthRequest("admin", "admin");

                // Act & Assert
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        void shouldFailLoginWithInvalidCredentials() throws Exception {
                // Arrange
                var request = new AuthController.AuthRequest("admin", "wrongpassword");

                // Act & Assert
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }
}
