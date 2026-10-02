package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.config.SecurityConfig;
import com.organizacao_de_recursos.domain.ReservaAprovacaoException;
import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.domain.estado.EstadoReserva;
import com.organizacao_de_recursos.model.ReservaEntity;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de controller + segurança (rota/método/objeto, ADR-003/ADR-018) para ReservaController.
 */
@WebMvcTest(ReservaController.class)
@Import(SecurityConfig.class)
class ReservaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaService reservaService;
    @MockBean(name = "autorizacaoReserva")
    private AutorizacaoReserva autorizacaoReserva;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    private Authentication autenticacaoDe(long id, Usuario.Perfil perfil) {
        var usuario = new com.organizacao_de_recursos.model.UsuarioEntity("user" + id, "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("Rota: criar reserva sem autenticação deve retornar 401")
    void criar_semAutenticacao_401() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaId\":1,\"inicio\":\"2026-11-01T08:00:00-03:00\",\"fim\":\"2026-11-01T09:00:00-03:00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Método: Responsável não pode criar reserva (apenas Solicitante)")
    void criar_comoResponsavel_403() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .with(authentication(autenticacaoDe(1L, Usuario.Perfil.RESPONSAVEL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaId\":1,\"inicio\":\"2026-11-01T08:00:00-03:00\",\"fim\":\"2026-11-01T09:00:00-03:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Happy path: Solicitante cria reserva (201)")
    void criar_comoSolicitante_201() throws Exception {
        ReservaEntity criada = new ReservaEntity(1L, EstadoReserva.SOLICITADA,
                OffsetDateTime.parse("2026-11-01T08:00:00-03:00"), OffsetDateTime.parse("2026-11-01T09:00:00-03:00"));
        ReflectionTestUtils.setField(criada, "id", 50L);
        when(reservaService.criar(anyLong(), anyLong(), any(), any())).thenReturn(criada);

        mockMvc.perform(post("/api/v1/reservas")
                        .with(authentication(autenticacaoDe(1L, Usuario.Perfil.SOLICITANTE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaId\":1,\"inicio\":\"2026-11-01T08:00:00-03:00\",\"fim\":\"2026-11-01T09:00:00-03:00\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Objeto: consultar reserva de outro Solicitante deve retornar 403")
    void obter_semPermissaoDeObjeto_403() throws Exception {
        when(autorizacaoReserva.podeVer(eq(10L), any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/reservas/10")
                        .with(authentication(autenticacaoDe(2L, Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Reserva inexistente deve retornar 404")
    void obter_inexistente_404() throws Exception {
        when(autorizacaoReserva.podeVer(eq(99L), any())).thenReturn(true);
        when(reservaService.buscarPorId(99L)).thenThrow(new NoSuchElementException("Reserva não encontrada"));

        mockMvc.perform(get("/api/v1/reservas/99")
                        .with(authentication(autenticacaoDe(1L, Usuario.Perfil.ADMINISTRADOR))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Objeto: cancelar reserva de outro Solicitante deve retornar 403")
    void cancelar_semPermissaoDeObjeto_403() throws Exception {
        when(autorizacaoReserva.podeCancelar(eq(10L), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/reservas/10/cancelar")
                        .with(authentication(autenticacaoDe(2L, Usuario.Perfil.SOLICITANTE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Objeto: Responsável fora do escopo não pode aprovar (D6) - 422 vindo da regra de negócio")
    void aprovar_foraDoEscopo_422() throws Exception {
        when(autorizacaoReserva.podeAprovarOuRejeitar(eq(10L), any())).thenReturn(true);
        when(reservaService.aprovar(10L, 5L)).thenThrow(new ReservaAprovacaoException("Recurso fora de sua responsabilidade"));

        mockMvc.perform(post("/api/v1/reservas/10/aprovar")
                        .with(authentication(autenticacaoDe(5L, Usuario.Perfil.RESPONSAVEL))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Happy path: Responsável no escopo aprova a reserva (200)")
    void aprovar_noEscopo_200() throws Exception {
        ReservaEntity aprovada = new ReservaEntity(1L, EstadoReserva.APROVADA,
                OffsetDateTime.parse("2026-11-01T08:00:00-03:00"), OffsetDateTime.parse("2026-11-01T09:00:00-03:00"));
        ReflectionTestUtils.setField(aprovada, "id", 10L);
        when(autorizacaoReserva.podeAprovarOuRejeitar(eq(10L), any())).thenReturn(true);
        when(reservaService.aprovar(10L, 5L)).thenReturn(aprovada);

        mockMvc.perform(post("/api/v1/reservas/10/aprovar")
                        .with(authentication(autenticacaoDe(5L, Usuario.Perfil.RESPONSAVEL))))
                .andExpect(status().isOk());
    }
}
