package com.inventario.service;

import com.inventario.client.FakeStoreClient;
import com.inventario.dto.FakeStoreProductDto;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService - Lógica de Negocio Crítica")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private FakeStoreClient fakeStoreClient;

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

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe insertar productos nuevos exitosamente")
    void syncProductsFromFakeStore_DebeInsertarProductosNuevos() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto1 = new FakeStoreProductDto();
        dto1.setId(1L);
        dto1.setTitle("Fjallraven Backpack");
        dto1.setPrice(109.95);
        dto1.setCategory("men's clothing");
        dto1.setDescription("Your perfect pack");
        dto1.setImage("https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg");
        fakeStoreProducts.add(dto1);

        FakeStoreProductDto dto2 = new FakeStoreProductDto();
        dto2.setId(2L);
        dto2.setTitle("Mens Casual T-Shirt");
        dto2.setPrice(22.3);
        dto2.setCategory("men's clothing");
        fakeStoreProducts.add(dto2);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int total = productoService.syncProductsFromFakeStore();

        // Assert
        assertThat(total).isEqualTo(2);
        verify(fakeStoreClient).getAllProducts();
        verify(productoRepository).findByNombreIn(anyCollection());
        verify(productoRepository).saveAll(argThat(list -> ((List<Producto>) list).size() == 2));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe aplicar sanitización XSS en campos de texto")
    void syncProductsFromFakeStore_DebeAplicarSanitizacionXSS() {
        // Arrange - Producto con código malicioso
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto = new FakeStoreProductDto();
        dto.setId(1L);
        dto.setTitle("<script>alert('XSS')</script>Producto Malicioso");
        dto.setPrice(99.99);
        dto.setCategory("<img src=x onerror=alert(1)>");
        fakeStoreProducts.add(dto);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productoService.syncProductsFromFakeStore();

        // Assert - Verifica que se sanitizó el HTML
        verify(productoRepository).saveAll(argThat(list -> {
            Producto p = ((List<Producto>) list).get(0);
            return p.getNombre().contains("&lt;script&gt;") && 
                   p.getCategoria().contains("&lt;img");
        }));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe omitir productos sin título (validación nulos)")
    void syncProductsFromFakeStore_DebeOmitirProductosSinTitulo() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        
        FakeStoreProductDto dtoValido = new FakeStoreProductDto();
        dtoValido.setId(1L);
        dtoValido.setTitle("Producto Válido");
        dtoValido.setPrice(50.0);
        dtoValido.setCategory("electronics");
        fakeStoreProducts.add(dtoValido);

        FakeStoreProductDto dtoSinTitulo = new FakeStoreProductDto();
        dtoSinTitulo.setId(2L);
        dtoSinTitulo.setTitle(null); //  INVÁLIDO
        dtoSinTitulo.setPrice(30.0);
        fakeStoreProducts.add(dtoSinTitulo);

        FakeStoreProductDto dtoTituloVacio = new FakeStoreProductDto();
        dtoTituloVacio.setId(3L);
        dtoTituloVacio.setTitle("   "); //  INVÁLIDO
        dtoTituloVacio.setPrice(20.0);
        fakeStoreProducts.add(dtoTituloVacio);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int total = productoService.syncProductsFromFakeStore();

        // Assert - Solo debe insertar 1 producto válido
        assertThat(total).isEqualTo(1);
        verify(productoRepository).saveAll(argThat(list -> ((List<Producto>) list).size() == 1));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe manejar precio nulo con BigDecimal.ZERO")
    void syncProductsFromFakeStore_DebeManejarPrecioNulo() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto = new FakeStoreProductDto();
        dto.setId(1L);
        dto.setTitle("Producto Sin Precio");
        dto.setPrice(null); //  Precio nulo
        dto.setCategory("test");
        fakeStoreProducts.add(dto);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productoService.syncProductsFromFakeStore();

        // Assert - Debe asignar BigDecimal.ZERO
        verify(productoRepository).saveAll(argThat(list -> {
            Producto p = ((List<Producto>) list).get(0);
            return p.getPrecio().compareTo(BigDecimal.ZERO) == 0;
        }));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe evitar duplicados usando findByNombreIn (Optimización N+1)")
    void syncProductsFromFakeStore_DebeEvitarDuplicadosConOptimizacion() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto1 = new FakeStoreProductDto();
        dto1.setTitle("Producto Nuevo");
        dto1.setPrice(50.0);
        dto1.setCategory("test");
        fakeStoreProducts.add(dto1);

        FakeStoreProductDto dto2 = new FakeStoreProductDto();
        dto2.setTitle("Producto Existente");
        dto2.setPrice(30.0);
        dto2.setCategory("test");
        fakeStoreProducts.add(dto2);

        // Simular que "Producto Existente" ya está en DB
        Producto productoExistente = new Producto();
        productoExistente.setNombre("Producto Existente");
        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Arrays.asList(productoExistente));
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int total = productoService.syncProductsFromFakeStore();

        // Assert - Solo debe insertar 1 producto (el nuevo)
        assertThat(total).isEqualTo(1);
        verify(productoRepository).findByNombreIn(anyCollection());
        verify(productoRepository, never()).existsByNombre(anyString());
        verify(productoRepository).saveAll(argThat(list -> ((List<Producto>) list).size() == 1));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe retornar 0 cuando API externa retorna lista vacía")
    void syncProductsFromFakeStore_DebeRetornar0ConListaVacia() {
        // Arrange
        when(fakeStoreClient.getAllProducts()).thenReturn(Collections.emptyList());

        // Act
        int total = productoService.syncProductsFromFakeStore();

        // Assert
        assertThat(total).isZero();
        verify(fakeStoreClient).getAllProducts();
        verify(productoRepository, never()).findByNombreIn(anyCollection());
        verify(productoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe asignar proveedor 'FakeStore API' a todos los productos")
    void syncProductsFromFakeStore_DebeAsignarProveedorFakeStore() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto = new FakeStoreProductDto();
        dto.setTitle("Test Product");
        dto.setPrice(25.0);
        dto.setCategory("test");
        fakeStoreProducts.add(dto);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productoService.syncProductsFromFakeStore();

        // Assert
        verify(productoRepository).saveAll(argThat(list -> {
            Producto p = ((List<Producto>) list).get(0);
            return "FakeStore API".equals(p.getProveedor());
        }));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe asignar stock inicial de 0 a productos sincronizados")
    void syncProductsFromFakeStore_DebeAsignarStockCero() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto = new FakeStoreProductDto();
        dto.setTitle("Test Product");
        dto.setPrice(25.0);
        dto.setCategory("test");
        fakeStoreProducts.add(dto);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productoService.syncProductsFromFakeStore();

        // Assert
        verify(productoRepository).saveAll(argThat(list -> {
            Producto p = ((List<Producto>) list).get(0);
            return p.getStock() == 0;
        }));
    }

    @Test
    @DisplayName("syncProductsFromFakeStore - Debe asignar categoría 'Sin categoría' cuando es null")
    void syncProductsFromFakeStore_DebeAsignarCategoriaPorDefecto() {
        // Arrange
        List<FakeStoreProductDto> fakeStoreProducts = new ArrayList<>();
        FakeStoreProductDto dto = new FakeStoreProductDto();
        dto.setTitle("Producto Sin Categoría");
        dto.setPrice(50.0);
        dto.setCategory(null); //  Categoría nula
        fakeStoreProducts.add(dto);

        when(fakeStoreClient.getAllProducts()).thenReturn(fakeStoreProducts);
        when(productoRepository.findByNombreIn(anyCollection())).thenReturn(Collections.emptyList());
        when(productoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productoService.syncProductsFromFakeStore();

        // Assert
        verify(productoRepository).saveAll(argThat(list -> {
            Producto p = ((List<Producto>) list).get(0);
            return "Sin categoría".equals(p.getCategoria());
        }));
    }
}
