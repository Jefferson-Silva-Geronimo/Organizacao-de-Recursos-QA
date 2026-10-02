package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.config.SecurityConfig;
import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.SalaRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalaController.class)
@Import(SecurityConfig.class)
class SalaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalaRepository salaRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    private Authentication autenticacaoDe(Usuario.Perfil perfil) {
        UsuarioEntity usuario = new UsuarioEntity("user", "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("Rota: listar salas sem autenticação deve retornar 401")
    void listar_semAutenticacao_401() throws Exception {
        mockMvc.perform(get("/api/v1/salas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Listar salas: qualquer perfil autenticado pode consultar")
    void listar_autenticado_200() throws Exception {
        when(salaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/salas").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Método: Solicitante não pode cadastrar sala (apenas Administrador)")
    void criar_comoSolicitante_403() throws Exception {
        mockMvc.perform(post("/api/v1/salas")
                        .with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sala Nova\",\"restrito\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Happy path: Administrador cadastra sala (201)")
    void criar_comoAdministrador_201() throws Exception {
        when(salaRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(new SalaEntity("Sala Nova", false, null));

        mockMvc.perform(post("/api/v1/salas")
                        .with(authentication(autenticacaoDe(Usuario.Perfil.ADMINISTRADOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sala Nova\",\"restrito\":false}"))
                .andExpect(status().isCreated());
    }
}
