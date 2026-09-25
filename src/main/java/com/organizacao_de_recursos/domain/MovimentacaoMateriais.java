package com.organizacao_de_recursos.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Retirada e devolução de materiais pelo Responsável (RF-16 e RF-17).
 *
 * Exige a permissão REGISTRAR_MOVIMENTACAO_MATERIAIS (ADR-003). O material deve estar associado à reserva
 * e a devolução exige retirada correspondente.
 *
 * Os registros ficam em memória apenas para aplicar essas regras; a persistência realista da movimentação
 * (RNF-07) segue pendente e não é atendida por este componente.
 */
public class MovimentacaoMateriais {
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();
    private final Set<String> retiradas = new HashSet<>();
    private final Set<String> devolucoes = new HashSet<>();

    /** Registra a retirada do material associado à reserva (somente Responsável). */
    public synchronized void registrarRetirada(Usuario usuario, Reserva reserva, Recurso material) {
        autorizacao.validarAcesso(usuario, "REGISTRAR_MOVIMENTACAO_MATERIAIS");
        validarMaterialDaReserva(reserva, material);
        retiradas.add(chave(reserva, material));
    }

    /** Registra a devolução do material; exige retirada correspondente (somente Responsável). */
    public synchronized void registrarDevolucao(Usuario usuario, Reserva reserva, Recurso material) {
        autorizacao.validarAcesso(usuario, "REGISTRAR_MOVIMENTACAO_MATERIAIS");
        validarMaterialDaReserva(reserva, material);
        if (!retiradas.contains(chave(reserva, material))) {
            throw new ReservaMovimentacaoException("Não há retirada registrada para este material; registre a retirada antes da devolução");
        }
        devolucoes.add(chave(reserva, material));
    }

    private void validarMaterialDaReserva(Reserva reserva, Recurso material) {
        if (reserva == null || material == null || !reserva.getMateriais().contains(material)) {
            throw new ReservaMovimentacaoException("Material não associado à reserva");
        }
    }

    private String chave(Reserva reserva, Recurso material) {
        return reserva.getId() + ":" + material.getId();
    }
}
