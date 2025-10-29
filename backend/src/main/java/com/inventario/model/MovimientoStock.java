package com.inventario.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;
    
    @Column(nullable = false)
    private Integer cantidad;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
    
    @Column(name = "usuario_id")
    private Long usuarioId;

    public enum TipoMovimiento {
        ENTRADA, SALIDA
    }
}
