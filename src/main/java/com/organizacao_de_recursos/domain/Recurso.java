package com.organizacao_de_recursos.domain;

/**
 * Classe de domínio representando um Recurso (Sala, Material, etc.)
 */
public class Recurso {
    public enum TipoRecurso {
        SALA, MATERIAL, PROFESSOR, EQUIPAMENTO
    }

    private Long id;
    private String nome;
    private TipoRecurso tipo;
    private boolean restrito;
    private String descricao;

    public Recurso() {
    }

    public Recurso(Long id, String nome, TipoRecurso tipo) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public void setTipo(TipoRecurso tipo) {
        this.tipo = tipo;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public void setRestrito(boolean restrito) {
        this.restrito = restrito;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
