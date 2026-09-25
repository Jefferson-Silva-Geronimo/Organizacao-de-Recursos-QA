package com.organizacao_de_recursos.domain;

/**
 * Gestão de usuários pelo Administrador (RF-05).
 *
 * Exige a permissão GERENCIAR_USUARIOS (ADR-003). A alteração de perfil devolve o usuário atualizado,
 * que a autorização passa a usar; o objeto original não é modificado.
 */
public class GestaoUsuarios {
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();

    /** Define o perfil oficial do usuário alvo (somente Administrador) e devolve o usuário atualizado. */
    public Usuario definirPerfil(Usuario administrador, Usuario alvo, Usuario.Perfil novoPerfil) {
        autorizacao.validarAcesso(administrador, "GERENCIAR_USUARIOS");
        if (alvo == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        if (novoPerfil == null) {
            throw new IllegalArgumentException("Perfil é obrigatório");
        }
        Usuario atualizado = new Usuario(alvo.getId(), alvo.getUsername(), novoPerfil);
        atualizado.setAtivo(alvo.isAtivo());
        return atualizado;
    }
}
