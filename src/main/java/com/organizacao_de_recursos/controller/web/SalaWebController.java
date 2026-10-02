package com.organizacao_de_recursos.controller.web;

import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.repository.SalaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Cadastro de salas (Onda 4) - Administrador. */
@Controller
@RequestMapping("/salas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SalaWebController {

    private final SalaRepository salaRepository;

    public SalaWebController(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("salas", salaRepository.findAll());
        return "salas/lista";
    }

    @PostMapping
    public String criar(@RequestParam(name = "nome") String nome,
                         @RequestParam(name = "restrito", defaultValue = "false") boolean restrito) {
        salaRepository.save(new SalaEntity(nome, restrito, null));
        return "redirect:/salas";
    }
}
