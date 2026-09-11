package com.organizacao_de_recursos.domain;

/**
 * Validador para RN-06: Aprovação de Recursos Restritos
 * Recursos restritos exigem aprovação; somente Responsável pode aprová-los.
 */
public class ValidadorAprovacao {

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
    public void aprovar(Reserva reserva, Usuario usuario) {
        if (usuario.getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            throw new ReservaAprovacaoException("Apenas Responsável pode aprovar");
        }

        // Lógica de aprovação aqui
    }

    /**
     * Rejeita uma reserva (apenas Responsável pode)
     */
    public void rejeitar(Reserva reserva, Usuario usuario) {
        if (usuario.getPerfil() != Usuario.Perfil.RESPONSAVEL) {
            throw new ReservaAprovacaoException("Apenas Responsável pode rejeitar");
        }

        // Lógica de rejeição aqui
    }
}
