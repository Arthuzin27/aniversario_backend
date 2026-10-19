package com.convite.evelyn.security;

import com.convite.evelyn.config.AppProperties;
import com.convite.evelyn.entity.Administrador;
import com.convite.evelyn.repository.AdministradorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Cria o primeiro administrador a partir de ADMIN_USERNAME / ADMIN_PASSWORD na inicialização.
 * Se o usuário já existir, nada muda — a menos que ADMIN_RESET_PASSWORD=true, que redefine a senha.
 * A senha nunca é registrada em log e é armazenada apenas como hash BCrypt.
 */
@Component
public class AdminInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);
    static final int TAMANHO_MINIMO_SENHA = 10;

    private final AdministradorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public AdminInicializador(AdministradorRepository repository, PasswordEncoder passwordEncoder,
                              AppProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String usuario = properties.admin().usuario();
        String senha = properties.admin().senha();

        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            if (repository.count() == 0) {
                log.warn("Nenhum administrador cadastrado. Defina ADMIN_USERNAME e ADMIN_PASSWORD e reinicie o backend.");
            }
            return;
        }
        usuario = usuario.strip();
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }

        Optional<Administrador> existente = repository.findByUsuario(usuario);
        if (existente.isEmpty()) {
            repository.save(new Administrador(usuario, passwordEncoder.encode(senha)));
            log.info("Administrador inicial '{}' criado.", usuario);
        } else if (properties.admin().redefinirSenha()) {
            existente.get().setSenhaHash(passwordEncoder.encode(senha));
            log.info("Senha do administrador '{}' redefinida. Volte ADMIN_RESET_PASSWORD para false.", usuario);
        }
    }
}
