package com.organizacao_de_recursos.notification;

/** Porta de notificação do fluxo de reservas (RF-20). */
public interface Notificador {

    void notificar(Long reservaId, String tipo);
}
