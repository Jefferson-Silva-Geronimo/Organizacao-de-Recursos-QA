package com.organizacao_de_recursos.controller.api;

import com.organizacao_de_recursos.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/** Relatórios operacionais (RF-21) - Responsável e Administrador. */
@RestController
@RequestMapping("/api/v1/relatorios")
@PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMINISTRADOR')")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/utilizacao")
    public Map<Long, Long> utilizacao(@RequestParam(name = "de") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
                                       @RequestParam(name = "ate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return relatorioService.utilizacaoPorSala(de, ate);
    }

    @GetMapping("/carga-horaria")
    public Map<Long, Double> cargaHoraria(@RequestParam(name = "de") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
                                           @RequestParam(name = "ate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return relatorioService.cargaHorariaPorSala(de, ate);
    }

    @GetMapping("/conflitos-evitados")
    public Map<String, Long> conflitosEvitados(@RequestParam(name = "de") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
                                                @RequestParam(name = "ate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return Map.of("total", relatorioService.conflitosEvitados(de, ate));
    }
}
