package com.inventario.service;

import com.inventario.dto.MovimientoStockRequest;
import com.inventario.dto.MovimientoStockResponse;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.model.MovimientoStock;
import com.inventario.model.Producto;
import com.inventario.repository.MovimientoStockRepository;
import com.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class MovimientoStockService {
    
    private final MovimientoStockRepository movimientoStockRepository;
    private final ProductoRepository productoRepository;
    
    private static final int MAX_STOCK_VALUE = Integer.MAX_VALUE - 1000000;
    
    @Transactional
    public MovimientoStockResponse registrarMovimiento(MovimientoStockRequest request) {
        validateRequest(request);
        
        if (log.isInfoEnabled()) {
            log.info("Registrando movimiento de stock - Producto ID: {}, Tipo: {}, Cantidad: {}", 
                     sanitizeForLog(request.getProductoId()), 
                     sanitizeForLog(request.getTipo()), 
                     sanitizeForLog(request.getCantidad()));
        }
        
        Producto producto = productoRepository.findById(request.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", request.getProductoId()));
        
        int stockActual = producto.getStock();
        int cantidad = request.getCantidad();
        int nuevoStock = calcularNuevoStock(stockActual, cantidad, request.getTipo());
        
        validarOverflow(nuevoStock);
        
        MovimientoStock movimiento = crearMovimiento(producto.getId(), request.getTipo(), cantidad);
        MovimientoStock savedMovimiento = movimientoStockRepository.save(movimiento);
        
        producto.setStock(nuevoStock);
        productoRepository.save(producto);
        
        if (log.isInfoEnabled()) {
            log.info("Movimiento registrado exitosamente - Producto ID: {}, Stock anterior: {}, Stock nuevo: {}", 
                     sanitizeForLog(producto.getId()), stockActual, nuevoStock);
        }
        
        return MovimientoStockResponse.fromEntity(savedMovimiento, producto.getNombre(), stockActual, nuevoStock);
    }
    
    private void validateRequest(MovimientoStockRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request no puede ser null");
        }
        if (request.getProductoId() == null) {
            throw new IllegalArgumentException("ProductoId no puede ser null");
        }
        if (request.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de movimiento no puede ser null");
        }
        if (request.getCantidad() == null) {
            throw new IllegalArgumentException("Cantidad no puede ser null");
        }
        
        if (request.getProductoId() <= 0) {
            throw new IllegalArgumentException("ProductoId debe ser positivo");
        }
        
        if (request.getCantidad() <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser positiva");
        }
        
        if (request.getCantidad() > 1000000) {
            throw new IllegalArgumentException("Cantidad excede el límite permitido de 1,000,000");
        }
    }
    
    private int calcularNuevoStock(int stockActual, int cantidad, MovimientoStock.TipoMovimiento tipo) {
        int nuevoStock;
        
        if (tipo == MovimientoStock.TipoMovimiento.ENTRADA) {
            long resultado = (long) stockActual + cantidad;
            if (resultado > MAX_STOCK_VALUE) {
                throw new IllegalArgumentException(
                    String.format("La operación causaría overflow. Stock actual: %d, Cantidad a agregar: %d", 
                                  stockActual, cantidad)
                );
            }
            nuevoStock = stockActual + cantidad;
        } else if (tipo == MovimientoStock.TipoMovimiento.SALIDA) {
            if (stockActual < cantidad) {
                throw new IllegalArgumentException(
                    String.format("Stock insuficiente. Disponible: %d, Solicitado: %d", stockActual, cantidad)
                );
            }
            nuevoStock = stockActual - cantidad;
        } else {
            throw new IllegalArgumentException("Tipo de movimiento no válido: " + tipo);
        }
        
        return nuevoStock;
    }
    
    private void validarOverflow(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
    }
    
    private MovimientoStock crearMovimiento(Long productoId, MovimientoStock.TipoMovimiento tipo, int cantidad) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProductoId(productoId);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        return movimiento;
    }
    
    private Object sanitizeForLog(Object value) {
        if (value == null) {
            return "null";
        }
        String str = value.toString();
        return str.replaceAll("[\n\r\t]", "_");
    }
}
