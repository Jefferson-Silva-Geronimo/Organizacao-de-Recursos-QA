package com.organizacao_de_recursos.dto;

import com.organizacao_de_recursos.model.BloqueioEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record BloqueioRequest(
        @NotNull(message = "Tipo de recurso é obrigatório") TipoRecursoReserva tipoRecurso,
        @NotNull(message = "Recurso é obrigatório") Long recursoId,
        @NotNull(message = "Início é obrigatório") OffsetDateTime inicio,
        @NotNull(message = "Fim é obrigatório") OffsetDateTime fim,
        @NotNull(message = "Motivo é obrigatório") BloqueioEntity.Motivo motivo) {
}
