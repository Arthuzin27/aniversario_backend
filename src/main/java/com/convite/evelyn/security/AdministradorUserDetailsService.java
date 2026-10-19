package com.convite.evelyn.security;

import com.convite.evelyn.repository.AdministradorRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdministradorUserDetailsService implements UserDetailsService {

    private final AdministradorRepository repository;

    public AdministradorUserDetailsService(AdministradorRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) {
        return repository.findByUsuario(usuario)
                .map(a -> User.withUsername(a.getUsuario()).password(a.getSenhaHash()).roles("ADMIN").build())
                .orElseThrow(() -> new UsernameNotFoundException("Administrador não encontrado"));
    }
}
