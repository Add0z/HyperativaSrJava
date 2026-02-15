package com.hyperativa.javaEspecialista.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.hyperativa.javaEspecialista.audit.adapters.out.persistence.repository.AuditLogRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.javaEspecialista.adapters.in.file.BatchFileAdapter;
import com.hyperativa.javaEspecialista.adapters.in.web.dto.BatchResponse;
import com.hyperativa.javaEspecialista.adapters.in.web.dto.CardRequest;
import com.hyperativa.javaEspecialista.adapters.in.web.exception.GlobalExceptionHandler;
import com.hyperativa.javaEspecialista.domain.ports.in.CardInputPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

@org.springframework.boot.test.context.SpringBootTest(properties = {
        "ENCRYPTION_KEY=12345678901234567890123456789012",
        "HASH_KEY=12345678901234567890123456789012",
        "JWT_PUBLIC_KEY=classpath:public.pem",
        "JWT_PRIVATE_KEY=classpath:private.pem",
        "spring.liquibase.enabled=false",
        "spring.data.redis.repositories.enabled=false",
        "management.health.redis.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.health.DataRedisReactiveHealthContributorAutoConfiguration,org.springframework.boot.autoconfigure.data.jdbc.DataJdbcRepositoriesAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration"
})
@ActiveProfiles("test")
@Import({ GlobalExceptionHandler.class })
class CardControllerTest {

    @MockitoBean
    private com.hyperativa.javaEspecialista.adapters.out.persistence.repo.CardRepository cardRepository;

    @MockitoBean
    private com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repo.UserRepository userRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private org.springframework.data.jdbc.core.JdbcAggregateOperations jdbcAggregateOperations;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private CardInputPort cardInputPort;

    @MockitoBean
    private BatchFileAdapter batchFileAdapter;

    @MockitoBean
    private MetricsPort metricsService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory; // Mock Redis to avoid connection issues if real one isn't
                                                           // needed

    @MockitoBean
    private StringRedisTemplate redisTemplate; // Mock RedisTemplate

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        // Mocking redis template behavior for rate limiter if necessary
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @WithMockUser
    void registerCard_ShouldReturnCreated() throws Exception {
        UUID uuid = UUID.randomUUID();
        CardRequest request = new CardRequest("1234567890123452");
        when(cardInputPort.registerCard(anyString())).thenReturn(uuid);

        mockMvc.perform(post("/api/v1/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(uuid.toString()));
    }

    @Test
    @WithMockUser
    void registerCard_WhenCardExists_ShouldReturnConflict() throws Exception {
        CardRequest request = new CardRequest("1234567890123452");
        when(cardInputPort.registerCard(anyString()))
                .thenThrow(new com.hyperativa.javaEspecialista.domain.exception.DuplicateCardException(
                        "Card already registered"));

        mockMvc.perform(post("/api/v1/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Card Already Registered"));
    }

    @Test
    @WithMockUser
    void registerCard_WhenValidationFails_ShouldReturnBadRequest() throws Exception {
        CardRequest request = new CardRequest(""); // Blank card number

        mockMvc.perform(post("/api/v1/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @WithMockUser
    void uploadFile_ShouldReturnOk() throws Exception {
        BatchResponse response = new BatchResponse(10, 8, 2, new ArrayList<>());
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        when(batchFileAdapter.processFile(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/cards")
                .file(file)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLinesProcessed").value(10));
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public javax.sql.DataSource dataSource() {
            return new javax.sql.DataSource() {
                @Override
                public java.sql.Connection getConnection() throws java.sql.SQLException {
                    java.sql.Connection connection = org.mockito.Mockito.mock(java.sql.Connection.class);
                    java.sql.DatabaseMetaData metaData = org.mockito.Mockito.mock(java.sql.DatabaseMetaData.class);
                    org.mockito.Mockito.when(connection.getMetaData()).thenReturn(metaData);
                    org.mockito.Mockito.when(metaData.getDatabaseProductName()).thenReturn("MySQL");
                    return connection;
                }

                @Override
                public java.sql.Connection getConnection(String username, String password)
                        throws java.sql.SQLException {
                    return getConnection();
                }

                @Override
                public java.io.PrintWriter getLogWriter() {
                    return null;
                }

                @Override
                public void setLogWriter(java.io.PrintWriter out) {
                }

                @Override
                public void setLoginTimeout(int seconds) {
                }

                @Override
                public int getLoginTimeout() {
                    return 0;
                }

                @Override
                public java.util.logging.Logger getParentLogger() {
                    return java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
                }

                @Override
                public <T> T unwrap(Class<T> iface) {
                    return null;
                }

                @Override
                public boolean isWrapperFor(Class<?> iface) {
                    return false;
                }
            };
        }
    }

    @Test
    @WithMockUser
    void getCardSecure_ShouldReturnOk() throws Exception {
        UUID uuid = UUID.randomUUID();
        CardRequest request = new CardRequest("1234567890123452");
        when(cardInputPort.findCardUuid(anyString())).thenReturn(Optional.of(uuid));

        mockMvc.perform(post("/api/v1/cards/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(uuid.toString()));
    }

    @Test
    @WithMockUser
    void handleIllegalArgumentException_ShouldReturnBadRequest() throws Exception {
        CardRequest request = new CardRequest("1234567890123452");
        when(cardInputPort.registerCard(anyString()))
                .thenThrow(new com.hyperativa.javaEspecialista.domain.exception.CardValidationException("Luhn failed"));

        mockMvc.perform(post("/api/v1/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Card Validation Error"))
                .andExpect(jsonPath("$.detail").value("Luhn failed"));
    }

    @Test
    @WithMockUser
    void handleGeneralException_ShouldReturnInternalServerError() throws Exception {
        CardRequest request = new CardRequest("1234567890123452");
        when(cardInputPort.registerCard(anyString())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/v1/cards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    @Test
    void getCard_WhenNotAuthenticated_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/cards/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CardRequest("1234567890123452"))))
                .andExpect(status().isUnauthorized());
    }

}
