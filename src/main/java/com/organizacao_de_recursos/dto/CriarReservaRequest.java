package com.organizacao_de_recursos.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record CriarReservaRequest(
        @NotNull(message = "Sala é obrigatória") Long salaId,
        @NotNull(message = "Início é obrigatório") OffsetDateTime inicio,
        @NotNull(message = "Fim é obrigatório") OffsetDateTime fim) {
}
