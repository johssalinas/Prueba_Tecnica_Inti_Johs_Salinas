package com.inventario.controller;

import com.inventario.dto.MovimientoStockRequest;
import com.inventario.dto.MovimientoStockResponse;
import com.inventario.service.MovimientoStockService;
// Removed OpenAPI/Swagger annotations to avoid compile errors when dependency is not present
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/stock-movements")
@RequiredArgsConstructor
@Slf4j
public class StockMovementController {
    
    private final MovimientoStockService movimientoStockService;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoStockResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoStockRequest request) {
        
        if (log.isDebugEnabled()) {
            log.debug("Recibida solicitud de movimiento de stock para producto ID: {}", request.getProductoId());
        }
        
        MovimientoStockResponse response = movimientoStockService.registrarMovimiento(request);
        
        // Crear Location header
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
}
