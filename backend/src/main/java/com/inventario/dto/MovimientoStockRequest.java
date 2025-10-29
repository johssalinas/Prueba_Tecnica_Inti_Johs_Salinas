package com.inventario.dto;

import com.inventario.model.MovimientoStock;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MovimientoStockRequest {
    
    @NotNull(message = "El ID del producto es obligatorio")
    @Positive(message = "El ID del producto debe ser positivo")
    private Long productoId;
    
    @NotNull(message = "El tipo de movimiento es obligatorio")
    private MovimientoStock.TipoMovimiento tipo;
    
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    @Max(value = 1000000, message = "La cantidad no puede exceder 1,000,000")
    private Integer cantidad;
}
