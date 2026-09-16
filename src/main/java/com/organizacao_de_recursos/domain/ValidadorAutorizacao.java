package com.organizacao_de_recursos.domain;

import java.util.Collections;
import java.util.List;

/**
 * Validador para RF-01: Autenticação e Autorização por Perfil
 * Assinatura mínima sem lógica de negócio (Fase RED TDD).
 */
public class ValidadorAutorizacao {

    public List<Recurso> consultarDisponibilidade(Usuario usuario) {
        return Collections.emptyList();
    }

    public void aprovarSolicitacao(Usuario usuario, Reserva reserva) {
        // Sem implementação na fase RED
    }

    public void gerenciarRecurso(Usuario usuario, Recurso recurso) {
        // Sem implementação na fase RED
    }

    public void gerenciarUsuarios(Usuario usuario) {
        // Sem implementação na fase RED
    }

    public boolean validarToken(String token) {
        return false;
    }

    public void validarAcesso(Usuario usuario, String operacao) {
        // Sem implementação na fase RED
    }

    public void validarUsuarioAtivo(Usuario usuario) {
        // Sem implementação na fase RED
    }

    public List<String> obterPermissoes(Usuario usuario) {
        return Collections.emptyList();
    }
}

