package com.hyperativa.javaEspecialista.adapters.in.web.filter;

import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MetricsPort metricsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private RateLimitFilter rateLimitFilter;
    private final int limitPerMinute = 2;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitFilter = new RateLimitFilter(redisTemplate, metricsService, limitPerMinute);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimitFilter.doFilter(request, response, chain);

        verify(redisTemplate).expire(eq("rate_limit:127.0.0.1"), any(Duration.class));
        verify(metricsService).incrementRateLimit(true, "127.0.0.1", "/api/test");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldBlockRequestWhenOverLimit() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(valueOperations.increment(anyString())).thenReturn(3L);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        rateLimitFilter.doFilter(request, response, chain);

        verify(metricsService).incrementRateLimit(false, "127.0.0.1", "/api/test");
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldIncrementButNotExpireWhenCountGreaterThanOne() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(valueOperations.increment(anyString())).thenReturn(2L);

        rateLimitFilter.doFilter(request, response, chain);

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSkipRateLimitingWhenNotHttpServletRequest() throws ServletException, IOException {
        ServletRequest nonHttpRequest = mock(ServletRequest.class);
        ServletResponse nonHttpResponse = mock(ServletResponse.class);

        rateLimitFilter.doFilter(nonHttpRequest, nonHttpResponse, chain);

        verifyNoInteractions(redisTemplate);
        verify(chain).doFilter(nonHttpRequest, nonHttpResponse);
    }
}
