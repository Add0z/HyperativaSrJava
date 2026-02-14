package com.hyperativa.javaEspecialista.auth.adapters.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.javaEspecialista.adapters.in.web.exception.GlobalExceptionHandler;
import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

@org.springframework.boot.test.context.SpringBootTest(properties = {
        "JWT_PUBLIC_KEY=classpath:public.pem",
        "JWT_PRIVATE_KEY=classpath:private.pem",
        "spring.liquibase.enabled=false",
        "ENCRYPTION_KEY=12345678901234567890123456789012",
        "HASH_KEY=12345678901234567890123456789012",
        "spring.data.redis.repositories.enabled=false",
        "management.health.redis.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.health.DataRedisReactiveHealthContributorAutoConfiguration,org.springframework.boot.autoconfigure.data.jdbc.DataJdbcRepositoriesAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration"
})
@ActiveProfiles("test")
@Import({ GlobalExceptionHandler.class })
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AuthInputPort authService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MetricsPort metricsService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private com.hyperativa.javaEspecialista.adapters.out.persistence.repo.CardRepository cardRepository;

    @MockitoBean
    private com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repo.UserRepository userRepository;

    @MockitoBean
    private com.hyperativa.javaEspecialista.adapters.out.persistence.repo.AuditLogRepository auditLogRepository;

    @MockitoBean
    private org.springframework.data.jdbc.core.JdbcAggregateOperations jdbcAggregateOperations;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @WithMockUser
    void login_ShouldReturnToken() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("user", "pass");
        when(authService.login(anyString(), anyString())).thenReturn("test-token");

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    @WithMockUser
    void login_WhenInvalid_ShouldReturnUnauthorized() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("user", "pass");
        when(authService.login(anyString(), anyString()))
                .thenThrow(
                        new com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException("Invalid"));

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void register_ShouldReturnOk() throws Exception {
        AuthController.RegisterRequest request = new AuthController.RegisterRequest("user", "StrongPass1!");
        doNothing().when(authService).register(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void authResponse_ShouldHaveToken() {
        AuthController.AuthResponse response = new AuthController.AuthResponse("token");
        assertEquals("token", response.token());
    }

    @Test
    void authRequest_ShouldHaveFields() {
        AuthController.AuthRequest request = new AuthController.AuthRequest("u", "p");
        assertEquals("u", request.username());
        assertEquals("p", request.password());
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

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
