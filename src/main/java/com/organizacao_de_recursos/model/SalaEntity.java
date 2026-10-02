package com.organizacao_de_recursos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sala")
public class SalaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private Integer capacidade;

    private String localizacao;

    @Column(nullable = false)
    private boolean restrito;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "responsavel_id")
    private Long responsavelId;

    protected SalaEntity() {
    }

    public SalaEntity(String nome, boolean restrito, Long responsavelId) {
        this.nome = nome;
        this.restrito = restrito;
        this.responsavelId = responsavelId;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public void setRestrito(boolean restrito) {
        this.restrito = restrito;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }
}
