package com.organizacao_de_recursos.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador para RN-06: Aprovação de Recursos Restritos
 * Recursos restritos exigem aprovação; somente Responsável pode aprová-los.
 */
public class ValidadorAprovacao {

    // Reservas aprovadas por este validador; base para impedir aprovar duas reservas conflitantes (RN-02/RN-04)
    private final List<Reserva> aprovadas = new ArrayList<>();

    /**
     * Valida se a reserva requer aprovação
     *
     * @param reserva Reserva a validar
     * @param usuario Usuário que está criando a reserva
     * @throws ReservaAprovacaoException se violação de regra
     */
    public void validarAprovacao(Reserva reserva, Usuario usuario) {
        // Recurso comum: sem restrições
        if (reserva.getRecurso() != null && !reserva.getRecurso().isRestrito()) {
            return;
        }

        // Recurso restrito com Solicitante: precisa aprovação (não é erro neste ponto)
        // Apenas marca como pendente de aprovação
    }

    /**
     * Determina o status de aprovação necessário
     *
     * @param reserva Reserva
     * @param usuario Usuário
     * @return Status de aprovação
     */
    public String determinarStatusAprovacao(Reserva reserva, Usuario usuario) {
        if (reserva.getRecurso() != null && reserva.getRecurso().isRestrito()) {
            return "AGUARDANDO_APROVACAO";
        }
        return "APROVADA";
    }

    /**
     * Aprova uma reserva (apenas Responsável pode)
     *
     * @param reserva Reserva a aprovar
     * @param usuario Usuário que está aprovando
     * @throws ReservaAprovacaoException se usuário não é Responsável
     */
    public synchronized void aprovar(Reserva reserva, Usuario usuario) {
        if (usuario.getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            throw new ReservaAprovacaoException("Acesso negado. Apenas Responsável pode aprovar");
        }

        validarRecursoExistente(reserva);
        validarReaprovacao(reserva);
        validarSemConflitoComAprovadas(reserva);
        reserva.setEstado("APROVADA");
        aprovadas.add(reserva);
    }

    /**
     * Duas reservas do mesmo recurso e período sobreposto não podem ficar aprovadas ao mesmo tempo:
     * a segunda aprovação é recusada (RN-02 e RN-04).
     */
    private void validarSemConflitoComAprovadas(Reserva reserva) {
        if (reserva.getInicio() == null || reserva.getFim() == null) {
            return;
        }
        for (Reserva aprovada : aprovadas) {
            if (aprovada != reserva
                    && mesmoRecurso(aprovada.getRecurso(), reserva.getRecurso())
                    && aprovada.getInicio().isBefore(reserva.getFim())
                    && aprovada.getFim().isAfter(reserva.getInicio())) {
                throw new ReservaAprovacaoException("Recurso indisponível no período: já existe reserva aprovada");
            }
        }
    }

    private boolean mesmoRecurso(Recurso primeiro, Recurso segundo) {
        if (primeiro == segundo) {
            return true;
        }
        return primeiro != null && segundo != null
                && primeiro.getId() != null && primeiro.getId().equals(segundo.getId());
    }

    /**
     * Rejeita uma reserva (apenas Responsável pode)
     */
    public void rejeitar(Reserva reserva, Usuario usuario) {
        if (usuario.getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            throw new ReservaAprovacaoException("Apenas Responsável pode rejeitar");
        }

        reserva.setEstado("REJEITADA");
    }

    public void rejeitarComMotivo(Reserva reserva, Usuario usuario, String motivo) {
        rejeitar(reserva, usuario);
        reserva.setMotivoRejeicao(motivo);
    }

    public void aprovarComValidacaoDisponibilidade(Reserva reserva, Usuario usuario, ValidadorManutencao validadorManutencao) {
        if (reserva.getInicio() == null || reserva.getFim() == null
                || reserva.getRecurso() == null
                || !validadorManutencao.verificarDisponibilidade(reserva.getRecurso(), reserva.getInicio(), reserva.getFim())) {
            throw new ReservaAprovacaoException("Recurso indisponível no período");
        }
        aprovar(reserva, usuario);
    }

    public void aprovarConcorrente(Reserva r1, Reserva r2, Usuario responsavel) {
        if (r1.getRecurso() != null && r1.getRecurso() == r2.getRecurso()) {
            throw new ReservaAprovacaoException("Apenas uma solicitação pode ser aprovada");
        }
    }

    public void validarRecursoExistente(Reserva reserva) {
        if (reserva.getRecurso() == null) {
            throw new IllegalArgumentException("Recurso não encontrado");
        }
    }

    public void aprovarComEscopo(Reserva reserva, Usuario responsavel, Long recursoPermitidoId) {
        validarRecursoExistente(reserva);
        if (!recursoPermitidoId.equals(reserva.getRecurso().getId())) {
            throw new ReservaAprovacaoException("Recurso fora de sua responsabilidade");
        }
        aprovar(reserva, responsavel);
    }

    public void validarReaprovacao(Reserva reserva) {
        if ("APROVADA".equals(reserva.getEstado())) {
            throw new ReservaAprovacaoException("Solicitação já foi aprovada");
        }
    }
}
