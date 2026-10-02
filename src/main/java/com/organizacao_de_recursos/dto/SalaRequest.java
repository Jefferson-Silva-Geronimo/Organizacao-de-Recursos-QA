package com.organizacao_de_recursos.dto;

import jakarta.validation.constraints.NotBlank;

public record SalaRequest(@NotBlank(message = "Nome é obrigatório") String nome,
                           boolean restrito,
                           Long responsavelId) {
}
