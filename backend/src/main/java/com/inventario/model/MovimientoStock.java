package com.inventario.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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
