package com.organizacao_de_recursos.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Canal de notificação simulada (perfil dev) - apenas loga, não chama nenhum serviço externo. */
public class NotificadorSimulado implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorSimulado.class);

    @Override
    public void notificar(Long reservaId, String tipo) {
        log.info("[notificação simulada] reserva {} - {}", reservaId, tipo);
    }
}
