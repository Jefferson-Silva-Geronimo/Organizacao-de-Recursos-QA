package com.organizacao_de_recursos.domain;

import java.util.List;

/**
 * Contrato mínimo (fase RED, RF-02, RF-03, RF-04 e RF-08): cadastro e consulta de salas, materiais e professores.
 * Sem lógica de negócio: nada é registrado e nenhuma autorização é aplicada. Implementação pendente.
 */
public class CadastroRecursos {

    /** Cadastra uma sala ou um material (somente Administrador). */
    public void cadastrarRecurso(Usuario usuario, Recurso recurso) {
        // não implementado
    }

    /** Consulta os recursos cadastrados do tipo informado. */
    public List<Recurso> consultarRecursos(Usuario usuario, Recurso.TipoRecurso tipo) {
        return List.of();
    }

    /** Cadastra um professor (somente Administrador). */
    public void cadastrarProfessor(Usuario usuario, Professor professor) {
        // não implementado
    }

    /** Consulta os professores cadastrados. */
    public List<Professor> consultarProfessores(Usuario usuario) {
        return List.of();
    }
}
