package com.organizacao_de_recursos.dto;

import com.organizacao_de_recursos.model.EventoAuditoriaEntity;

public record EventoAuditoriaResponse(Long id, String estadoNovo) {

    public static EventoAuditoriaResponse de(EventoAuditoriaEntity evento) {
        return new EventoAuditoriaResponse(evento.getId(), evento.getEstadoNovo().name());
    }
}
