package com.inventario.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Producto {
    private Long id;
    private String nombre;
    private String categoria;
    private String proveedor;
    private BigDecimal precio;
    private Integer stock;
    private LocalDateTime fechaRegistro;
}
