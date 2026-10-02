package com.organizacao_de_recursos.dto;

import com.organizacao_de_recursos.domain.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarUsuarioRequest(
        @NotBlank(message = "Usuário é obrigatório") String username,
        @NotBlank(message = "Senha é obrigatória") String senha,
        @NotNull(message = "Perfil é obrigatório") Usuario.Perfil perfil) {
}
