package com.organizacao_de_recursos.dto;

import com.organizacao_de_recursos.model.SalaEntity;

public record SalaResponse(Long id, String nome, boolean restrito, boolean ativo, Long responsavelId) {

    public static SalaResponse de(SalaEntity sala) {
        return new SalaResponse(sala.getId(), sala.getNome(), sala.isRestrito(), sala.isAtivo(), sala.getResponsavelId());
    }
}
