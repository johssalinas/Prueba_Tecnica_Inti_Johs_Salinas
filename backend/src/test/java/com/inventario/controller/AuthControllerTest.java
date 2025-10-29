package com.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.config.CustomAuthenticationProvider;
import com.inventario.config.JwtAuthenticationFilter;
import com.inventario.dto.LoginRequest;
import com.inventario.dto.LoginResponse;
import com.inventario.repository.UserRepository;
import com.inventario.service.AuthService;
import com.inventario.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomAuthenticationProvider customAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /login - Debe retornar 200 con token, username y role cuando credenciales válidas")
    void shouldReturn200AndTokenWhenCredentialsAreValid() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password123");
        
        LoginResponse response = new LoginResponse("jwt.token.here", "admin", "ADMIN");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /login - Debe retornar 401 cuando credenciales inválidas")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("invalid");
        request.setPassword("wrong");
        
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }
}
