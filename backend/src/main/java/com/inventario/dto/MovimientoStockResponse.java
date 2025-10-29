package com.inventario.dto;

import com.inventario.model.MovimientoStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockResponse {
    
    private Long id;
    private Long productoId;
    private String productoNombre;
    private MovimientoStock.TipoMovimiento tipo;
    private Integer cantidad;
    private LocalDateTime fecha;
    private Long usuarioId;
    private Integer stockResultante;
    private Integer stockAnterior;
    private Integer stockNuevo;
    
    public static MovimientoStockResponse fromEntity(MovimientoStock movimiento, String productoNombre, Integer stockAnterior, Integer stockNuevo) {
        return MovimientoStockResponse.builder()
                .id(movimiento.getId())
                .productoId(movimiento.getProductoId())
                .productoNombre(productoNombre)
                .tipo(movimiento.getTipo())
                .cantidad(movimiento.getCantidad())
                .fecha(movimiento.getFecha())
                .usuarioId(movimiento.getUsuarioId())
                .stockResultante(stockNuevo)
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .build();
    }
}
