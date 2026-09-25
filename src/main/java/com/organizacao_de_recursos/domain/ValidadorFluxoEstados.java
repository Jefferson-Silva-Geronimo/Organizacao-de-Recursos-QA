package com.organizacao_de_recursos.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validador para RN-07: Fluxo de Estados
 * Fluxo principal: SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA
 * Estados alternativos: REJEITADA, CANCELADA, NAO_COMPARECEU
 */
public class ValidadorFluxoEstados {
    private static final Set<String> ESTADOS_VALIDOS = new HashSet<>();
    private static final Map<String, Set<String>> TRANSICOES_VALIDAS = new HashMap<>();

    static {
        // Estados válidos
        ESTADOS_VALIDOS.add("SOLICITADA");
        ESTADOS_VALIDOS.add("APROVADA");
        ESTADOS_VALIDOS.add("EM_USO");
        ESTADOS_VALIDOS.add("CONCLUIDA");
        ESTADOS_VALIDOS.add("REJEITADA");
        ESTADOS_VALIDOS.add("CANCELADA");
        ESTADOS_VALIDOS.add("NAO_COMPARECEU");

        // Transições válidas
        Set<String> deSolicitada = new HashSet<>();
        deSolicitada.add("APROVADA");
        deSolicitada.add("REJEITADA");
        deSolicitada.add("CANCELADA");
        TRANSICOES_VALIDAS.put("SOLICITADA", deSolicitada);

        Set<String> deAprovada = new HashSet<>();
        deAprovada.add("EM_USO");
        deAprovada.add("CANCELADA");
        TRANSICOES_VALIDAS.put("APROVADA", deAprovada);

        Set<String> deEmUso = new HashSet<>();
        deEmUso.add("CONCLUIDA");
        deEmUso.add("NAO_COMPARECEU");
        TRANSICOES_VALIDAS.put("EM_USO", deEmUso);

        Set<String> deConcluida = new HashSet<>();
        TRANSICOES_VALIDAS.put("CONCLUIDA", deConcluida);

        Set<String> deRejeitada = new HashSet<>();
        TRANSICOES_VALIDAS.put("REJEITADA", deRejeitada);

        Set<String> deCancelada = new HashSet<>();
        TRANSICOES_VALIDAS.put("CANCELADA", deCancelada);

        Set<String> deNaoCompareceu = new HashSet<>();
        TRANSICOES_VALIDAS.put("NAO_COMPARECEU", deNaoCompareceu);
    }

    /**
     * Valida uma transição de estado
     *
     * @param reserva Reserva
     * @param novoEstado Novo estado desejado
     * @throws ReservaFluxoEstadosException se transição não é válida
     */
    public void validarTransicao(Reserva reserva, String novoEstado) {
        String estadoAtual = reserva.getEstado();

        // O novo estado é obrigatório (null ou vazio)
        if (novoEstado == null || novoEstado.isBlank()) {
            throw new ReservaFluxoEstadosException("Estado é obrigatório");
        }

        // Verificar se estado é válido
        if (!ESTADOS_VALIDOS.contains(novoEstado)) {
            throw new ReservaFluxoEstadosException("Estado inválido");
        }

        // Verificar se transição é válida
        Set<String> transicoesValidas = TRANSICOES_VALIDAS.get(estadoAtual);
        if (transicoesValidas == null || !transicoesValidas.contains(novoEstado)) {
            // Validação específica: não pode cancelar EM_USO
            if ("EM_USO".equals(estadoAtual) && "CANCELADA".equals(novoEstado)) {
                throw new ReservaFluxoEstadosException("Reserva iniciada não pode ser cancelada");
            }
            throw new ReservaFluxoEstadosException("Transição não permitida");
        }
    }
}
