package com.organizacao_de_recursos.service;

import com.organizacao_de_recursos.model.ConflitoEvitadoEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import com.organizacao_de_recursos.repository.ConflitoEvitadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra uma tentativa recusada em transação própria (REQUIRES_NEW): a transação da criação
 * da reserva já foi abortada pela violação da constraint de exclusão no banco, então este
 * registro precisa sobreviver ao rollback que vem a seguir.
 */
@Service
public class ConflitoEvitadoService {

    private final ConflitoEvitadoRepository conflitoEvitadoRepository;

    public ConflitoEvitadoService(ConflitoEvitadoRepository conflitoEvitadoRepository) {
        this.conflitoEvitadoRepository = conflitoEvitadoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(TipoRecursoReserva tipoRecurso, Long recursoId, Long solicitanteId) {
        conflitoEvitadoRepository.save(new ConflitoEvitadoEntity(tipoRecurso, recursoId, solicitanteId));
    }
}
