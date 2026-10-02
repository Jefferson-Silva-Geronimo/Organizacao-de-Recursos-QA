package com.organizacao_de_recursos.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Dispara a notificação só depois que a transação confirma (D4/ADR-004 - nunca antes do commit). */
@Component
public class NotificacaoListener {

    private final Notificador notificador;

    public NotificacaoListener(Notificador notificador) {
        this.notificador = notificador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarMudancaDeEstado(ReservaNotificacaoEvent evento) {
        notificador.notificar(evento.reservaId(), evento.tipo());
    }
}
