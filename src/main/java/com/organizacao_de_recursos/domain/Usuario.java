package com.organizacao_de_recursos.domain;

/**
 * Classe de domínio representando um Usuário
 */
public class Usuario {
    public enum Perfil {
        SOLICITANTE, RESPONSAVEL, ADMINISTRADOR
    }

    private Long id;
    private String username;
    private Perfil perfil;

    private boolean ativo = true;

    public Usuario(Long id, String username, Perfil perfil) {
        this.id = id;
        this.username = username;
        this.perfil = perfil;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
