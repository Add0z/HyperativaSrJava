package com.hyperativa.javaEspecialista.auth.adapters.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.javaEspecialista.adapters.in.web.exception.GlobalExceptionHandler;
import com.hyperativa.javaEspecialista.auth.domain.port.in.AuthInputPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthInputPort authService;

    @Mock
    private MetricsPort metricsService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler(metricsService))
                .build();
    }

    @Test
    void login_ShouldReturnToken() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("user", "pass");
        com.hyperativa.javaEspecialista.auth.domain.model.TokenPair tokenPair = new com.hyperativa.javaEspecialista.auth.domain.model.TokenPair(
                "test-access", "test-refresh", "Bearer", 3600);

        when(authService.login(anyString(), anyString())).thenReturn(tokenPair);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh"));
    }

    @Test
    void login_WhenInvalid_ShouldReturnUnauthorized() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("user", "pass");
        when(authService.login(anyString(), anyString()))
                .thenThrow(
                        new com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException("Invalid"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_ShouldReturnOk() throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        AuthController.RegisterRequest request = new AuthController.RegisterRequest("user", "StrongPass1!", roles);
        doNothing().when(authService).register(anyString(), anyString(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_WithInvalidRole_ShouldIgnoreAndReturnOk() throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add("INVALID_ROLE");
        AuthController.RegisterRequest request = new AuthController.RegisterRequest("user", "StrongPass1!", roles);
        doNothing().when(authService).register(anyString(), anyString(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_WithNullRoles_ShouldReturnOk() throws Exception {
        AuthController.RegisterRequest request = new AuthController.RegisterRequest("user", "StrongPass1!", null);
        doNothing().when(authService).register(anyString(), anyString(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void authResponse_ShouldHaveToken() {
        AuthController.AuthResponse response = new AuthController.AuthResponse("acc", "ref", "Bearer", 3600);
        assertEquals("acc", response.accessToken());
        assertEquals("ref", response.refreshToken());
    }

    @Test
    void authRequest_ShouldHaveFields() {
        AuthController.AuthRequest request = new AuthController.AuthRequest("u", "p");
        assertEquals("u", request.username());
        assertEquals("p", request.password());
    }

    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
