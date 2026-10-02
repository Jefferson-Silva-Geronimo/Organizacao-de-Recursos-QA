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

/** Uma tentativa de reserva recusada pela constraint de exclusão (RN-04) - alimenta o relatório de conflitos evitados. */
@Entity
@Table(name = "conflito_evitado")
public class ConflitoEvitadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private TipoRecursoReserva tipoRecurso;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(name = "solicitante_id")
    private Long solicitanteId;

    @Column(name = "tentado_em", nullable = false)
    private OffsetDateTime tentadoEm;

    protected ConflitoEvitadoEntity() {
    }

    public ConflitoEvitadoEntity(TipoRecursoReserva tipoRecurso, Long recursoId, Long solicitanteId) {
        this.tipoRecurso = tipoRecurso;
        this.recursoId = recursoId;
        this.solicitanteId = solicitanteId;
        this.tentadoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getTentadoEm() {
        return tentadoEm;
    }
}
