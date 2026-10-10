package br.edu.atitus.currencyapi.services;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service("currencyServiceJpa")
public class CurrencyServiceJpa implements CurrencyService {

    private final CurrencyRepository repository;

    public CurrencyServiceJpa(CurrencyRepository repository) {
        this.repository = repository;
    }

    @Value("${server.port:8100}")
    private String serverPort;

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(
            String sourceCurrency,
            String targetCurrency
    ) throws Exception {
        CurrencyEntity entity = repository.findBySourceCurrencyAndTargetCurrency(
                        normalizeCode(sourceCurrency),
                        normalizeCode(targetCurrency))
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));

        String environment = "Currency API running in Port: " + serverPort;

        return new CurrencyResponse(
                entity.getSourceCurrency(),
                entity.getTargetCurrency(),
                entity.getConversionRate(),
                environment
        );
    }

    // Códigos ISO 4217 são armazenados em maiúsculas (CHAR(3)); "usd" e " USD " devem encontrar "USD".
    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}