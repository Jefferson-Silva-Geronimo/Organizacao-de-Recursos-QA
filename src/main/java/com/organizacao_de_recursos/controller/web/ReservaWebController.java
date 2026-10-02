package com.organizacao_de_recursos.controller.web;

import com.organizacao_de_recursos.domain.TradutorErros;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.security.UsuarioPrincipal;
import com.organizacao_de_recursos.service.ReservaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Páginas de reserva (Onda 4) - reutiliza o mesmo {@link ReservaService} da API REST. */
@Controller
@RequestMapping("/reservas")
public class ReservaWebController {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ReservaService reservaService;
    private final SalaRepository salaRepository;
    private final TradutorErros tradutorErros = new TradutorErros();

    public ReservaWebController(ReservaService reservaService, SalaRepository salaRepository) {
        this.reservaService = reservaService;
        this.salaRepository = salaRepository;
    }

    @GetMapping("/nova")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public String formNova(Model model) {
        model.addAttribute("salas", salaRepository.findAll());
        return "reservas/nova";
    }

    @PostMapping("/nova")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public String criar(@RequestParam(name = "salaId") Long salaId,
                         @RequestParam(name = "inicio") String inicio,
                         @RequestParam(name = "fim") String fim,
                         Authentication authentication, RedirectAttributes redirectAttributes) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        try {
            var inicioOffset = LocalDateTime.parse(inicio).atZone(FUSO).toOffsetDateTime();
            var fimOffset = LocalDateTime.parse(fim).atZone(FUSO).toOffsetDateTime();
            reservaService.criar(usuario.getId(), salaId, inicioOffset, fimOffset);
            redirectAttributes.addFlashAttribute("sucesso", "Reserva criada com sucesso.");
        } catch (RuntimeException erro) {
            redirectAttributes.addFlashAttribute("erro", tradutorErros.mensagemParaUsuario(erro));
            return "redirect:/reservas/nova";
        }
        return "redirect:/reservas/minhas";
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public String minhas(Model model, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        model.addAttribute("reservas", reservaService.listarDoSolicitante(usuario.getId()));
        return "reservas/minhas";
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@autorizacaoReserva.podeCancelar(#id, authentication)")
    public String cancelar(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        try {
            reservaService.cancelar(id, usuario.getId());
            redirectAttributes.addFlashAttribute("sucesso", "Reserva cancelada.");
        } catch (RuntimeException erro) {
            redirectAttributes.addFlashAttribute("erro", tradutorErros.mensagemParaUsuario(erro));
        }
        return "redirect:/reservas/minhas";
    }

    @GetMapping("/aprovacao")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public String fila(Model model, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        model.addAttribute("reservas", reservaService.listarNoEscopoDoResponsavel(usuario.getId()));
        return "reservas/aprovacao";
    }

    @PostMapping("/{id}/aprovar")
    @PreAuthorize("@autorizacaoReserva.podeAprovarOuRejeitar(#id, authentication)")
    public String aprovar(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        try {
            reservaService.aprovar(id, usuario.getId());
            redirectAttributes.addFlashAttribute("sucesso", "Reserva aprovada.");
        } catch (RuntimeException erro) {
            redirectAttributes.addFlashAttribute("erro", tradutorErros.mensagemParaUsuario(erro));
        }
        return "redirect:/reservas/aprovacao";
    }

    @PostMapping("/{id}/rejeitar")
    @PreAuthorize("@autorizacaoReserva.podeAprovarOuRejeitar(#id, authentication)")
    public String rejeitar(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        try {
            reservaService.rejeitar(id, usuario.getId());
            redirectAttributes.addFlashAttribute("sucesso", "Reserva rejeitada.");
        } catch (RuntimeException erro) {
            redirectAttributes.addFlashAttribute("erro", tradutorErros.mensagemParaUsuario(erro));
        }
        return "redirect:/reservas/aprovacao";
    }
}
