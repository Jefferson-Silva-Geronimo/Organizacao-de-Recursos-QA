package com.organizacao_de_recursos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "bloqueio")
public class BloqueioEntity {

    public enum Motivo { MANUTENCAO, ADMINISTRATIVO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private TipoRecursoReserva tipoRecurso;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Motivo motivo;

    @Column(name = "criado_por")
    private Long criadoPor;

    protected BloqueioEntity() {
    }

    public BloqueioEntity(TipoRecursoReserva tipoRecurso, Long recursoId, OffsetDateTime inicio,
                           OffsetDateTime fim, Motivo motivo, Long criadoPor) {
        this.tipoRecurso = tipoRecurso;
        this.recursoId = recursoId;
        this.inicio = inicio;
        this.fim = fim;
        this.motivo = motivo;
        this.criadoPor = criadoPor;
    }

    public Long getId() {
        return id;
    }

    public TipoRecursoReserva getTipoRecurso() {
        return tipoRecurso;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public OffsetDateTime getFim() {
        return fim;
    }
}
