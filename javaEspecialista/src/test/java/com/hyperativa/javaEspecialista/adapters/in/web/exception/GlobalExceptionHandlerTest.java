package com.hyperativa.javaEspecialista.adapters.in.web.exception;

import com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException;
import com.hyperativa.javaEspecialista.domain.exception.CardValidationException;
import com.hyperativa.javaEspecialista.domain.exception.DuplicateCardException;
import com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MetricsPort metricsService;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(metricsService);
    }

    @Test
    void handleCardValidation_ShouldReturnBadRequestAndIncrementMetric() {
        CardValidationException ex = new CardValidationException("Invalid card");

        ProblemDetail problem = exceptionHandler.handleCardValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Invalid card", problem.getDetail());
        assertEquals("Card Validation Error", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/card-validation"), problem.getType());
        verify(metricsService).incrementCardsValidationFailed();
    }

    @Test
    void handleUsernameExists_ShouldReturnConflict() {
        UsernameAlreadyExistsException ex = new UsernameAlreadyExistsException("User exists");

        ProblemDetail problem = exceptionHandler.handleUsernameExists(ex);

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Username already exists: User exists", problem.getDetail());
        assertEquals("Username Already Exists", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/conflict"), problem.getType());
    }

    @Test
    void handleDuplicateCard_ShouldReturnConflict() {
        DuplicateCardException ex = new DuplicateCardException("Card exists");

        ProblemDetail problem = exceptionHandler.handleDuplicateCard(ex);

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Card exists", problem.getDetail());
        assertEquals("Card Already Registered", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/duplicate-card"), problem.getType());
    }

    @Test
    void handleIllegalArgument_ShouldReturnBadRequestAndIncrementMetric() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad arg");

        ProblemDetail problem = exceptionHandler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Bad arg", problem.getDetail());
        assertEquals("Invalid Request", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/bad-request"), problem.getType());
        verify(metricsService).incrementCardsValidationFailed();
    }

    @Test
    void handleValidationErrors_ShouldReturnBadRequestAndIncrementMetric() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "defaultMessage");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail problem = exceptionHandler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Validation failed", problem.getDetail());
        assertEquals("Validation Error", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/validation-error"), problem.getType());
        assertEquals("defaultMessage", problem.getProperties().get("error_field"));
        verify(metricsService).incrementCardsValidationFailed();
    }

    @Test
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        AuthenticationException ex = new AuthenticationException("Auth failed");

        ProblemDetail problem = exceptionHandler.handleAuthenticationException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), problem.getStatus());
        assertEquals("Invalid username or password", problem.getDetail());
        assertEquals("Unauthorized", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/unauthorized"), problem.getType());
    }

    @Test
    void handleBadCredentials_ShouldReturnUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Bad creds");

        ProblemDetail problem = exceptionHandler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), problem.getStatus());
        assertEquals("Invalid username or password", problem.getDetail());
        assertEquals("Unauthorized", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/unauthorized"), problem.getType());
    }

    @Test
    void handleAccessDenied_ShouldReturnForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Denied");

        ProblemDetail problem = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), problem.getStatus());
        assertEquals("Access Denied", problem.getDetail());
        assertEquals("Forbidden", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/forbidden"), problem.getType());
    }

    @Test
    void handleGeneralException_ShouldReturnInternalServerError() {
        Exception ex = new Exception("Boom");

        ProblemDetail problem = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("An unexpected error occurred", problem.getDetail());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(URI.create("https://hyperativa.com.br/errors/internal-server-error"), problem.getType());
    }
}
