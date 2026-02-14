package com.hyperativa.javaEspecialista.adapters.in.web.exception;

import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MetricsPort metricsService;

    public GlobalExceptionHandler(MetricsPort metricsService) {
        this.metricsService = metricsService;
    }

    @ExceptionHandler(com.hyperativa.javaEspecialista.domain.exception.CardValidationException.class)
    public ProblemDetail handleCardValidation(
            com.hyperativa.javaEspecialista.domain.exception.CardValidationException e) {
        metricsService.incrementCardsValidationFailed();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Card Validation Error");
        problem.setType(URI.create("https://hyperativa.com.br/errors/card-validation"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException.class)
    public ProblemDetail handleUsernameExists(
            com.hyperativa.javaEspecialista.domain.exception.UsernameAlreadyExistsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Username Already Exists");
        problem.setType(URI.create("https://hyperativa.com.br/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        metricsService.incrementCardsValidationFailed();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid Request");
        problem.setType(Objects.requireNonNull(URI.create("https://hyperativa.com.br/errors/bad-request")));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException e) {
        metricsService.incrementCardsValidationFailed();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setType(Objects.requireNonNull(URI.create("https://hyperativa.com.br/errors/validation-error")));
        problem.setProperty("timestamp", Instant.now());

        e.getBindingResult().getFieldErrors()
                .forEach(error -> problem.setProperty("error_" + error.getField(), error.getDefaultMessage()));

        return problem;
    }

    @ExceptionHandler(com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(
            com.hyperativa.javaEspecialista.auth.domain.exception.AuthenticationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Invalid username or password");
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://hyperativa.com.br/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(org.springframework.security.authentication.BadCredentialsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Invalid username or password");
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://hyperativa.com.br/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception e) {
        // Not a validation failure, so we don't increment that counter
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(Objects.requireNonNull(URI.create("https://hyperativa.com.br/errors/internal-server-error")));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
