package com.organizacao_de_recursos.security;

import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.model.ReservaRecursoEntity;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.ReservaRecursoRepository;
import com.organizacao_de_recursos.repository.ReservaRepository;
import com.organizacao_de_recursos.repository.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testa a camada de autorização por objeto (D6/ADR-003) isoladamente (Mockito, sem contexto Spring).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutorizacaoReserva: propriedade e escopo do Responsável (D6)")
class AutorizacaoReservaTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ReservaRecursoRepository reservaRecursoRepository;
    @Mock
    private SalaRepository salaRepository;

    private AutorizacaoReserva autorizacao;

    @BeforeEach
    void setUp() {
        autorizacao = new AutorizacaoReserva(reservaRepository, reservaRecursoRepository, salaRepository);
    }

    private UsuarioEntity usuario(long id, Usuario.Perfil perfil) {
        UsuarioEntity usuario = new UsuarioEntity("user" + id, "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private ReservaEntity reserva(long id, long solicitanteId) {
        ReservaEntity reserva = new ReservaEntity(solicitanteId, null, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }

    private Authentication autenticacaoDe(UsuarioEntity usuario) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    @DisplayName("podeCancelar: dono da reserva pode cancelar")
    void podeCancelar_dono() {
        UsuarioEntity dono = usuario(1L, Usuario.Perfil.SOLICITANTE);
        ReservaEntity reserva = reserva(10L, 1L);
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));

        assertThat(autorizacao.podeCancelar(10L, autenticacaoDe(dono))).isTrue();
    }

    @Test
    @DisplayName("podeCancelar: outro Solicitante não pode cancelar reserva de terceiro")
    void podeCancelar_naoDono() {
        UsuarioEntity intruso = usuario(2L, Usuario.Perfil.SOLICITANTE);
        ReservaEntity reserva = reserva(10L, 1L);
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));

        assertThat(autorizacao.podeCancelar(10L, autenticacaoDe(intruso))).isFalse();
    }

    @Test
    @DisplayName("podeAprovarOuRejeitar: Responsável do recurso pode aprovar (D6)")
    void podeAprovar_responsavelNoEscopo() {
        UsuarioEntity responsavel = usuario(5L, Usuario.Perfil.RESPONSAVEL);
        ReservaEntity reserva = reserva(10L, 1L);
        SalaEntity sala = new SalaEntity("Sala A", true, 5L);
        ReflectionTestUtils.setField(sala, "id", 100L);
        ReservaRecursoEntity recurso = new ReservaRecursoEntity(10L, TipoRecursoReserva.SALA, 100L,
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRecursoRepository.findByReservaId(10L)).thenReturn(List.of(recurso));
        when(salaRepository.findById(100L)).thenReturn(Optional.of(sala));

        assertThat(autorizacao.podeAprovarOuRejeitar(10L, autenticacaoDe(responsavel))).isTrue();
    }

    @Test
    @DisplayName("podeAprovarOuRejeitar: Responsável de outro recurso não pode aprovar (D6)")
    void podeAprovar_responsavelForaDoEscopo() {
        UsuarioEntity responsavel = usuario(5L, Usuario.Perfil.RESPONSAVEL);
        ReservaEntity reserva = reserva(10L, 1L);
        SalaEntity sala = new SalaEntity("Sala A", true, 99L); // atribuída a outro responsável
        ReflectionTestUtils.setField(sala, "id", 100L);
        ReservaRecursoEntity recurso = new ReservaRecursoEntity(10L, TipoRecursoReserva.SALA, 100L,
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRecursoRepository.findByReservaId(10L)).thenReturn(List.of(recurso));
        when(salaRepository.findById(100L)).thenReturn(Optional.of(sala));

        assertThat(autorizacao.podeAprovarOuRejeitar(10L, autenticacaoDe(responsavel))).isFalse();
    }

    @Test
    @DisplayName("podeAprovarOuRejeitar: Solicitante nunca pode aprovar, mesmo sendo o dono")
    void podeAprovar_solicitanteNuncaPode() {
        UsuarioEntity solicitante = usuario(1L, Usuario.Perfil.SOLICITANTE);

        assertThat(autorizacao.podeAprovarOuRejeitar(10L, autenticacaoDe(solicitante))).isFalse();
    }

    @Test
    @DisplayName("podeVer: Administrador vê qualquer reserva, sem consultar o banco")
    void podeVer_administrador() {
        UsuarioEntity admin = usuario(9L, Usuario.Perfil.ADMINISTRADOR);

        assertThat(autorizacao.podeVer(10L, autenticacaoDe(admin))).isTrue();
    }
}
