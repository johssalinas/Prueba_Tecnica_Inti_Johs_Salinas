package com.inventario.dto;

import com.inventario.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {
    
    private Long id;
    private String nombre;
    private String categoria;
    private String proveedor;
    private BigDecimal precio;
    private Integer stock;
    private LocalDateTime fechaRegistro;
    
    public static ProductoResponse fromEntity(Producto producto) {
        Objects.requireNonNull(producto, "Producto no puede ser null");
        return new ProductoResponse(
            producto.getId(),
            producto.getNombre(),
            producto.getCategoria(),
            producto.getProveedor(),
            producto.getPrecio(),
            producto.getStock(),
            producto.getFechaRegistro()
        );
    }
}
