package com.organizacao_de_recursos.service;

import com.organizacao_de_recursos.model.ReservaEntity;
import com.organizacao_de_recursos.model.ReservaRecursoEntity;
import com.organizacao_de_recursos.model.TipoRecursoReserva;
import com.organizacao_de_recursos.repository.ConflitoEvitadoRepository;
import com.organizacao_de_recursos.repository.ReservaRecursoRepository;
import com.organizacao_de_recursos.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Relatórios operacionais (RF-21): utilização, carga horária e conflitos evitados por sala,
 * calculados a partir dos dados reais persistidos (substitui o stub de autorização do domínio
 * puro, {@code ServicoRelatorios}, que nunca teve o conteúdo implementado).
 */
@Service
public class RelatorioService {

    private final ReservaRepository reservaRepository;
    private final ReservaRecursoRepository reservaRecursoRepository;
    private final ConflitoEvitadoRepository conflitoEvitadoRepository;

    public RelatorioService(ReservaRepository reservaRepository,
                             ReservaRecursoRepository reservaRecursoRepository,
                             ConflitoEvitadoRepository conflitoEvitadoRepository) {
        this.reservaRepository = reservaRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
        this.conflitoEvitadoRepository = conflitoEvitadoRepository;
    }

    /** Número de reservas de sala iniciadas no período, por sala. */
    public Map<Long, Long> utilizacaoPorSala(OffsetDateTime de, OffsetDateTime ate) {
        return recursosDeSalaNoPeriodo(de, ate).stream()
                .collect(Collectors.groupingBy(ReservaRecursoEntity::getRecursoId, Collectors.counting()));
    }

    /** Soma de horas reservadas no período, por sala. */
    public Map<Long, Double> cargaHorariaPorSala(OffsetDateTime de, OffsetDateTime ate) {
        return recursosDeSalaNoPeriodo(de, ate).stream()
                .collect(Collectors.groupingBy(ReservaRecursoEntity::getRecursoId,
                        Collectors.summingDouble(rr -> Duration.between(rr.getInicio(), rr.getFim()).toMinutes() / 60.0)));
    }

    /** Quantidade de tentativas de dupla-reserva recusadas pela constraint de exclusão no período (RN-04). */
    public long conflitosEvitados(OffsetDateTime de, OffsetDateTime ate) {
        return conflitoEvitadoRepository.countByTentadoEmBetween(de, ate);
    }

    private List<ReservaRecursoEntity> recursosDeSalaNoPeriodo(OffsetDateTime de, OffsetDateTime ate) {
        List<ReservaEntity> reservas = reservaRepository.findByInicioGreaterThanEqualAndFimLessThanEqual(de, ate);
        return reservas.stream()
                .flatMap(reserva -> reservaRecursoRepository.findByReservaId(reserva.getId()).stream())
                .filter(recurso -> recurso.getTipoRecurso() == TipoRecursoReserva.SALA)
                .toList();
    }
}
