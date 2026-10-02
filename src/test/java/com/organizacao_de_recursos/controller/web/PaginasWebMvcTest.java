package com.organizacao_de_recursos.controller.web;

import com.organizacao_de_recursos.config.SecurityConfig;
import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.security.AutorizacaoReserva;
import com.organizacao_de_recursos.security.JwtService;
import com.organizacao_de_recursos.security.UsuarioDetailsService;
import com.organizacao_de_recursos.security.UsuarioPrincipal;
import com.organizacao_de_recursos.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderiza de fato as páginas Thymeleaf (via o motor real do Spring, não mockado) para pegar
 * erros de template (sintaxe th:/sec:, campos inexistentes) que uma verificação manual não cobriria
 * de forma confiável neste ambiente. Cobre também a segurança de rota/método das páginas web.
 */
@WebMvcTest(controllers = {PaginaController.class, ReservaWebController.class, SalaWebController.class})
@Import(SecurityConfig.class)
class PaginasWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;
    @MockBean(name = "autorizacaoReserva")
    private AutorizacaoReserva autorizacaoReserva;
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
    @DisplayName("/login renderiza sem autenticação")
    void login_renderiza() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entrar")));
    }

    @Test
    @DisplayName("/painel sem autenticação redireciona para /login")
    void painel_semAutenticacao_redireciona() throws Exception {
        mockMvc.perform(get("/painel")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("/painel autenticado renderiza com o nav (sec:authorize) sem lançar exceção")
    void painel_autenticado_renderiza() throws Exception {
        mockMvc.perform(get("/painel").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SOLICITANTE")));
    }

    @Test
    @DisplayName("/reservas/nova renderiza o formulário com a lista de salas")
    void reservasNova_renderiza() throws Exception {
        when(salaRepository.findAll()).thenReturn(List.of(new SalaEntity("Sala A", false, null)));

        mockMvc.perform(get("/reservas/nova").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sala A")));
    }

    @Test
    @DisplayName("/reservas/minhas renderiza a tabela de reservas do solicitante")
    void reservasMinhas_renderiza() throws Exception {
        ReservaEntity reserva = new ReservaEntity(1L, com.organizacao_de_recursos.domain.estado.EstadoReserva.SOLICITADA,
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1));
        ReflectionTestUtils.setField(reserva, "id", 7L);
        when(reservaService.listarDoSolicitante(1L)).thenReturn(List.of(reserva));

        mockMvc.perform(get("/reservas/minhas").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SOLICITADA")));
    }

    @Test
    @DisplayName("/reservas/minhas vazio renderiza mensagem de 'nenhuma reserva'")
    void reservasMinhas_vazia_renderiza() throws Exception {
        when(reservaService.listarDoSolicitante(1L)).thenReturn(List.of());

        mockMvc.perform(get("/reservas/minhas").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma reserva")));
    }

    @Test
    @DisplayName("/reservas/aprovacao renderiza a fila do Responsável")
    void reservasAprovacao_renderiza() throws Exception {
        when(reservaService.listarNoEscopoDoResponsavel(1L)).thenReturn(List.of());

        mockMvc.perform(get("/reservas/aprovacao").with(authentication(autenticacaoDe(Usuario.Perfil.RESPONSAVEL))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Solicitante não pode acessar a fila de aprovação (403)")
    void reservasAprovacao_comoSolicitante_403() throws Exception {
        mockMvc.perform(get("/reservas/aprovacao").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/salas renderiza a lista e o formulário de cadastro (Administrador)")
    void salas_renderiza() throws Exception {
        when(salaRepository.findAll()).thenReturn(List.of(new SalaEntity("Sala B", true, 5L)));

        mockMvc.perform(get("/salas").with(authentication(autenticacaoDe(Usuario.Perfil.ADMINISTRADOR))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sala B")));
    }

    @Test
    @DisplayName("Solicitante não pode acessar /salas (403)")
    void salas_comoSolicitante_403() throws Exception {
        mockMvc.perform(get("/salas").with(authentication(autenticacaoDe(Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /salas cadastra e redireciona (Administrador)")
    void salas_criar_redireciona() throws Exception {
        mockMvc.perform(post("/salas")
                        .with(authentication(autenticacaoDe(Usuario.Perfil.ADMINISTRADOR)))
                        .param("nome", "Sala Nova")
                        .param("restrito", "false")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
