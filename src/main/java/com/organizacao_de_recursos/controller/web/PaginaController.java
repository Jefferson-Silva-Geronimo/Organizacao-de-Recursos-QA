package com.organizacao_de_recursos.controller.web;

import com.organizacao_de_recursos.security.UsuarioPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@org.springframework.stereotype.Controller
public class PaginaController {

    @GetMapping("/")
    public RedirectView raiz() {
        return new RedirectView("/painel");
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/painel")
    public String painel(Model model, Authentication authentication) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        model.addAttribute("username", usuario.getUsername());
        model.addAttribute("perfil", usuario.getUsuario().getPerfil().name());
        return "painel";
    }
}
