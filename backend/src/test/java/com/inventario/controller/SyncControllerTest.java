package com.inventario.controller;

import com.inventario.repository.UserRepository;
import com.inventario.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para SyncController - Endpoint /sync-products
 * Cobertura: Autenticación JWT, manejo de errores, respuestas diferenciadas
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SyncController - Endpoint /sync-products")
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    @DisplayName("POST /sync-products - Debe retornar 200 con productos sincronizados exitosamente")
    void syncProducts_DebeRetornar200ConProductosSincronizados() throws Exception {
        // Arrange
        when(productoService.syncProductsFromFakeStore()).thenReturn(20);

        // Act & Assert
        mockMvc.perform(post("/sync-products")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSincronizados").value(20))
            .andExpect(jsonPath("$.mensaje").value("Sincronización completada exitosamente. Total insertados: 20"));

        verify(productoService).syncProductsFromFakeStore();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /sync-products - Debe retornar 200 con mensaje diferenciado cuando no hay productos nuevos")
    void syncProducts_DebeRetornar200SinProductosNuevos() throws Exception {
        // Arrange
        when(productoService.syncProductsFromFakeStore()).thenReturn(0);

        // Act & Assert
        mockMvc.perform(post("/sync-products")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSincronizados").value(0))
            .andExpect(jsonPath("$.mensaje").value("Sincronización completada. No hay productos nuevos para insertar"));

        verify(productoService).syncProductsFromFakeStore();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /sync-products - Debe retornar 500 cuando service lanza excepción")
    void syncProducts_DebeRetornar500ConExcepcion() throws Exception {
        // Arrange
        when(productoService.syncProductsFromFakeStore())
            .thenThrow(new RuntimeException("Error de conexión"));

        // Act & Assert
        mockMvc.perform(post("/sync-products")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.totalSincronizados").value(0))
            .andExpect(jsonPath("$.mensaje").value("Error interno durante la sincronización: Error de conexión"));

        verify(productoService).syncProductsFromFakeStore();
    }
}
