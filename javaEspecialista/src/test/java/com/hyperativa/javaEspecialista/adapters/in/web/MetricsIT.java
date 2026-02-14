package com.hyperativa.javaEspecialista.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = {
                "ENCRYPTION_KEY=12345678901234567890123456789012",
                "HASH_KEY=12345678901234567890123456789012",
                "JWT_PUBLIC_KEY=classpath:public.pem",
                "JWT_PRIVATE_KEY=classpath:private.pem"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MetricsIT {

        @Container
        @ServiceConnection
        static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

        @Container
        @ServiceConnection
        @SuppressWarnings("resource")
        static GenericContainer<?> redis = new GenericContainer<>("redis:7.0").withExposedPorts(6379);

        @Autowired
        private WebApplicationContext context;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @DynamicPropertySource
        static void redisProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.redis.host", redis::getHost);
                registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        }

        @Test
        void shouldExposeCustomMetrics() throws Exception {
                String validCard = "4407917445428994";
                String newCard = "0653169194539311"; // Valid Luhn card

                mockMvc.perform(post("/api/v1/cards").with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cardNumber\": \"" + newCard + "\"}"))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/cards").with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cardNumber\": \"" + validCard + "\"}"))
                                .andExpect(status().isCreated());

                mockMvc.perform(get("/api/v1/cards/" + validCard)
                                .with(jwt()))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/v1/cards").with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cardNumber\": \"invalid\"}"))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(get("/actuator/prometheus"))
                                .andExpect(status().isOk())
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(content().string(containsString("cards_registered_total")))
                                .andExpect(content().string(containsString("cards_already_exists_total")))
                                .andExpect(content().string(containsString("cards_validation_failed_total")))
                                .andExpect(content().string(containsString("card_lookup_total")))
                                .andExpect(content().string(containsString("card_lookup_found_total")))
                                .andExpect(content().string(containsString("card_lookup_not_found_total")))
                                .andExpect(content().string(containsString("cache_hits_total")))
                                .andExpect(content().string(containsString("cache_misses_total")))
                                .andExpect(content().string(containsString("cache_get_latency_seconds")))
                                .andExpect(content().string(containsString("cache_put_latency_seconds")))
                                .andExpect(content().string(containsString("crypto_encrypt_latency_seconds")))
                                .andExpect(content().string(containsString("crypto_failures_total")));
        }
}
