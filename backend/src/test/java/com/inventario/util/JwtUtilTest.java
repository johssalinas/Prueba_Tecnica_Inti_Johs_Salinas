package com.inventario.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para JwtUtil - Componente crítico de seguridad
 * Enfoque: Solo casos esenciales y críticos para la funcionalidad
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-256-bits-encryption";
    private static final long TEST_EXPIRATION = 3600000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Debe generar y validar token JWT correctamente")
    void shouldGenerateAndValidateToken() {
        // Given
        String username = "admin";
        String role = "ADMIN";

        // When
        String token = jwtUtil.generateToken(username, role);

        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // Header.Payload.Signature
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.extractUsername(token));
        assertEquals(role, jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Debe rechazar token con firma inválida")
    void shouldRejectTokenWithInvalidSignature() {
        // Given
        JwtUtil differentSecretUtil = new JwtUtil(
            "different-secret-key-that-is-also-long-enough-for-256-bits", 
            TEST_EXPIRATION
        );
        String token = differentSecretUtil.generateToken("user", "USER");

        // When/Then
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Debe rechazar token expirado")
    void shouldRejectExpiredToken() throws InterruptedException {
        // Given - Token con expiración de 1 segundo
        JwtUtil shortLivedUtil = new JwtUtil(TEST_SECRET, 1000L);
        String token = shortLivedUtil.generateToken("user", "USER");

        // When - Esperar a que expire
        Thread.sleep(1500);

        // Then
        assertFalse(shortLivedUtil.validateToken(token));
    }

    @Test
    @DisplayName("Debe rechazar tokens malformados o inválidos")
    void shouldRejectMalformedTokens() {
        assertFalse(jwtUtil.validateToken("invalid.token.format"));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    @DisplayName("Debe validar longitud mínima del secret key")
    void shouldValidateSecretKeyLength() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new JwtUtil("tooshort", TEST_EXPIRATION)
        );
        
        assertTrue(exception.getMessage().contains("256 bits"));
    }
}
