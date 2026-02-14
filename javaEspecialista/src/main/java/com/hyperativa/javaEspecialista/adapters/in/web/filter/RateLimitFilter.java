package com.hyperativa.javaEspecialista.adapters.in.web.filter;

import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    // Keeping MeterRegistry if it's used elsewhere, but typically MetricsPort
    // is the preferred abstraction for domain metrics.
    private final MetricsPort metricsService;
    private final int limitPerMinute;

    public RateLimitFilter(StringRedisTemplate redisTemplate,
            MetricsPort metricsService,
            @Value("${app.ratelimit.requests-per-minute:100}") int limitPerMinute) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            String clientIp = httpRequest.getRemoteAddr();
            String startKey = "rate_limit:" + clientIp;
            String requestUri = httpRequest.getRequestURI();

            Long count = redisTemplate.opsForValue().increment(startKey);
            if (count != null && count == 1) {
                redisTemplate.expire(startKey, Duration.ofMinutes(1));
            }

            if (count != null && count > limitPerMinute) {
                metricsService.incrementRateLimit(false, clientIp, requestUri);
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"title\": \"Rate Limit Exceeded\", \"status\": 429}");
                return;
            }

            metricsService.incrementRateLimit(true, clientIp, requestUri);
        }

        chain.doFilter(request, response);
    }
}
