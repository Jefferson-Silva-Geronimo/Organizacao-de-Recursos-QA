package com.organizacao_de_recursos.security;

import com.organizacao_de_recursos.model.UsuarioEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/** Adapta {@link UsuarioEntity} para o contrato do Spring Security; perfil vira a authority ROLE_{perfil}. */
public class UsuarioPrincipal implements UserDetails {

    private final UsuarioEntity usuario;

    public UsuarioPrincipal(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public Long getId() {
        return usuario.getId();
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo();
    }
}
