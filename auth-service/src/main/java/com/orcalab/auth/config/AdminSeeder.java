package com.orcalab.auth.config;

import com.orcalab.auth.model.Rol;
import com.orcalab.auth.model.Usuario;
import com.orcalab.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-seed.email}")
    private String adminEmail;

    @Value("${app.admin-seed.password}")
    private String adminPassword;

    @Value("${app.admin-seed.nombre}")
    private String adminNombre;

    public AdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByRol(Rol.ADMINISTRADOR)) {
            return;
        }

        if (usuarioRepository.existsByEmail(adminEmail)) {
            log.warn("No hay ningún ADMINISTRADOR pero el email de seed {} ya está en uso por otro usuario; " +
                    "no se pudo crear el admin automáticamente. Asciende un usuario manualmente vía PATCH " +
                    "/api/auth/usuarios/{{id}}/rol tras conceder el rol por otro medio.", adminEmail);
            return;
        }

        Usuario admin = new Usuario(adminEmail, passwordEncoder.encode(adminPassword), adminNombre, Rol.ADMINISTRADOR);
        usuarioRepository.save(admin);

        log.info("Administrador inicial creado: {}", adminEmail);
    }
}
