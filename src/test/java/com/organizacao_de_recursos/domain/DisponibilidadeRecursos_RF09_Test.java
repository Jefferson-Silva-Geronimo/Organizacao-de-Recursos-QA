package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-09: Pesquisa por filtros e disponibilidade
 * Identifier: RF-09 | docs/prd.md:7.9 | E3: Pesquisa e Disponibilidade
 *
 * Casos de teste mapeados:
 * - T-RF09-001: Happy Path - Pesquisa por filtros [BLOQUEADO_POR_LACUNA: capacidade, localização e competência sem definição]
 * - T-RF09-002: Conflicts - Recurso reservado, bloqueado, em manutenção ou com professor ocupado não aparece disponível
 */
@DisplayName("RF-09: Pesquisa por filtros e disponibilidade")
class DisponibilidadeRecursos_RF09_Test {

    private static final LocalDateTime INICIO = LocalDateTime.now().plusDays(5)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);
    private static final LocalDateTime FIM = INICIO.plusHours(1);

    enum Impedimento { RESERVADO, BLOQUEADO, EM_MANUTENCAO, PROFESSOR_COM_AGENDA_OCUPADA }

    @ParameterizedTest(name = "recurso {0} não é apresentado como disponível")
    @EnumSource(Impedimento.class)
    @DisplayName("T-RF09-002: Conflicts - Recurso indisponível no período não é apresentado como disponível")
    void recursoIndisponivelNaoDeveAparecerComoDisponivel(Impedimento impedimento) {
        // Arrange
        Usuario administrador = new Usuario(1L, "admin", Usuario.Perfil.ADMINISTRADOR);
        Usuario outroSolicitante = new Usuario(3L, "outro", Usuario.Perfil.SOLICITANTE);
        Recurso sala = new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA);
        Professor professor = new Professor(7L, "Prof Carlos");
        ValidadorManutencao manutencao = new ValidadorManutencao();
        GestaoBloqueios bloqueios = new GestaoBloqueios();
        ServicoCriacaoReserva reservas = new ServicoCriacaoReserva(manutencao);
        switch (impedimento) {
            case RESERVADO -> {
                Reserva existente = new Reserva();
                existente.setRecurso(sala);
                existente.setInicio(INICIO);
                existente.setFim(FIM);
                reservas.criarReserva(outroSolicitante, existente);
            }
            case BLOQUEADO -> bloqueios.registrarBloqueio(administrador, sala, INICIO, FIM);
            case EM_MANUTENCAO -> manutencao.registrarManutencao(sala, INICIO, FIM);
            case PROFESSOR_COM_AGENDA_OCUPADA -> professor.adicionarAgenda(INICIO, FIM);
        }
        ConsultaDisponibilidade consulta = new ConsultaDisponibilidade(reservas, manutencao, bloqueios);

        // Act
        boolean disponivelNoPeriodo = consulta.estaDisponivel(sala, professor, INICIO, FIM);
        boolean disponivelDepois = consulta.estaDisponivel(sala, professor, FIM.plusHours(2), FIM.plusHours(3));

        // Assert
        assertThat(disponivelNoPeriodo).isFalse();
        assertThat(disponivelDepois).isTrue();
    }
}
