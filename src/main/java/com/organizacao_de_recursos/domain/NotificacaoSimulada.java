package com.organizacao_de_recursos.domain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canal de notificação simulada (RF-20): em vez de chamar um serviço externo, guarda as notificações
 * produzidas para que possam ser observadas.
 */
public class NotificacaoSimulada implements CanalNotificacao {
    private final List<EventoNotificacao> enviados = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(EventoNotificacao evento) {
        enviados.add(evento);
    }

    /** Notificações simuladas produzidas até agora, em ordem de envio. */
    public List<EventoNotificacao> enviados() {
        return List.copyOf(enviados);
    }
}
