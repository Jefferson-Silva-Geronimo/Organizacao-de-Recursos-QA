package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador para RN-03: Agenda do Professor
 * A regra de sobreposição também se aplica à agenda do professor alocado.
 */
public class ValidadorAgendaProfessor {

    /**
     * Valida se a reserva tem conflito com agenda de professores
     *
     * @param reserva Reserva com professores alocados
     * @param inicio Data/hora de início da reserva
     * @param fim Data/hora de término da reserva
     * @throws ReservaAgendaProfessorException se houver conflito
     * @throws IllegalArgumentException se professor não for identificado
     */
    public void validarAgenda(Reserva reserva, LocalDateTime inicio, LocalDateTime fim) {
        Professor professor = reserva.getProfessor();
        
        if (professor == null && (reserva.getProfessores() == null || reserva.getProfessores().isEmpty())) {
            throw new IllegalArgumentException("Professor não identificado");
        }

        // Validar professor único
        if (professor != null) {
            if (professor.temConflito(inicio, fim)) {
                throw new ReservaAgendaProfessorException("Professor indisponível");
            }
        }

        // Validar múltiplos professores
        if (reserva.getProfessores() != null) {
            for (Professor prof : reserva.getProfessores()) {
                if (prof.temConflito(inicio, fim)) {
                    throw new ReservaAgendaProfessorException("Professor " + prof.getNome() + " indisponível");
                }
            }
        }
    }
}
