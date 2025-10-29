package com.inventario.controller;

import com.inventario.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para sincronización de productos desde APIs externas
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final ProductoService productoService;

    /**
     * Sincroniza productos desde FakeStore API
     * Retorna diferentes códigos HTTP según el resultado
     * @return ResponseEntity con el resultado de la sincronización
     */
    @PostMapping("/sync-products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SyncResponse> syncProducts() {
        log.info("POST /sync-products - iniciando sincronización desde FakeStore API");

        try {
            int total = productoService.syncProductsFromFakeStore();

            if (total == 0) {
                log.info("Sincronización completada sin productos nuevos");
                return ResponseEntity.ok(
                    new SyncResponse(0, "Sincronización completada. No hay productos nuevos para insertar")
                );
            }

            log.info("Sincronización exitosa: {} productos insertados", total);
            SyncResponse response = new SyncResponse(
                total, 
                "Sincronización completada exitosamente. Total insertados: " + total
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error durante la sincronización de productos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SyncResponse(0, "Error interno durante la sincronización: " + e.getMessage()));
        }
    }

    /**
     * DTO para respuesta de sincronización
     * @param totalSincronizados Número de productos insertados
     * @param mensaje Mensaje descriptivo del resultado
     */
    public record SyncResponse(int totalSincronizados, String mensaje) {}
}
