package com.inventario.config;

import com.inventario.model.Usuario;
import com.inventario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos por defecto en la base de datos.
 * Se ejecuta automáticamente al iniciar la aplicación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Iniciando verificación de datos iniciales...");
        
        createDefaultAdminUser();
        
        log.info("Verificación de datos iniciales completada");
    }

    private void createDefaultAdminUser() {
        String adminUsername = "admin";
        
        if (userRepository.findByUsernameAndActivoTrue(adminUsername).isPresent()) {
            log.info("Usuario administrador '{}' ya existe en la base de datos", adminUsername);
            return;
        }
        
        log.info("Creando usuario administrador por defecto...");
        
        Usuario admin = new Usuario();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@inventario.com");
        admin.setRol("ADMIN");
        admin.setActivo(true);
        
        userRepository.save(admin);
        
        log.info(" Usuario administrador creado exitosamente");
        log.info("   Username: admin");
        log.info("   Password: admin123");
    }
}
