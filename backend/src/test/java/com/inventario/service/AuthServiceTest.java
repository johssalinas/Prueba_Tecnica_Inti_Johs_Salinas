package com.inventario.service;

import com.inventario.dto.LoginRequest;
import com.inventario.dto.LoginResponse;
import com.inventario.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService - Lógica de autenticación
 * Enfoque: Casos críticos de autenticación y manejo de roles
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Debe autenticar usuario y retornar token con rol sin prefijo ROLE_")
    void shouldAuthenticateUserSuccessfully() {
        // Given
        String expectedToken = "jwt.token.here";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .when(authentication).getAuthorities();
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn(expectedToken);

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole()); // Sin prefijo ROLE_
        
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtUtil, times(1)).generateToken("admin", "ADMIN");
    }

    @Test
    @DisplayName("Debe lanzar BadCredentialsException con credenciales inválidas")
    void shouldThrowExceptionWhenCredentialsAreInvalid() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Debe asignar rol USER por defecto si no hay authorities")
    void shouldUseDefaultRoleWhenNoAuthorities() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(jwtUtil.generateToken("user", "USER")).thenReturn("token");

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertEquals("USER", response.getRole());
        verify(jwtUtil).generateToken("user", "USER");
    }
}
