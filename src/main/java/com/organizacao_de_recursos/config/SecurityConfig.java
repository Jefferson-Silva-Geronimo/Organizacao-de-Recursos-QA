package com.organizacao_de_recursos.config;

import com.organizacao_de_recursos.security.JwtAuthenticationFilter;
import com.organizacao_de_recursos.security.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Duas SecurityFilterChain ordenadas por path (ADR-018): /api/** stateless + JWT; o restante
 * (páginas Thymeleaf da Onda 4) cai na chain padrão, por ora apenas liberando o essencial.
 * Três camadas de autorização: rota (aqui), método (@PreAuthorize nos controllers) e objeto
 * ({@link com.organizacao_de_recursos.security.AutorizacaoReserva}).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UsuarioDetailsService usuarioDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        AuthenticationEntryPoint entradaNaoAutenticado = (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado");

        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entradaNaoAutenticado))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}
