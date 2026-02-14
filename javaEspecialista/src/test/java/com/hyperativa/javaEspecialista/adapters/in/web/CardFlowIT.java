package com.hyperativa.javaEspecialista.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full end-to-end integration test covering the complete card tokenization
 * flow:
 * 1. Register a new user
 * 2. Login and receive JWT token
 * 3. Register a card (tokenize)
 * 4. Look up the card token
 */
@SpringBootTest(properties = {
                "ENCRYPTION_KEY=12345678901234567890123456789012",
                "HASH_KEY=12345678901234567890123456789012",
                "JWT_PUBLIC_KEY=classpath:public.pem",
                "JWT_PRIVATE_KEY=classpath:private.pem"
})
@ActiveProfiles("test")
@Testcontainers
class CardFlowIT {

        @Container
        @ServiceConnection
        @SuppressWarnings("resource")
        static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

        @Container
        @ServiceConnection
        @SuppressWarnings("resource")
        static GenericContainer<?> redis = new GenericContainer<>("redis:7.0").withExposedPorts(6379);

        @DynamicPropertySource
        static void redisProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }

        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        void fullFlow_RegisterUser_Login_RegisterCard_LookupCard() throws Exception {
                // Step 1: Register user
                String registerJson = """
                                {"username": "flowuser", "password": "FlowPass1!xy"}
                                """;

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerJson))
                                .andExpect(status().isCreated());

                // Step 2: Login and get JWT token
                String loginJson = """
                                {"username": "flowuser", "password": "FlowPass1!xy"}
                                """;

                MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").isNotEmpty())
                                .andReturn();

                JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
                String token = loginResponse.get("token").asText();

                // Step 3: Register a card
                String cardJson = """
                                {"cardNumber": "4539578763621486"}
                                """;

                MvcResult cardResult = mockMvc.perform(post("/api/v1/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token)
                                .content(cardJson))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").isNotEmpty())
                                .andReturn();

                JsonNode cardResponse = objectMapper.readTree(cardResult.getResponse().getContentAsString());
                String cardToken = cardResponse.get("token").asText();

                // Step 4: Lookup the card
                String lookupJson = """
                                {"cardNumber": "4539578763621486"}
                                """;

                mockMvc.perform(post("/api/v1/cards/lookup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token)
                                .content(lookupJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value(cardToken));

                // Step 5: Registering the same card returns 409 Conflict
                mockMvc.perform(post("/api/v1/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token)
                                .content(cardJson))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.title").value("Card Already Registered"));
        }
}
