package com.organizacao_de_recursos.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro e consulta de salas, materiais e professores (RF-02, RF-03, RF-04 e RF-08).
 *
 * Cadastrar exige a permissão GERENCIAR_RECURSOS (Administrador); consultar exige a permissão
 * CONSULTAR_DISPONIBILIDADE (qualquer perfil autenticado e ativo). A autorização é aplicada
 * na camada de serviço (ADR-003).
 *
 * Os cadastros ficam em memória: a persistência real dos recursos não faz parte deste componente.
 */
public class CadastroRecursos {
    private final ValidadorAutorizacao autorizacao = new ValidadorAutorizacao();
    private final List<Recurso> recursos = new ArrayList<>();
    private final List<Professor> professores = new ArrayList<>();

    /** Cadastra uma sala ou um material (somente Administrador). */
    public synchronized void cadastrarRecurso(Usuario usuario, Recurso recurso) {
        autorizacao.validarAcesso(usuario, "GERENCIAR_RECURSOS");
        if (recurso == null) {
            throw new IllegalArgumentException("Recurso é obrigatório");
        }
        recursos.add(recurso);
    }

    /** Consulta os recursos cadastrados do tipo informado. */
    public synchronized List<Recurso> consultarRecursos(Usuario usuario, Recurso.TipoRecurso tipo) {
        autorizacao.validarAcesso(usuario, "CONSULTAR_DISPONIBILIDADE");
        return recursos.stream().filter(recurso -> recurso.getTipo() == tipo).toList();
    }

    /** Cadastra um professor (somente Administrador). */
    public synchronized void cadastrarProfessor(Usuario usuario, Professor professor) {
        autorizacao.validarAcesso(usuario, "GERENCIAR_RECURSOS");
        if (professor == null) {
            throw new IllegalArgumentException("Professor é obrigatório");
        }
        professores.add(professor);
    }

    /** Consulta os professores cadastrados. */
    public synchronized List<Professor> consultarProfessores(Usuario usuario) {
        autorizacao.validarAcesso(usuario, "CONSULTAR_DISPONIBILIDADE");
        return List.copyOf(professores);
    }
}
