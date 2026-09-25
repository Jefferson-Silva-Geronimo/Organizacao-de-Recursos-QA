package com.organizacao_de_recursos.domain;

/**
 * Contrato mínimo (fase RED, RF-05): gestão de usuários pelo Administrador.
 * Sem lógica de negócio. Implementação pendente.
 */
public class GestaoUsuarios {

    /** Define o perfil oficial do usuário alvo (somente Administrador) e devolve o usuário atualizado. */
    public Usuario definirPerfil(Usuario administrador, Usuario alvo, Usuario.Perfil novoPerfil) {
        return alvo;
    }
}
