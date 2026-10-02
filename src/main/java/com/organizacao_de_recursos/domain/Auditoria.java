package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Classe de domínio representando um registro de Auditoria
 */
public class Auditoria {
    private static final AtomicLong CONTADOR_ID = new AtomicLong(0);

    private final Long id;
    private Long reservaId;
    private String usuario;
    private String acao;
    private String estadoAnterior;
    private String estadoNovo;
    private LocalDateTime timestamp;
    private String descricao;

    public Auditoria(Long reservaId, String usuario, String acao, String estadoNovo) {
        this.id = CONTADOR_ID.incrementAndGet();
        this.reservaId = reservaId;
        this.usuario = usuario;
        this.acao = acao;
        this.estadoNovo = estadoNovo;
        this.timestamp = LocalDateTime.now();
    }

    public Auditoria(Long reservaId, String usuario, String acao, String estadoNovo, String estadoAnterior) {
        this(reservaId, usuario, acao, estadoNovo);
        this.estadoAnterior = estadoAnterior;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAcao() {
        return acao;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNovo() {
        return estadoNovo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
