package com.inventario.controller;

import com.inventario.dto.PageResponse;
import com.inventario.dto.ProductoRequest;
import com.inventario.dto.ProductoResponse;
import com.inventario.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Slf4j
public class ProductoController {
    
    private final ProductoService productoService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ProductoResponse>> getAllProductos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaRegistro") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET /api/productos - search: {}, categoria: {}, page: {}, size: {}", 
                 search, categoria, page, size);
        
        PageResponse<ProductoResponse> response = productoService.getAllProductos(
            search, categoria, page, size, sortBy, sortDir
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Long id) {
        log.info("GET /api/productos/{}", id);
        ProductoResponse response = productoService.getProductoById(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductoResponse> createProducto(@Valid @RequestBody ProductoRequest request) {
        log.info("POST /api/productos");
        ProductoResponse response = productoService.createProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductoResponse> updateProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request
    ) {
        log.info("PUT /api/productos/{}", id);
        ProductoResponse response = productoService.updateProducto(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
        log.info("DELETE /api/productos/{}", id);
        productoService.deleteProducto(id);
        return ResponseEntity.noContent().build();
    }
}
