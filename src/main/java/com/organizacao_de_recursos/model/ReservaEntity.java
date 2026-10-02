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
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reserva")
public class ReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "aprovador_id")
    private Long aprovadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fim;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    private long version;

    protected ReservaEntity() {
    }

    public ReservaEntity(Long solicitanteId, EstadoReserva estado, OffsetDateTime inicio, OffsetDateTime fim) {
        this.solicitanteId = solicitanteId;
        this.estado = estado;
        this.inicio = inicio;
        this.fim = fim;
        this.criadoEm = OffsetDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getSolicitanteId() {
        return solicitanteId;
    }

    public Long getAprovadorId() {
        return aprovadorId;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void setAprovadorId(Long aprovadorId) {
        this.aprovadorId = aprovadorId;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public OffsetDateTime getFim() {
        return fim;
    }
}
