package br.edu.atitus.authapi.dtos;

import br.edu.atitus.authapi.entities.UserEntity;
import br.edu.atitus.authapi.entities.UserType;

// Nunca expoe a senha (nem o hash) nas respostas
public record UserResponse(
        Long id,
        String name,
        String email,
        UserType type
) {
    public static UserResponse fromEntity(UserEntity entity) {
        return new UserResponse(entity.getId(), entity.getName(), entity.getEmail(), entity.getType());
    }
}
