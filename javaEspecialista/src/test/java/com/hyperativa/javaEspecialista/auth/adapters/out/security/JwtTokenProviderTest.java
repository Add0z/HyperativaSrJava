package com.hyperativa.javaEspecialista.auth.adapters.out.security;

import com.hyperativa.javaEspecialista.auth.domain.model.Role;
import com.hyperativa.javaEspecialista.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(jwtEncoder);
        ReflectionTestUtils.setField(tokenProvider, "expirationSeconds", 3600);
    }

    @Test
    void generateToken_ShouldEncodeCorrectClaims() {
        User user = new User(UUID.randomUUID(), "testuser", "pass", Set.of(Role.USER, Role.ADMIN));
        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("test-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = tokenProvider.generateToken(user);

        assertEquals("test-token", token);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtEncoderParameters params = captor.getValue();
        assertNotNull(params.getClaims());
        assertEquals("testuser", params.getClaims().getSubject());
        assertEquals("https://hyperativa.com", params.getClaims().getIssuer().toString());

        String scope = (String) params.getClaims().getClaim("scope");
        assertNotNull(scope);
        assertEquals(2, scope.split(" ").length);
    }
}
