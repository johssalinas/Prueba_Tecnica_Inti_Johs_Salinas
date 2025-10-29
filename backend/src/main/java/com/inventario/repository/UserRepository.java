package com.inventario.repository;

import com.inventario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<Usuario> findByUsername(String username) {
        String sql = "SELECT id, username, password, email, rol, activo, fecha_creacion " +
                "FROM usuarios WHERE username = ? AND activo = true";

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getLong("id"));
                usuario.setUsername(rs.getString("username"));
                usuario.setPassword(rs.getString("password"));
                usuario.setEmail(rs.getString("email"));
                usuario.setRol(rs.getString("rol"));
                usuario.setActivo(rs.getBoolean("activo"));
                usuario.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                return Optional.of(usuario);
            }
            return Optional.empty();
        });
    }
}
