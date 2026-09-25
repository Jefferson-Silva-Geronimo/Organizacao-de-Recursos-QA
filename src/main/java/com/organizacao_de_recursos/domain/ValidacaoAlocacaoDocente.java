package com.organizacao_de_recursos.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Validação da alocação de docentes pelo Responsável (RF-15).
 *
 * Exige a permissão VALIDAR_ALOCACAO_DOCENTE (ADR-003). Alocação que conflita com a agenda do
 * professor informa o conflito (RN-03) e não é considerada validada.
 *
 * O registro do "resultado da validação" além de validada ou não validada depende de definição
 * pendente (T-RF15-001 bloqueado) e não foi implementado.
 */
public class ValidacaoAlocacaoDocente {
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();
    private final Set<Reserva> validadas = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Valida a alocação do professor da reserva; conflito com a agenda do professor impede a validação. */
    public synchronized void validar(Usuario responsavel, Reserva reserva) {
        autorizacao.validarAcesso(responsavel, "VALIDAR_ALOCACAO_DOCENTE");
        List<Professor> alocados = new ArrayList<>();
        if (reserva.getProfessor() != null) {
            alocados.add(reserva.getProfessor());
        }
        alocados.addAll(reserva.getProfessores());
        if (alocados.isEmpty()) {
            throw new IllegalArgumentException("Professor não encontrado");
        }
        LocalDateTime inicio = reserva.getInicio();
        LocalDateTime fim = reserva.getFim();
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Período é obrigatório");
        }
        for (Professor professor : alocados) {
            if (professor.temConflitoIgnorando(inicio, fim, inicio, fim)) {
                throw new ReservaAgendaProfessorException("Professor indisponível (" + professor.getNome() + ")");
            }
        }
        validadas.add(reserva);
    }

    /** Informa se a alocação da reserva foi validada. */
    public synchronized boolean foiValidada(Reserva reserva) {
        return validadas.contains(reserva);
    }
}
