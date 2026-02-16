package com.hyperativa.javaEspecialista.adapters.out.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityAdapterTest {

    private SecurityAdapter securityAdapter;

    @BeforeEach
    void setUp() {
        securityAdapter = new SecurityAdapter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getCurrentUser_WhenAuthenticationIsNull_ShouldReturnSystem() {
        SecurityContextHolder.clearContext();
        assertEquals("system", securityAdapter.getCurrentUser());
    }

    @Test
    void getCurrentUser_WhenNotAuthenticated_ShouldReturnSystem() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        setAuthentication(auth);

        assertEquals("system", securityAdapter.getCurrentUser());
    }

    @Test
    void getCurrentUser_WhenAnonymousUser_ShouldReturnSystem() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        setAuthentication(auth);

        assertEquals("system", securityAdapter.getCurrentUser());
    }

    @Test
    void getCurrentUser_WhenJwtPrincipal_ShouldReturnSubject() {
        Authentication auth = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-uuid");

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(jwt);
        setAuthentication(auth);

        assertEquals("user-uuid", securityAdapter.getCurrentUser());
    }

    @Test
    void getCurrentUser_WhenStringPrincipal_ShouldReturnPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user-email@example.com");
        setAuthentication(auth);

        assertEquals("user-email@example.com", securityAdapter.getCurrentUser());
    }

    @Test
    void getCurrentUser_WhenUnknownPrincipalType_ShouldReturnName() {
        Authentication auth = mock(Authentication.class);
        Object unknownPrincipal = new Object();

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(unknownPrincipal);
        when(auth.getName()).thenReturn("fallback-name");
        setAuthentication(auth);

        assertEquals("fallback-name", securityAdapter.getCurrentUser());
    }

    @Test
    void getCurrentIp_WhenNoRequestAttributes_ShouldReturnNull() {
        RequestContextHolder.resetRequestAttributes();
        assertNull(securityAdapter.getCurrentIp());
    }

    @Test
    void getCurrentIp_WhenXForwardedForExists_ShouldReturnFirstIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("10.0.0.1", securityAdapter.getCurrentIp());
    }

    @Test
    void getCurrentIp_WhenXForwardedForIsEmpty_ShouldReturnRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("127.0.0.1", securityAdapter.getCurrentIp());
    }

    @Test
    void getCurrentIp_WhenXForwardedForIsNull_ShouldReturnRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("127.0.0.1", securityAdapter.getCurrentIp());
    }

    private void setAuthentication(Authentication auth) {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
