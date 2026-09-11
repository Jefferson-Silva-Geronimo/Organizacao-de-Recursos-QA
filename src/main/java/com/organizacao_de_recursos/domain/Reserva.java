package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de domínio representando uma Reserva de Recurso
 */
public class Reserva {
    private Long id;
    private String titulo;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private String estado;
    private Recurso recurso;
    private Professor professor;
    private List<Professor> professores = new ArrayList<>();

    public Reserva() {
    }

    /**
     * Valida a ordem temporal da reserva (RN-01)
     * O término deve ser posterior ao início
     *
     * @param inicio Data/hora de início
     * @param fim    Data/hora de término
     * @throws IllegalArgumentException se início ou fim for nulo
     * @throws ReservaTemporalException se a ordem temporal for inválida
     */
    public void validarTemporalidade(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) {
            throw new IllegalArgumentException("Início é obrigatório");
        }
        if (fim == null) {
            throw new IllegalArgumentException("Fim é obrigatório");
        }

        // Verificar se data está no passado
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new ReservaTemporalException("Data no passado");
        }

        // Verificar se fim é anterior ao início
        if (fim.isBefore(inicio)) {
            throw new ReservaTemporalException("Fim anterior ao início");
        }

        // Verificar se início e fim são iguais (duração zero)
        if (fim.isEqual(inicio)) {
            throw new ReservaTemporalException("Duração inválida");
        }
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public List<Professor> getProfessores() {
        return professores;
    }

    public void adicionarProfessor(Professor professor) {
        this.professores.add(professor);
    }
}
