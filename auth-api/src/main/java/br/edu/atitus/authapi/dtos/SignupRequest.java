package br.edu.atitus.authapi.dtos;

public record SignupRequest(
        String name,
        String email,
        String password
) {
}
