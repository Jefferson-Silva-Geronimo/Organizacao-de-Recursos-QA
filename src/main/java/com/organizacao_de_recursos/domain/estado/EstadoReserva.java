package com.organizacao_de_recursos.domain.estado;

/**
 * Vocabulário canônico de estados de reserva (seção 3 do plano de migração).
 * Fluxo principal: SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA.
 * Alternativos: REJEITADA, CANCELADA, NAO_COMPARECEU (todos finais).
 */
public enum EstadoReserva {
    SOLICITADA,
    APROVADA,
    EM_USO,
    CONCLUIDA,
    REJEITADA,
    CANCELADA,
    NAO_COMPARECEU
}
