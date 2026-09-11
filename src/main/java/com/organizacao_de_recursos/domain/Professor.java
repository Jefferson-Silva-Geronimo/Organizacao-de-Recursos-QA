package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de domínio representando um Professor
 */
public class Professor {
    private Long id;
    private String nome;
    private List<AgendaProfessor> agenda = new ArrayList<>();

    public Professor(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public void adicionarAgenda(LocalDateTime inicio, LocalDateTime fim) {
        agenda.add(new AgendaProfessor(inicio, fim));
    }

    public boolean temConflito(LocalDateTime inicio, LocalDateTime fim) {
        for (AgendaProfessor ag : agenda) {
            if (ag.temSobreposicao(inicio, fim)) {
                return true;
            }
        }
        return false;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<AgendaProfessor> getAgenda() {
        return agenda;
    }

    // Classe interna para representar agendas
    public static class AgendaProfessor {
        private LocalDateTime inicio;
        private LocalDateTime fim;

        public AgendaProfessor(LocalDateTime inicio, LocalDateTime fim) {
            this.inicio = inicio;
            this.fim = fim;
        }

        public boolean temSobreposicao(LocalDateTime i, LocalDateTime f) {
            return inicio.isBefore(f) && fim.isAfter(i);
        }

        public LocalDateTime getInicio() {
            return inicio;
        }

        public LocalDateTime getFim() {
            return fim;
        }
    }
}
