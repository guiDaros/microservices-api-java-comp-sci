package br.edu.atitus.authapi.services;

import br.edu.atitus.authapi.components.JwtUtil;
import br.edu.atitus.authapi.dtos.SigninRequest;
import br.edu.atitus.authapi.dtos.SigninResponse;
import br.edu.atitus.authapi.dtos.SignupRequest;
import br.edu.atitus.authapi.dtos.UserResponse;
import br.edu.atitus.authapi.entities.UserEntity;
import br.edu.atitus.authapi.entities.UserType;
import br.edu.atitus.authapi.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Cadastro publico: sempre cria usuario comum (nunca permite se auto-promover a Admin)
    public UserResponse signup(SignupRequest request) {
        if (request == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requisição inválida");
        if (request.name() == null || request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome inválido");
        if (request.email() == null || !request.email().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail inválido");
        if (request.password() == null || request.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter ao menos 6 caracteres");

        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");

        var user = new UserEntity(null, request.name().trim(), email,
                passwordEncoder.encode(request.password()), UserType.Common);
        return UserResponse.fromEntity(repository.save(user));
    }

    // Valida as credenciais e emite o JWT. A mensagem e a mesma para e-mail ou senha incorretos.
    public SigninResponse signin(SigninRequest request) {
        if (request == null || request.email() == null || request.password() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe e-mail e senha");

        var user = repository.findByEmail(request.email().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        return new SigninResponse(UserResponse.fromEntity(user), jwtUtil.generateToken(user));
    }

    public UserResponse findById(Long id) {
        return repository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(UserResponse::fromEntity).toList();
    }
}
