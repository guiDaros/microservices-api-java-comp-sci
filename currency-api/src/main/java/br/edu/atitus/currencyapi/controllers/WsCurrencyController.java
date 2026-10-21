package br.edu.atitus.currencyapi.controllers;

import br.edu.atitus.currencyapi.dtos.CurrencyRequest;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.services.AuthorizationService;
import br.edu.atitus.currencyapi.services.CurrencyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Rotas protegidas: o gateway ja validou o JWT e propagou X-User-Id, X-User-Email e X-User-Type
@RestController
@RequestMapping("/ws/currencies")
public class WsCurrencyController {

    private final CurrencyService service;
    private final AuthorizationService authService;

    public WsCurrencyController(@Qualifier("currencyServiceJpa") CurrencyService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    // Somente administradores cadastram cotacoes
    @PostMapping
    public ResponseEntity<CurrencyEntity> postCurrency(
            @RequestBody CurrencyRequest dto,
            @RequestHeader("X-User-Type") Integer userType) throws Exception {

        authService.requireAdmin(userType);

        return ResponseEntity.status(201).body(service.save(dto));
    }

    // Requisicao sem a identidade propagada pelo gateway (ex.: acesso direto ao servico)
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
        return ResponseEntity.status(500).body("Ops!!! Algo deu errado.");
    }
}
