package com.organizacao_de_recursos.domain.estado;

import com.organizacao_de_recursos.exception.TransicaoInvalidaException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ponto único de decisão sobre transições de estado da reserva (seção 3 do plano de migração),
 * usado pela camada persistida. {@code SOLICITADA}, {@code APROVADA} e {@code EM_USO} ocupam o
 * recurso (D3); os demais estados são finais e liberam.
 */
@Component
public final class MaquinaDeEstados {

    private static final Map<EstadoReserva, Set<EstadoReserva>> TRANSICOES_VALIDAS = new EnumMap<>(EstadoReserva.class);

    private static final Set<EstadoReserva> ESTADOS_QUE_OCUPAM =
            EnumSet.of(EstadoReserva.SOLICITADA, EstadoReserva.APROVADA, EstadoReserva.EM_USO);

    static {
        TRANSICOES_VALIDAS.put(EstadoReserva.SOLICITADA,
                EnumSet.of(EstadoReserva.APROVADA, EstadoReserva.REJEITADA, EstadoReserva.CANCELADA));
        TRANSICOES_VALIDAS.put(EstadoReserva.APROVADA,
                EnumSet.of(EstadoReserva.EM_USO, EstadoReserva.CANCELADA, EstadoReserva.NAO_COMPARECEU));
        TRANSICOES_VALIDAS.put(EstadoReserva.EM_USO, EnumSet.of(EstadoReserva.CONCLUIDA));
        TRANSICOES_VALIDAS.put(EstadoReserva.CONCLUIDA, EnumSet.noneOf(EstadoReserva.class));
        TRANSICOES_VALIDAS.put(EstadoReserva.REJEITADA, EnumSet.noneOf(EstadoReserva.class));
        TRANSICOES_VALIDAS.put(EstadoReserva.CANCELADA, EnumSet.noneOf(EstadoReserva.class));
        TRANSICOES_VALIDAS.put(EstadoReserva.NAO_COMPARECEU, EnumSet.noneOf(EstadoReserva.class));
    }

    public boolean podeTransitar(EstadoReserva de, EstadoReserva para) {
        return de != null && para != null && TRANSICOES_VALIDAS.getOrDefault(de, Set.of()).contains(para);
    }

    /** @throws TransicaoInvalidaException se a transição não é permitida pela máquina de estados */
    public void validarTransicao(EstadoReserva de, EstadoReserva para) {
        if (para == null) {
            throw new TransicaoInvalidaException("Estado é obrigatório");
        }
        if (!podeTransitar(de, para)) {
            throw new TransicaoInvalidaException("Transição de " + de + " para " + para + " não permitida");
        }
    }

    /** Estados que ocupam o recurso (D3): bloqueiam sala/material/agenda do professor. */
    public boolean ocupaRecurso(EstadoReserva estado) {
        return ESTADOS_QUE_OCUPAM.contains(estado);
    }
}
