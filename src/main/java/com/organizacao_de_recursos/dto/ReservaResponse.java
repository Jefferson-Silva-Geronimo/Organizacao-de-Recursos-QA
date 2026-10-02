package com.organizacao_de_recursos.dto;

import com.organizacao_de_recursos.model.ReservaEntity;

import java.time.OffsetDateTime;

public record ReservaResponse(Long id, String estado, Long solicitanteId, Long aprovadorId,
                               OffsetDateTime inicio, OffsetDateTime fim) {

    public static ReservaResponse de(ReservaEntity reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getEstado().name(), reserva.getSolicitanteId(),
                reserva.getAprovadorId(), reserva.getInicio(), reserva.getFim());
    }
}
