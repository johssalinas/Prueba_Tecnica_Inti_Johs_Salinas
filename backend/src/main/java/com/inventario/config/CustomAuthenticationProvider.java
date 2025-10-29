package com.inventario.config;

import com.inventario.model.Usuario;
import com.inventario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.debug("Attempting authentication for user: {}", username);

        Usuario usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: user '{}' not found", username);
                    return new BadCredentialsException("Credenciales inválidas");
                });

        if (!usuario.getActivo()) {
            log.warn("Authentication failed: user '{}' is disabled", username);
            throw new DisabledException("Cuenta deshabilitada");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            log.warn("Authentication failed: invalid password for user '{}'", username);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        log.info("User '{}' authenticated successfully with role: {}", username, usuario.getRol());

        return new UsernamePasswordAuthenticationToken(
                username,
                password,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
