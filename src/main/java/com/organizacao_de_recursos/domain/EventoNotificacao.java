package com.organizacao_de_recursos.domain;

/**
 * Evento do fluxo de reservas que aciona uma notificação (RF-20).
 * O tipo do evento é livre: quais eventos notificam, o canal, o destinatário e o conteúdo seguem pendentes.
 */
public record EventoNotificacao(Long reservaId, String tipo) {
}
