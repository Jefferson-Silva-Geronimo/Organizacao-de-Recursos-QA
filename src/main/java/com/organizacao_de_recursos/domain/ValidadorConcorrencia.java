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
        int aceitas = 0;
        if (procesarReservaSimultanea(r1)) aceitas++;
        if (procesarReservaSimultanea(r2)) aceitas++;
        if (procesarReservaSimultanea(r3)) aceitas++;
        return aceitas;
    }

    public boolean processarDuplaPeriodosAdjacentes(Reserva r1, Reserva r2) {
        boolean adjacentes = r1.getFim().equals(r2.getInicio()) || r2.getFim().equals(r1.getInicio());
        return adjacentes && procesarReservaSimultanea(r1) && procesarReservaSimultanea(r2);
    }

    public void processarReservaComRestricao(Reserva rRestrita, Reserva rComum) {
        procesarReservaSimultanea(rRestrita);
        procesarReservaSimultanea(rComum);
    }

    public boolean verificarConsistenciaAposConcorrencia(Reserva r1, Reserva r2) {
        boolean primeira = procesarReservaSimultanea(r1);
        boolean segunda = procesarReservaSimultanea(r2);
        return primeira ^ segunda;
    }

    public boolean validarSequenciaAuditoriaConcorrente(Reserva r1, Reserva r2, ValidadorAuditoria validadorAuditoria) {
        boolean primeira = procesarReservaSimultanea(r1);
        boolean segunda = procesarReservaSimultanea(r2);
        return primeira ^ segunda;
    }
}
