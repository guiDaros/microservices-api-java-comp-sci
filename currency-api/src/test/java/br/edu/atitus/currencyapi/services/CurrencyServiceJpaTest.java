package br.edu.atitus.currencyapi.services;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CurrencyServiceJpaTest {

    private CurrencyRepository repository;
    private BcbCotacaoService bcbCotacaoService;
    private CurrencyServiceJpa service;

    @BeforeEach
    void setUp() {
        repository = mock(CurrencyRepository.class);
        bcbCotacaoService = mock(BcbCotacaoService.class);
        service = new CurrencyServiceJpa(repository, bcbCotacaoService);
        ReflectionTestUtils.setField(service, "serverPort", "8100");
        ReflectionTestUtils.setField(service, "dataCotacao", "");
    }

    @Test
    void usesConfiguredQuoteDateWhenSet() throws Exception {
        ReflectionTestUtils.setField(service, "dataCotacao", "2026-10-09");
        when(bcbCotacaoService.getCotacao("USD", LocalDate.of(2026, 10, 9))).thenReturn(4.9892);

        CurrencyResponse response = service.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(4.9892, response.conversionRate());
    }

    @Test
    void usesBcbRateWhenAvailable() throws Exception {
        when(bcbCotacaoService.getCotacao(eq("USD"), any())).thenReturn(5.0);

        CurrencyResponse response = service.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals("USD", response.sourceCurrency());
        assertEquals("BRL", response.targetCurrency());
        assertEquals(5.0, response.conversionRate());
        assertEquals("Currency API running in Port: 8100 - Source: BCB", response.environment());
        verifyNoInteractions(repository);
    }

    @Test
    void convertsBetweenTwoForeignCurrenciesUsingBrlQuotes() throws Exception {
        when(bcbCotacaoService.getCotacao(eq("USD"), any())).thenReturn(5.0);
        when(bcbCotacaoService.getCotacao(eq("EUR"), any())).thenReturn(6.25);

        CurrencyResponse response = service.findBySourceCurrencyAndTargetCurrency("USD", "EUR");

        assertEquals(0.8, response.conversionRate(), 0.0001);
    }

    @Test
    void fallsBackToDatabaseWhenBcbFails() throws Exception {
        when(bcbCotacaoService.getCotacao(eq("USD"), any())).thenThrow(new RuntimeException("timeout"));
        when(repository.findBySourceCurrencyAndTargetCurrency("USD", "BRL"))
                .thenReturn(Optional.of(new CurrencyEntity(1L, "USD", "BRL", 5.15)));

        CurrencyResponse response = service.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(5.15, response.conversionRate());
        assertTrue(response.environment().startsWith("Currency API running in Port: "));
        assertTrue(response.environment().endsWith(" - Source: Local DB"));
    }

    @Test
    void fallsBackToDatabaseWhenBcbHasNoQuote() throws Exception {
        when(bcbCotacaoService.getCotacao(eq("USD"), any())).thenReturn(null);
        when(repository.findBySourceCurrencyAndTargetCurrency("USD", "BRL"))
                .thenReturn(Optional.of(new CurrencyEntity(1L, "USD", "BRL", 5.15)));

        CurrencyResponse response = service.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(5.15, response.conversionRate());
    }

    @Test
    void throwsEntityNotFoundExceptionWhenNoSourceHasQuote() {
        when(bcbCotacaoService.getCotacao(eq("USD"), any())).thenReturn(null);
        when(repository.findBySourceCurrencyAndTargetCurrency("USD", "EUR"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.findBySourceCurrencyAndTargetCurrency("USD", "EUR")
        );

        assertEquals("Cotação não encontrada", exception.getMessage());
    }
}
