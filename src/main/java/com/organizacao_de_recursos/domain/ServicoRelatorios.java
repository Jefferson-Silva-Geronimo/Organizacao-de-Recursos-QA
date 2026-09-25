package com.organizacao_de_recursos.domain;

/**
 * Relatórios operacionais (RF-21).
 *
 * Somente a autorização está implementada (permissão CONSULTAR_RELATORIOS, Administrador; ADR-003).
 * O conteúdo do relatório (utilização, carga horária e conflitos evitados) depende de fórmulas
 * ainda pendentes (arquitetura, §38) e não foi implementado.
 */
public class ServicoRelatorios {
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();

    /** Consulta o relatório operacional (somente Administrador). */
    public void consultarRelatorio(Usuario usuario) {
        autorizacao.validarAcesso(usuario, "CONSULTAR_RELATORIOS");
    }
}
