package com.inventario.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MovimientoStock {
    private Long id;
    private Long productoId;
    private TipoMovimiento tipo;
    private Integer cantidad;
    private LocalDateTime fecha;
    private Long usuarioId;

    public enum TipoMovimiento {
        ENTRADA, SALIDA
    }
}
