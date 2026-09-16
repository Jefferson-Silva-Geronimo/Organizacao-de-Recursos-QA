package com.organizacao_de_recursos.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Validador para RN-04: Unicidade Sob Concorrência
 * Duas solicitações simultâneas para o mesmo recurso e período 
 * devem produzir somente uma reserva aceita.
 */
public class ValidadorConcorrencia {
    private final ConcurrentHashMap<String, AtomicBoolean> locks = new ConcurrentHashMap<>();

    /**
     * Processa uma reserva simultânea com garantia de atomicidade
     *
     * @param reserva Reserva a processar
     * @return true se reserva foi aceita, false se recusada
     */
    public boolean procesarReservaSimultanea(Reserva reserva) {
        String chave = gerarChaveRecursoTempo(reserva);
        
        // Tentar adquirir o lock para este recurso+tempo
        AtomicBoolean lock = locks.computeIfAbsent(chave, k -> new AtomicBoolean(false));
        
        // Apenas uma thread conseguirá mudar de false para true
        return lock.compareAndSet(false, true);
    }

    private String gerarChaveRecursoTempo(Reserva reserva) {
        if (reserva.getRecurso() == null) {
            return "null";
        }
        return "R:" + reserva.getRecurso().getId() + 
               ":I:" + reserva.getInicio() + 
               ":F:" + reserva.getFim();
    }

    public int processarTriplaSimultanea(Reserva r1, Reserva r2, Reserva r3) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
        return 0;
    }

    public boolean processarDuplaPeriodosAdjacentes(Reserva r1, Reserva r2) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
        return false;
    }

    public void processarReservaComRestricao(Reserva rRestrita, Reserva rComum) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
    }

    public boolean verificarConsistenciaAposConcorrencia(Reserva r1, Reserva r2) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
        return false;
    }

    public boolean validarSequenciaAuditoriaConcorrente(Reserva r1, Reserva r2, ValidadorAuditoria validadorAuditoria) {
        // Assinatura mínima sem regra de negócio (Fase RED TDD)
        return false;
    }
}
