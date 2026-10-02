package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.dto.LoginRequest;
import com.organizacao_de_recursos.dto.LoginResponse;
import com.organizacao_de_recursos.dto.UsuarioResponse;
import com.organizacao_de_recursos.security.JwtService;
import com.organizacao_de_recursos.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication autenticado = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.senha()));
        UsuarioPrincipal usuario = (UsuarioPrincipal) autenticado.getPrincipal();
        String perfil = usuario.getUsuario().getPerfil().name();
        String token = jwtService.gerarToken(usuario.getUsername(), perfil);
        return ResponseEntity.ok(new LoginResponse(token, usuario.getUsername(), perfil));
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(),
                usuario.getUsuario().getPerfil().name(), usuario.getUsuario().isAtivo());
    }
}
