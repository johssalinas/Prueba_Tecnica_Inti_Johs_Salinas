package com.inventario.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Usuario {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String rol;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
