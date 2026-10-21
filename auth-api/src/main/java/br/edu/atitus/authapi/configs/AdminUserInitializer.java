package br.edu.atitus.authapi.configs;

import br.edu.atitus.authapi.entities.UserEntity;
import br.edu.atitus.authapi.entities.UserType;
import br.edu.atitus.authapi.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Cria o primeiro administrador na primeira execucao (credenciais exclusivamente didaticas)
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@admin.dev";
    private static final String ADMIN_PASSWORD = "teste";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserInitializer(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        repository.save(new UserEntity(null, "Administrator", ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_PASSWORD), UserType.Admin));
    }
}
