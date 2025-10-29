package com.inventario.service;

import com.inventario.dto.PageResponse;
import com.inventario.dto.ProductoRequest;
import com.inventario.dto.ProductoResponse;
import com.inventario.exception.DuplicateResourceException;
import com.inventario.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService - Tests Unitarios")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;
    private ProductoRequest productoRequest;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Laptop HP");
        producto.setCategoria("Electrónica");
        producto.setProveedor("HP Inc");
        producto.setPrecio(new BigDecimal("999.99"));
        producto.setStock(10);
        producto.setFechaRegistro(LocalDateTime.now());

        productoRequest = new ProductoRequest();
        productoRequest.setNombre("Laptop HP");
        productoRequest.setCategoria("Electrónica");
        productoRequest.setProveedor("HP Inc");
        productoRequest.setPrecio(new BigDecimal("999.99"));
        productoRequest.setStock(10);
    }

    @Test
    @DisplayName("getAllProductos - Debe retornar página de productos con paginación")
    void getAllProductos_DebeRetornarPaginaDeProductos() {
        Page<Producto> page = new PageImpl<>(Collections.singletonList(producto));
        when(productoRepository.findByFilters(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(page);

        PageResponse<ProductoResponse> result = productoService.getAllProductos(
            null, null, 0, 10, "fechaRegistro", "desc"
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(productoRepository).findByFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getAllProductos - Debe normalizar búsqueda vacía a null")
    void getAllProductos_DebeNormalizarBusquedaVacia() {
        Page<Producto> page = new PageImpl<>(Collections.emptyList());
        when(productoRepository.findByFilters(isNull(), isNull(), any(Pageable.class)))
            .thenReturn(page);

        productoService.getAllProductos("   ", null, 0, 10, "nombre", "asc");

        verify(productoRepository).findByFilters(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getAllProductos - Debe validar sortBy contra whitelist")
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
    @DisplayName("getAllProductos - Debe limitar tamaño de página a MAX_PAGE_SIZE")
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
    @DisplayName("getProductoById - Debe retornar producto cuando existe")
    void getProductoById_DebeRetornarProductoCuandoExiste() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        ProductoResponse result = productoService.getProductoById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Laptop HP");
        verify(productoRepository).findById(1L);
    }

    @Test
    @DisplayName("getProductoById - Debe lanzar ResourceNotFoundException cuando no existe")
    void getProductoById_DebeLanzarExcepcionCuandoNoExiste() {
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.getProductoById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Producto")
            .hasMessageContaining("id")
            .hasMessageContaining("999");

        verify(productoRepository).findById(999L);
    }

    @Test
    @DisplayName("createProducto - Debe crear producto correctamente")
    void createProducto_DebeCrearProductoCorrectamente() {
        when(productoRepository.existsByNombre("Laptop HP")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        ProductoResponse result = productoService.createProducto(productoRequest);

        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Laptop HP");
        verify(productoRepository).existsByNombre("Laptop HP");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("createProducto - Debe trimear campos de texto")
    void createProducto_DebeTrimmearCamposTexto() {
        productoRequest.setNombre("  Laptop HP  ");
        productoRequest.setCategoria("  Electrónica  ");
        productoRequest.setProveedor("  HP Inc  ");
        
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
    @DisplayName("updateProducto - Debe actualizar producto correctamente")
    void updateProducto_DebeActualizarProductoCorrectamente() {
        productoRequest.setNombre("Laptop Dell");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.existsByNombreAndIdNot("Laptop Dell", 1L)).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        ProductoResponse result = productoService.updateProducto(1L, productoRequest);

        assertThat(result).isNotNull();
        verify(productoRepository).findById(1L);
        verify(productoRepository).existsByNombreAndIdNot("Laptop Dell", 1L);
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("updateProducto - Debe permitir mismo nombre en mismo producto")
    void updateProducto_DebePermitirMismoNombreEnMismoProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        productoService.updateProducto(1L, productoRequest);

        verify(productoRepository, never()).existsByNombreAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("updateProducto - Debe validar nombre duplicado en otro producto")
    void updateProducto_DebeValidarNombreDuplicadoEnOtroProducto() {
        productoRequest.setNombre("Nuevo Nombre");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.existsByNombreAndIdNot("Nuevo Nombre", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productoService.updateProducto(1L, productoRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Ya existe otro producto con el nombre");

        verify(productoRepository).existsByNombreAndIdNot("Nuevo Nombre", 1L);
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProducto - Debe lanzar ResourceNotFoundException si no existe")
    void updateProducto_DebeLanzarExcepcionSiNoExiste() {
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.updateProducto(999L, productoRequest))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(productoRepository).findById(999L);
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProducto - Debe eliminar producto correctamente")
    void deleteProducto_DebeEliminarProductoCorrectamente() {
        when(productoRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productoRepository).deleteById(1L);

        productoService.deleteProducto(1L);

        verify(productoRepository).existsById(1L);
        verify(productoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProducto - Debe lanzar ResourceNotFoundException si no existe")
    void deleteProducto_DebeLanzarExcepcionSiNoExiste() {
        when(productoRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productoService.deleteProducto(999L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(productoRepository).existsById(999L);
        verify(productoRepository, never()).deleteById(anyLong());
    }
}
