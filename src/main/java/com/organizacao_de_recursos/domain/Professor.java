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

    /**
     * Conflito com a agenda desconsiderando a primeira entrada igual ao período informado
     * (usado quando o próprio período da reserva já está na agenda do professor).
     */
    public boolean temConflitoIgnorando(LocalDateTime inicio, LocalDateTime fim,
                                        LocalDateTime inicioIgnorado, LocalDateTime fimIgnorado) {
        boolean ignorou = false;
        for (AgendaProfessor ag : agenda) {
            if (!ignorou && ag.getInicio().equals(inicioIgnorado) && ag.getFim().equals(fimIgnorado)) {
                ignorou = true;
                continue;
            }
            if (ag.temSobreposicao(inicio, fim)) {
                return true;
            }
        }
        return false;
    }

    /** Troca a primeira entrada da agenda igual ao período antigo pelo novo período. */
    public void substituirAgenda(LocalDateTime inicioAntigo, LocalDateTime fimAntigo,
                                 LocalDateTime novoInicio, LocalDateTime novoFim) {
        for (int indice = 0; indice < agenda.size(); indice++) {
            AgendaProfessor ag = agenda.get(indice);
            if (ag.getInicio().equals(inicioAntigo) && ag.getFim().equals(fimAntigo)) {
                agenda.remove(indice);
                break;
            }
        }
        agenda.add(new AgendaProfessor(novoInicio, novoFim));
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

    public void validarFormatoAgenda(String inicio, String fim) {
        try {
            java.time.LocalTime.parse(inicio);
            java.time.LocalTime.parse(fim);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Formato de hora inválido", e);
        }
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
