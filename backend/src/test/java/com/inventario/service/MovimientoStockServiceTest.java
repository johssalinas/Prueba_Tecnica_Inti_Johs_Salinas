package com.inventario.service;

import com.inventario.dto.MovimientoStockRequest;
import com.inventario.dto.MovimientoStockResponse;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.model.MovimientoStock;
import com.inventario.model.Producto;
import com.inventario.repository.MovimientoStockRepository;
import com.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para MovimientoStockService
 * Cubre casos de éxito, validaciones, concurrencia y edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovimientoStockService - Tests de Lógica de Negocio")
class MovimientoStockServiceTest {

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private MovimientoStockService movimientoStockService;

    private Producto producto;
    private MovimientoStockRequest request;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Laptop Dell");
        producto.setCategoria("Electrónica");
        producto.setProveedor("Dell Inc");
        producto.setPrecio(new BigDecimal("1500.00"));
        producto.setStock(100);
        producto.setVersion(0);

        request = new MovimientoStockRequest();
        request.setProductoId(1L);
        request.setCantidad(10);
    }

    @Nested
    @DisplayName("Movimientos de ENTRADA")
    class MovimientosEntrada {

        @BeforeEach
        void setUp() {
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
        }

        @Test
        @DisplayName("Debe registrar entrada y aumentar stock correctamente")
        void debeRegistrarEntradaCorrectamente() {
            // Arrange
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(1L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProductoId()).isEqualTo(1L);
            assertThat(response.getTipo()).isEqualTo(MovimientoStock.TipoMovimiento.ENTRADA);
            assertThat(response.getCantidad()).isEqualTo(10);
            assertThat(response.getStockResultante()).isEqualTo(110); // 100 + 10

            // Verify
            verify(productoRepository).findById(1L);
            verify(movimientoStockRepository).save(any(MovimientoStock.class));
            
            ArgumentCaptor<Producto> productoCaptor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).save(productoCaptor.capture());
            assertThat(productoCaptor.getValue().getStock()).isEqualTo(110);
        }

        @Test
        @DisplayName("Debe manejar entrada con cantidad máxima válida")
        void debeManejaCantidadMaxima() {
            // Arrange
            request.setCantidad(999999); // Justo debajo del límite de 1,000,000
            producto.setStock(100);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(1L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStockResultante()).isEqualTo(100 + 999999);
        }

        @Test
        @DisplayName("Debe fallar si el producto no existe")
        void debeFallarSiProductoNoExiste() {
            // Arrange
            when(productoRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Producto no encontrado");

            verify(productoRepository).findById(1L);
            verify(movimientoStockRepository, never()).save(any());
            verify(productoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe detectar overflow de stock")
        void debeDetectarOverflow() {
            // Arrange
            producto.setStock(Integer.MAX_VALUE);
            request.setCantidad(1);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("overflow");

            verify(movimientoStockRepository, never()).save(any());
            verify(productoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Movimientos de SALIDA")
    class MovimientosSalida {

        @BeforeEach
        void setUp() {
            request.setTipo(MovimientoStock.TipoMovimiento.SALIDA);
        }

        @Test
        @DisplayName("Debe registrar salida y disminuir stock correctamente")
        void debeRegistrarSalidaCorrectamente() {
            // Arrange
            request.setCantidad(30);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(2L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getTipo()).isEqualTo(MovimientoStock.TipoMovimiento.SALIDA);
            assertThat(response.getCantidad()).isEqualTo(30);
            assertThat(response.getStockResultante()).isEqualTo(70); // 100 - 30

            ArgumentCaptor<Producto> productoCaptor = ArgumentCaptor.forClass(Producto.class);
            verify(productoRepository).save(productoCaptor.capture());
            assertThat(productoCaptor.getValue().getStock()).isEqualTo(70);
        }

        @Test
        @DisplayName("Debe permitir salida que deje stock en cero")
        void debePermitirSalidaACero() {
            // Arrange
            request.setCantidad(100);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(3L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);

            // Assert
            assertThat(response.getStockResultante()).isZero();
        }

        @Test
        @DisplayName("Debe fallar si stock insuficiente")
        void debeFallarStockInsuficiente() {
            // Arrange
            request.setCantidad(150); // Mayor que stock disponible (100)
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stock insuficiente");

            verify(movimientoStockRepository, never()).save(any());
            verify(productoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe fallar con producto sin stock")
        void debeFallarProductoSinStock() {
            // Arrange
            producto.setStock(0);
            request.setCantidad(1);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stock insuficiente");
        }

        @Test
        @DisplayName("Debe detectar underflow de stock")
        void debeDetectarUnderflow() {
            // Arrange
            producto.setStock(10);
            request.setCantidad(500); // Cantidad válida pero mayor que el stock
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Stock insuficiente");
        }
    }

    @Nested
    @DisplayName("Validaciones de Request")
    class ValidacionesRequest {

        @Test
        @DisplayName("Debe fallar con productoId nulo")
        void debeFallarProductoIdNulo() {
            // Arrange
            request.setProductoId(null);
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Debe fallar con cantidad negativa")
        void debeFallarCantidadNegativa() {
            // Arrange
            request.setCantidad(-1);
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Debe fallar con cantidad cero")
        void debeFallarCantidadCero() {
            // Arrange
            request.setCantidad(0);
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Debe fallar con tipo nulo")
        void debeFallarTipoNulo() {
            // Arrange
            request.setTipo(null);

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Manejo de Concurrencia")
    class ManejoConcurrencia {

        @Test
        @DisplayName("Debe propagar OptimisticLockingFailureException")
        void debePropagaOptimisticLockException() {
            // Arrange
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(1L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Producto.class, 1L));

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            // El movimiento se guardó pero el producto falló (rollback esperado por @Transactional)
            verify(movimientoStockRepository).save(any(MovimientoStock.class));
            verify(productoRepository).save(any(Producto.class));
        }
    }

    @Nested
    @DisplayName("Atomicidad de Transacciones")
    class AtomicidadTransacciones {

        @Test
        @DisplayName("Debe guardar movimiento y actualizar producto en orden correcto")
        void debeGuardarEnOrdenCorrecto() {
            // Arrange
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(1L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            movimientoStockService.registrarMovimiento(request);

            // Assert - Verificar orden de invocaciones
            var inOrder = inOrder(productoRepository, movimientoStockRepository);
            inOrder.verify(productoRepository).findById(1L);
            inOrder.verify(movimientoStockRepository).save(any(MovimientoStock.class));
            inOrder.verify(productoRepository).save(any(Producto.class));
        }

        @Test
        @DisplayName("No debe actualizar producto si falla guardar movimiento")
        void noDebeActualizarProductoSiFallaMovimiento() {
            // Arrange
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenThrow(new RuntimeException("DB Error"));

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(RuntimeException.class);

            verify(movimientoStockRepository).save(any(MovimientoStock.class));
            verify(productoRepository, never()).save(any(Producto.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Debe manejar producto con stock inicial cero y entrada")
        void debeManejarStockCeroConEntrada() {
            // Arrange
            producto.setStock(0);
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
            request.setCantidad(50);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(1L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);

            // Assert
            assertThat(response.getStockResultante()).isEqualTo(50);
        }

        @Test
        @DisplayName("Debe manejar múltiples dígitos en cantidad")
        void debeManejarCantidadesGrandes() {
            // Arrange
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
            request.setCantidad(999999);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(movimientoStockRepository.save(any(MovimientoStock.class)))
                    .thenAnswer(invocation -> {
                        MovimientoStock mov = invocation.getArgument(0);
                        mov.setId(1L);
                        return mov;
                    });
            when(productoRepository.save(any(Producto.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);

            // Assert
            assertThat(response.getStockResultante()).isEqualTo(1000099); // 100 + 999999
        }

        @Test
        @DisplayName("Debe manejar productoId muy grande")
        void debeManejarProductoIdGrande() {
            // Arrange
            request.setProductoId(Long.MAX_VALUE);
            request.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
            when(productoRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> movimientoStockService.registrarMovimiento(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
