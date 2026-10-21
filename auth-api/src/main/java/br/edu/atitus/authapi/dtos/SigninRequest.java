package br.edu.atitus.authapi.dtos;

public record SigninRequest(
        String email,
        String password
) {
}
