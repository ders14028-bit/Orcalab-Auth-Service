package com.orcalab.auth.controller;

import com.orcalab.auth.dto.AuthResponse;
import com.orcalab.auth.dto.CambiarRolRequest;
import com.orcalab.auth.dto.LoginRequest;
import com.orcalab.auth.dto.RegistroRequest;
import com.orcalab.auth.dto.UsuarioResponse;
import com.orcalab.auth.dto.UsuarioResumenResponse;
import com.orcalab.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<Void> cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequest request) {
        authService.cambiarRol(id, request.getNuevoRol());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios/resumen")
    public ResponseEntity<List<UsuarioResumenResponse>> obtenerResumen(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(authService.obtenerResumen(ids));
    }
}