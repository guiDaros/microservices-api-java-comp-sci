package br.edu.atitus.productapi.servicesimpl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Nao valida JWT (isso e feito pelo gateway); apenas aplica a regra de permissao sobre a identidade propagada
@Service
public class AuthorizationService {

    // Tipo 0 = Admin
    public void requireAdmin(Integer userType) {
        if (userType == null || userType != 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação");
    }
}
