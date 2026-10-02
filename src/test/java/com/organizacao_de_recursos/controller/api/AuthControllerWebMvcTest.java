package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.config.SecurityConfig;
import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.security.JwtService;
import com.organizacao_de_recursos.security.UsuarioDetailsService;
import com.organizacao_de_recursos.security.UsuarioPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("Login com credenciais válidas devolve o token")
    void login_credenciaisValidas_200() throws Exception {
        UsuarioEntity usuario = new UsuarioEntity("solicitante1", "hash", Usuario.Perfil.SOLICITANTE);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtService.gerarToken("solicitante1", "SOLICITANTE")).thenReturn("token-fake");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"solicitante1\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-fake"))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"));
    }

    @Test
    @DisplayName("Login com credenciais inválidas devolve 401 sem detalhe interno")
    void login_credenciaisInvalidas_401() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"solicitante1\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuário ou senha inválidos"));
    }

    @Test
    @DisplayName("Login sem username deve retornar 400 (Bean Validation)")
    void login_semUsername_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("/me sem token deve retornar 401")
    void me_semToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me autenticado devolve os dados do usuário")
    void me_autenticado_200() throws Exception {
        UsuarioEntity usuario = new UsuarioEntity("admin1", "hash", Usuario.Perfil.ADMINISTRADOR);
        ReflectionTestUtils.setField(usuario, "id", 9L);
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin1"))
                .andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"));
    }
}
