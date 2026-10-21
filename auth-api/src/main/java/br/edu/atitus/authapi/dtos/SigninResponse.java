package br.edu.atitus.authapi.dtos;

public record SigninResponse(
        UserResponse user,
        String token
) {
}
