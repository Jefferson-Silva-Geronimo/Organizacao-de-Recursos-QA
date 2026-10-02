package com.organizacao_de_recursos.security;

import com.organizacao_de_recursos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos"));
    }
}
