package com.organizacao_de_recursos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes para RF-15: Validação da alocação de docentes
 * Identifier: RF-15 | docs/prd.md:7.15 | E5: Aprovação de Recursos Restritos
 *
 * Casos de teste mapeados:
 * - T-RF15-001: [BLOQUEADO_POR_LACUNA] - o que é e onde se registra "o resultado da validação" não está definido
 * - T-RF15-002: Conflicts - Alocação que conflita com a agenda do professor não é validada
 */
@DisplayName("RF-15: Validação da alocação de docentes")
class ValidacaoAlocacaoDocente_RF15_Test {

    private static final LocalDateTime DIA_08H = LocalDateTime.now().plusDays(9)
            .withHour(8).withMinute(0).withSecond(0).withNano(0);

    @Test
    @DisplayName("T-RF15-002: Conflicts - Alocação em conflito com a agenda do professor informa o conflito e não é validada")
    void alocacaoEmConflitoComAgendaNaoDeveSerValidada() {
        // Arrange - Prof Carlos ocupado 08:00-09:00; alocação 08:30-09:30
        Usuario responsavel = new Usuario(2L, "responsavel", Usuario.Perfil.RESPONSAVEL);
        Professor professor = new Professor(7L, "Prof Carlos");
        professor.adicionarAgenda(DIA_08H, DIA_08H.plusHours(1));
        Reserva reserva = new Reserva();
        reserva.setRecurso(new Recurso(1L, "Sala A", Recurso.TipoRecurso.SALA));
        reserva.setProfessor(professor);
        reserva.setInicio(DIA_08H.plusMinutes(30));
        reserva.setFim(DIA_08H.plusMinutes(90));
        ValidacaoAlocacaoDocente validacao = new ValidacaoAlocacaoDocente();

        // Act & Assert - o conflito é informado (RN-03) e a alocação não fica validada
        assertThatThrownBy(() -> validacao.validar(responsavel, reserva))
                .isInstanceOf(ReservaAgendaProfessorException.class)
                .hasMessageContaining("Professor indisponível");
        assertThat(validacao.foiValidada(reserva)).isFalse();
    }
}
