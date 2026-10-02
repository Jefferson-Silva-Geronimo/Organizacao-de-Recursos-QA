package com.organizacao_de_recursos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Lê o Bearer token de /api/**, valida e popula o SecurityContext (ADR-018). */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO_BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecalho.substring(PREFIXO_BEARER.length());
            Optional<String> username = jwtService.validarEExtrairUsername(token);
            if (username.isPresent()) {
                UserDetails usuario = usuarioDetailsService.loadUserByUsername(username.get());
                if (usuario.isEnabled()) {
                    var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
