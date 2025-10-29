package com.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventario.dto.PageResponse;
import com.inventario.dto.ProductoRequest;
import com.inventario.dto.ProductoResponse;
import com.inventario.exception.DuplicateResourceException;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.repository.UserRepository;
import com.inventario.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductoController - Contrato REST API")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductoService productoService;

    @MockitoBean
    private UserRepository userRepository;

    private ProductoResponse productoResponse;
    private ProductoRequest productoRequest;

    @BeforeEach
    void setUp() {
        productoResponse = new ProductoResponse();
        productoResponse.setId(1L);
        productoResponse.setNombre("Laptop HP");
        productoResponse.setCategoria("Electrónica");
        productoResponse.setProveedor("HP Inc");
        productoResponse.setPrecio(new BigDecimal("999.99"));
        productoResponse.setStock(10);
        productoResponse.setFechaRegistro(LocalDateTime.now());

        productoRequest = new ProductoRequest();
        productoRequest.setNombre("Laptop HP");
        productoRequest.setCategoria("Electrónica");
        productoRequest.setProveedor("HP Inc");
        productoRequest.setPrecio(new BigDecimal("999.99"));
        productoRequest.setStock(10);
    }

    @Test
    @DisplayName("GET /api/productos - Debe retornar 401 sin autenticación")
    void getAllProductos_DebeRetornar401SinAuth() throws Exception {
        mockMvc.perform(get("/api/productos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/productos - Debe retornar página de productos")
    void getAllProductos_DebeRetornarPaginaDeProductos() throws Exception {
        PageResponse<ProductoResponse> pageResponse = new PageResponse<>(
            Collections.singletonList(productoResponse),
            0,
            10,
            1L,
            1,
            true,
            true
        );

        when(productoService.getAllProductos(null, null, 0, 10, "fechaRegistro", "desc"))
            .thenReturn(pageResponse);

        mockMvc.perform(get("/api/productos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nombre").value("Laptop HP"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(productoService).getAllProductos(null, null, 0, 10, "fechaRegistro", "desc");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/productos - Debe aplicar filtros correctamente")
    void getAllProductos_DebeAplicarFiltros() throws Exception {
        PageResponse<ProductoResponse> pageResponse = new PageResponse<>(
            Collections.emptyList(),
            0,
            20,
            0L,
            0,
            true,
            true
        );

        when(productoService.getAllProductos("laptop", "Electrónica", 0, 20, "nombre", "asc"))
            .thenReturn(pageResponse);

        mockMvc.perform(get("/api/productos")
                .param("search", "laptop")
                .param("categoria", "Electrónica")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "nombre")
                .param("sortDir", "asc"))
            .andExpect(status().isOk());

        verify(productoService).getAllProductos("laptop", "Electrónica", 0, 20, "nombre", "asc");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/productos/{id} - Debe retornar 404 si no existe")
    void getProductoById_DebeRetornar404SiNoExiste() throws Exception {
        when(productoService.getProductoById(999L))
            .thenThrow(new ResourceNotFoundException("Producto", "id", 999L));

        mockMvc.perform(get("/api/productos/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Producto no encontrado con id: '999'"));

        verify(productoService).getProductoById(999L);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/productos - Debe retornar 201 con Location header")
    void createProducto_DebeRetornar201ConLocationHeader() throws Exception {
        when(productoService.createProducto(any(ProductoRequest.class)))
            .thenReturn(productoResponse);

        mockMvc.perform(post("/api/productos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoRequest)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombre").value("Laptop HP"));

        verify(productoService).createProducto(any(ProductoRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/productos - Debe retornar 400 con validación fallida")
    void createProducto_DebeRetornar400ConValidacionFallida() throws Exception {
        productoRequest.setNombre("");
        productoRequest.setPrecio(new BigDecimal("-10"));

        mockMvc.perform(post("/api/productos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.nombre").exists())
            .andExpect(jsonPath("$.errors.precio").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/productos - Debe retornar 409 con nombre duplicado")
    void createProducto_DebeRetornar409ConNombreDuplicado() throws Exception {
        when(productoService.createProducto(any(ProductoRequest.class)))
            .thenThrow(new DuplicateResourceException("Ya existe un producto con el nombre: Laptop HP"));

        mockMvc.perform(post("/api/productos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Ya existe un producto con el nombre: Laptop HP"));

        verify(productoService).createProducto(any(ProductoRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/productos/{id} - Debe retornar 409 con nombre duplicado")
    void updateProducto_DebeRetornar409ConNombreDuplicado() throws Exception {
        when(productoService.updateProducto(eq(1L), any(ProductoRequest.class)))
            .thenThrow(new DuplicateResourceException("Ya existe otro producto con el nombre: Laptop HP"));

        mockMvc.perform(put("/api/productos/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productoRequest)))
            .andExpect(status().isConflict());

        verify(productoService).updateProducto(eq(1L), any(ProductoRequest.class));
    }
}
