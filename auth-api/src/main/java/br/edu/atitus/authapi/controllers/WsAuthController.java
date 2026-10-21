package br.edu.atitus.authapi.controllers;

import br.edu.atitus.authapi.dtos.UserResponse;
import br.edu.atitus.authapi.services.AuthorizationService;
import br.edu.atitus.authapi.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Rotas protegidas: a identidade chega nos cabecalhos propagados pelo gateway apos validar o JWT
@RestController
@RequestMapping("/ws/auth")
public class WsAuthController {

    private final UserService service;
    private final AuthorizationService authService;

    public WsAuthController(UserService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    // Qualquer usuario autenticado consulta os proprios dados
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.findById(userId));
    }

    // Somente administradores listam os usuarios
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestHeader("X-User-Type") Integer userType) {
        authService.requireAdmin(userType);
        return ResponseEntity.ok(service.findAll());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.status(401).body("Identidade do usuário não informada");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exceptionHandler(Exception ex) {
        return ResponseEntity.status(500).body("Ops!!! Algo deu errado. - " + ex.getMessage());
    }
}
