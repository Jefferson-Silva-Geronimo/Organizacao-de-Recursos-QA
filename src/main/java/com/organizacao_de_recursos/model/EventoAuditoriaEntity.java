package com.organizacao_de_recursos.model;

import com.organizacao_de_recursos.domain.estado.EstadoReserva;
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
 * Registro append-only de mudança de estado (D2/ADR-004). Gravado na mesma transação da
 * mudança; ordenação canônica (ocorrido_em, id) - o id sequencial desempata timestamps iguais.
 */
@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false, length = 20)
    private EstadoReserva estadoNovo;

    @Column(name = "ator_id")
    private Long atorId;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    private String detalhe;

    protected EventoAuditoriaEntity() {
    }

    public EventoAuditoriaEntity(Long reservaId, EstadoReserva estadoAnterior, EstadoReserva estadoNovo, Long atorId) {
        this.reservaId = reservaId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.atorId = atorId;
        this.ocorridoEm = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public EstadoReserva getEstadoNovo() {
        return estadoNovo;
    }
}
