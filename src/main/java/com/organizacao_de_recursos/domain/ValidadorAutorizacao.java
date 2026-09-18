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
        validarAcesso(usuario, "APROVAR_SOLICITACAO");
    }

    public void gerenciarRecurso(Usuario usuario, Recurso recurso) {
        validarAcesso(usuario, "GERENCIAR_RECURSOS");
    }

    public void gerenciarUsuarios(Usuario usuario) {
        validarAcesso(usuario, "GERENCIAR_USUARIOS");
    }

    public boolean validarToken(String token) {
        return token != null && token.startsWith("Bearer ") && token.length() > 7;
    }

    public void validarAcesso(Usuario usuario, String operacao) {
        validarUsuarioAtivo(usuario);
        if (!obterPermissoes(usuario).contains(operacao)) {
            throw new AcessoNegadoException("Acesso negado");
        }
    }

    public void validarUsuarioAtivo(Usuario usuario) {
        if (usuario == null || !usuario.isAtivo()) {
            throw new AcessoNegadoException("Usuário inativo");
        }
    }

    public List<String> obterPermissoes(Usuario usuario) {
        validarUsuarioAtivo(usuario);
        switch (usuario.getPerfil()) {
            case ADMINISTRADOR:
                return List.of("GERENCIAR_RECURSOS", "GERENCIAR_USUARIOS", "CONSULTAR_DISPONIBILIDADE");
            case RESPONSAVEL:
                return List.of("APROVAR_SOLICITACAO", "CONSULTAR_DISPONIBILIDADE");
            case SOLICITANTE:
                return List.of("CONSULTAR_DISPONIBILIDADE");
            default:
                return Collections.emptyList();
        }
    }
}

