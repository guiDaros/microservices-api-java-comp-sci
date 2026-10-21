package br.edu.atitus.productapi.controllers;

import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.services.ProductService;
import br.edu.atitus.productapi.servicesimpl.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Rotas protegidas: o gateway ja validou o JWT e propagou X-User-Id, X-User-Email e X-User-Type
@RestController
@RequestMapping("/ws/products")
public class WsProductController {
    private final ProductService service;
    private final AuthorizationService authService;

    public WsProductController(@Qualifier("productServiceJpa") ProductService service, AuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ProductEntity> postProduct(
            @RequestBody ProductRequest dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Type") Integer userType) throws Exception {

        // Valida se o usuario e administrador
        authService.requireAdmin(userType);

        var product = service.save(dto);
        return ResponseEntity.status(201).body(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductEntity> putProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest dto,
            @RequestHeader("X-User-Type") Integer userType) throws Exception {

        authService.requireAdmin(userType);

        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("X-User-Type") Integer userType) throws Exception {

        authService.requireAdmin(userType);

        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Requisicao sem a identidade propagada pelo gateway (ex.: acesso direto ao servico)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.status(401).body("Identidade do usuário não informada");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> entityNotFoundExceptionHandler(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
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
