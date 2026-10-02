package com.organizacao_de_recursos.dto;

import com.organizacao_de_recursos.model.BloqueioEntity;

import java.time.OffsetDateTime;

public record BloqueioResponse(Long id, String tipoRecurso, Long recursoId, OffsetDateTime inicio, OffsetDateTime fim) {

    public static BloqueioResponse de(BloqueioEntity bloqueio) {
        return new BloqueioResponse(bloqueio.getId(), bloqueio.getTipoRecurso().name(), bloqueio.getRecursoId(),
                bloqueio.getInicio(), bloqueio.getFim());
    }
}
