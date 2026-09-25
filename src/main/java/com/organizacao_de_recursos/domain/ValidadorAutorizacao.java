package com.organizacao_de_recursos.domain;

import java.util.Collections;
import java.util.List;

/**
 * Validador para RF-01: Autenticação e Autorização por Perfil
 */
public class ValidadorAutorizacao {

    public List<Recurso> consultarDisponibilidade(Usuario usuario) {
        return Collections.emptyList();
    }

    /**
     * Aprova a solicitação (somente perfis com a permissão APROVAR_SOLICITACAO): a reserva passa de
     * SOLICITADA para APROVADA e a mudança de estado gera registro de auditoria (RN-07 e RN-09).
     */
    public void aprovarSolicitacao(Usuario usuario, Reserva reserva) {
        validarAcesso(usuario, "APROVAR_SOLICITACAO");
        new ValidadorFluxoEstados().validarTransicao(reserva, "APROVADA");
        String estadoAnterior = reserva.getEstado();
        ValidadorAuditoria.registrarAuditoriaPendente(
                new Auditoria(reserva.getId(), usuario.getUsername(), "APROVAR", "APROVADA", estadoAnterior));
        reserva.setEstado("APROVADA");
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

