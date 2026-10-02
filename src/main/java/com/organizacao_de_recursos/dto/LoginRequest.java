package com.organizacao_de_recursos.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Usuário é obrigatório") String username,
        @NotBlank(message = "Senha é obrigatória") String senha) {
}
