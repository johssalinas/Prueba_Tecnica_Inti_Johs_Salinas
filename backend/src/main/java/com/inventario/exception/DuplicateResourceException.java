package com.inventario.exception;

/**
 * Excepción lanzada cuando se intenta crear un recurso duplicado
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
}
