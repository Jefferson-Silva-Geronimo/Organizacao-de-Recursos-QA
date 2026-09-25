package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Alteração e cancelamento de reservas pelo Solicitante (RF-11 e RF-12).
 *
 * Somente o solicitante da reserva pode alterá-la ou cancelá-la (autorização por objeto, ADR-003).
 * Reserva iniciada não pode ser alterada nem cancelada. A alteração revalida manutenção, sobreposição
 * e agenda do professor (ADR-005) e o cancelamento gera auditoria (RN-09).
 *
 * A auditoria de alteração de estado por alteração de período (RF-11, critério 3) não foi implementada:
 * qual alteração muda o estado da reserva é decisão pendente.
 */
public class ServicoGestaoReserva {
    private final ServicoCriacaoReserva reservas;
    private final ValidadorManutencao manutencao;
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();

    public ServicoGestaoReserva(ServicoCriacaoReserva reservas, ValidadorManutencao manutencao) {
        this.reservas = reservas;
        this.manutencao = manutencao;
    }

    /** Altera o período da reserva do próprio solicitante. */
    public void alterarReserva(Usuario solicitante, Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        validarSolicitanteDaReserva(solicitante, reserva, "alterá-la");
        reserva.validarTemporalidade(novoInicio, novoFim);
        validarManutencao(reserva, novoInicio, novoFim);
        reservas.alterarPeriodo(reserva, novoInicio, novoFim);
    }

    /** Cancela a reserva do próprio solicitante. */
    public void cancelarReserva(Usuario usuario, Reserva reserva) {
        validarSolicitanteDaReserva(usuario, reserva, "cancelá-la");
        new ValidadorApagamento().validarApagamento(reserva);
        new ValidadorFluxoEstados().validarTransicao(reserva, "CANCELADA");
        ValidadorAuditoria.registrarAuditoriaPendente(
                new Auditoria(reserva.getId(), usuario.getUsername(), "CANCELAR", "CANCELADA", reserva.getEstado()));
        reserva.setEstado("CANCELADA");
    }

    private void validarSolicitanteDaReserva(Usuario usuario, Reserva reserva, String operacao) {
        autorizacao.validarUsuarioAtivo(usuario);
        Usuario dono = reserva.getUsuarioSolicitante();
        if (dono == null || !Objects.equals(dono.getId(), usuario.getId())) {
            throw new AcessoNegadoException("Acesso negado. Somente o solicitante da reserva pode " + operacao);
        }
    }

    private void validarManutencao(Reserva reserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        manutencao.validarAlteracaoManutencao(reserva, novoInicio, novoFim);
        for (Recurso material : reserva.getMateriais()) {
            if (!manutencao.verificarDisponibilidade(material, novoInicio, novoFim)) {
                throw new ReservaManutencaoException("Material indisponível no período");
            }
        }
    }
}
