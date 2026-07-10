package com.orcalab.auth.service;

import com.orcalab.auth.config.JwtUtil;
import com.orcalab.auth.dto.AuthResponse;
import com.orcalab.auth.dto.LoginRequest;
import com.orcalab.auth.dto.RegistroRequest;
import com.orcalab.auth.dto.UsuarioResponse;
import com.orcalab.auth.dto.UsuarioResumenResponse;
import com.orcalab.auth.model.Rol;
import com.orcalab.auth.model.Usuario;
import com.orcalab.auth.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // El registro público siempre crea INVESTIGADOR, sin importar lo que mande el cliente.
        Usuario usuario = new Usuario(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNombre(),
                Rol.INVESTIGADOR
        );

        usuario = usuarioRepository.save(usuario);

        String token = jwtUtil.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol().name());

        return new AuthResponse(token, usuario.getId(), usuario.getNombre(), usuario.getRol().name());
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        String token = jwtUtil.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol().name());

        return new AuthResponse(token, usuario.getId(), usuario.getNombre(), usuario.getRol().name());
    }

    public List<UsuarioResponse> listarUsuarios() {
        verificarEsAdmin();
        return usuarioRepository.findAll().stream().map(UsuarioResponse::new).toList();
    }

    public void cambiarRol(Long usuarioId, Rol nuevoRol) {
        verificarEsAdmin();

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
    }

    // Accesible para cualquier usuario autenticado: solo expone id + nombre.
    public List<UsuarioResumenResponse> obtenerResumen(List<Long> ids) {
        return usuarioRepository.findAllById(ids).stream().map(UsuarioResumenResponse::new).toList();
    }

    private void verificarEsAdmin() {
        boolean esAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + Rol.ADMINISTRADOR.name()));

        if (!esAdmin) {
            throw new AccessDeniedException("Solo un administrador puede realizar esta acción");
        }
    }
}