package br.edu.atitus.currencyapi.services;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrencyServiceJpaTest {

    @Test
    void returnsCurrencyResponseWhenCurrencyExists() throws Exception {
        CurrencyRepository repository = mock(CurrencyRepository.class);
        CurrencyServiceJpa service = new CurrencyServiceJpa(repository);
        ReflectionTestUtils.setField(service, "serverPort", "8100");
        CurrencyEntity currency = new CurrencyEntity(1L, "USD", "BRL", 5.15);
        when(repository.findBySourceCurrencyAndTargetCurrency("USD", "BRL"))
                .thenReturn(Optional.of(currency));

        CurrencyResponse response = service.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals("USD", response.sourceCurrency());
        assertEquals("BRL", response.targetCurrency());
        assertEquals(5.15, response.conversionRate());
        assertTrue(response.environment().startsWith("Currency API running in Port: "));
    }

    @Test
    void throwsEntityNotFoundExceptionWhenCurrencyDoesNotExist() {
        CurrencyRepository repository = mock(CurrencyRepository.class);
        CurrencyServiceJpa service = new CurrencyServiceJpa(repository);
        when(repository.findBySourceCurrencyAndTargetCurrency("USD", "EUR"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.findBySourceCurrencyAndTargetCurrency("USD", "EUR")
        );

        assertEquals("Cotação não encontrada", exception.getMessage());
    }
}