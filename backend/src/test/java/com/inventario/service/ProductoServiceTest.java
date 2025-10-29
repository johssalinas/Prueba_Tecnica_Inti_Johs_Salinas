package com.inventario.service;

import com.inventario.dto.ProductoRequest;
import com.inventario.exception.DuplicateResourceException;
import com.inventario.model.Producto;
import com.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService - Lógica de Negocio Crítica")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private ProductoRequest productoRequest;

    @BeforeEach
    void setUp() {
        productoRequest = new ProductoRequest();
        productoRequest.setNombre("Laptop HP");
        productoRequest.setCategoria("Electrónica");
        productoRequest.setProveedor("HP Inc");
        productoRequest.setPrecio(new BigDecimal("999.99"));
        productoRequest.setStock(10);
    }

    @Test
    @DisplayName("getAllProductos - Debe normalizar búsqueda vacía a null (Prevención SQL)")
    void getAllProductos_DebeNormalizarBusquedaVacia() {
        Page<Producto> page = new PageImpl<>(Collections.emptyList());
        when(productoRepository.findByFilters(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(page);

        productoService.getAllProductos("   ", null, 0, 10, "nombre", "asc");

        verify(productoRepository).findByFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getAllProductos - Debe validar sortBy contra whitelist (Prevención SQL Injection)")
    void getAllProductos_DebeValidarSortByContraWhitelist() {
        Page<Producto> page = new PageImpl<>(Collections.emptyList());
        when(productoRepository.findByFilters(any(), any(), any(Pageable.class)))
            .thenReturn(page);

        productoService.getAllProductos(null, null, 0, 10, "INVALID_FIELD", "asc");

        verify(productoRepository).findByFilters(any(), any(), argThat(pageable -> 
            pageable.getSort().toString().contains("fechaRegistro")
        ));
    }

    @Test
    @DisplayName("getAllProductos - Debe limitar tamaño de página a MAX_PAGE_SIZE (Prevención DoS)")
    void getAllProductos_DebeLimitarTamanioPagina() {
        Page<Producto> page = new PageImpl<>(Collections.emptyList());
        when(productoRepository.findByFilters(any(), any(), any(Pageable.class)))
            .thenReturn(page);

        productoService.getAllProductos(null, null, 0, 200, "nombre", "asc");

        verify(productoRepository).findByFilters(any(), any(), argThat(pageable -> 
            pageable.getPageSize() == 100
        ));
    }

    @Test
    @DisplayName("createProducto - Debe trimear campos de texto (Data Sanitization)")
    void createProducto_DebeTrimmearCamposTexto() {
        productoRequest.setNombre("  Laptop HP  ");
        productoRequest.setCategoria("  Electrónica  ");
        productoRequest.setProveedor("  HP Inc  ");
        
        Producto producto = new Producto();
        producto.setId(1L);
        when(productoRepository.existsByNombre("Laptop HP")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        productoService.createProducto(productoRequest);

        verify(productoRepository).existsByNombre("Laptop HP");
        verify(productoRepository).save(argThat(p -> 
            p.getNombre().equals("Laptop HP") &&
            p.getCategoria().equals("Electrónica") &&
            p.getProveedor().equals("HP Inc")
        ));
    }

    @Test
    @DisplayName("createProducto - Debe lanzar DuplicateResourceException si nombre existe")
    void createProducto_DebeLanzarExcepcionSiNombreExiste() {
        when(productoRepository.existsByNombre("Laptop HP")).thenReturn(true);

        assertThatThrownBy(() -> productoService.createProducto(productoRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Ya existe un producto con el nombre");

        verify(productoRepository).existsByNombre("Laptop HP");
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProducto - Debe permitir mismo nombre en mismo producto")
    void updateProducto_DebePermitirMismoNombreEnMismoProducto() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Laptop HP");
        when(productoRepository.findById(1L)).thenReturn(java.util.Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        productoService.updateProducto(1L, productoRequest);

        verify(productoRepository, never()).existsByNombreAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("updateProducto - Debe validar nombre duplicado en otro producto")
    void updateProducto_DebeValidarNombreDuplicadoEnOtroProducto() {
        productoRequest.setNombre("Nuevo Nombre");
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Laptop HP");
        when(productoRepository.findById(1L)).thenReturn(java.util.Optional.of(producto));
        when(productoRepository.existsByNombreAndIdNot("Nuevo Nombre", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productoService.updateProducto(1L, productoRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Ya existe otro producto con el nombre");

        verify(productoRepository).existsByNombreAndIdNot("Nuevo Nombre", 1L);
        verify(productoRepository, never()).save(any());
    }
}
