package com.organizacao_de_recursos.domain;

/**
 * Resultado observável de uma notificação: entregue ao canal ou falha registrada (RF-20).
 * A mensagem nunca traz detalhes internos da falha.
 */
public record ResultadoNotificacao(EventoNotificacao evento, boolean entregue, String mensagem) {
}
