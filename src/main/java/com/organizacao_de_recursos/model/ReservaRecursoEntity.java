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

/**
 * Uma linha por recurso envolvido em uma reserva (sala, professor ou material, D5/D7).
 * A garantia de RN-04 é a constraint de exclusão da migration V1, não esta classe.
 */
@Entity
@Table(name = "reserva_recurso")
public class ReservaRecursoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private TipoRecursoReserva tipoRecurso;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fim;

    @Column(nullable = false)
    private boolean ocupa = true;

    protected ReservaRecursoEntity() {
    }

    public ReservaRecursoEntity(Long reservaId, TipoRecursoReserva tipoRecurso, Long recursoId,
                                 OffsetDateTime inicio, OffsetDateTime fim) {
        this.reservaId = reservaId;
        this.tipoRecurso = tipoRecurso;
        this.recursoId = recursoId;
        this.inicio = inicio;
        this.fim = fim;
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public TipoRecursoReserva getTipoRecurso() {
        return tipoRecurso;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public boolean isOcupa() {
        return ocupa;
    }

    public void setOcupa(boolean ocupa) {
        this.ocupa = ocupa;
    }
}
