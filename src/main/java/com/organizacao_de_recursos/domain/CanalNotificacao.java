package com.organizacao_de_recursos.domain;

/**
 * Canal pelo qual uma notificação é produzida (RF-20). A alternativa (notificação simulada ou API externa)
 * segue pendente na arquitetura (J15); esta interface não escolhe nenhuma delas.
 * Uma falha de envio, inclusive a ausência de resposta, é sinalizada por exceção.
 */
public interface CanalNotificacao {

    void enviar(EventoNotificacao evento);
}
