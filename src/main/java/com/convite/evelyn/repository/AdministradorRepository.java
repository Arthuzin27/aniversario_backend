package com.convite.evelyn.repository;

import com.convite.evelyn.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByUsuario(String usuario);
}
