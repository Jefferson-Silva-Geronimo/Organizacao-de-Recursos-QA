package com.organizacao_de_recursos.notification;

/** Evento de domínio que dispara notificação após aprovação, rejeição ou cancelamento (RF-20). */
public record ReservaNotificacaoEvent(Long reservaId, String tipo) {
}
