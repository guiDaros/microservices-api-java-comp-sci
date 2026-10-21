package br.edu.atitus.currencyapi.services;

import br.edu.atitus.currencyapi.clients.BcbClient;
import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sobe o contexto real (aspectos de Cache, Retry e Circuit Breaker) sem PostgreSQL e sem Consul
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "spring.cloud.consul.enabled=false",
        "spring.cloud.consul.config.enabled=false"
})
@AutoConfigureMockMvc
class BcbResilienceTest {

    @MockitoBean
    private BcbClient bcbClient;

    @MockitoBean
    private CurrencyRepository repository;

    @Autowired
    private CurrencyServiceJpa currencyService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("bcb-api").reset();
        cacheManager.getCache("bcb-api").clear();
        when(repository.findBySourceCurrencyAndTargetCurrency("USD", "BRL"))
                .thenReturn(Optional.of(new CurrencyEntity(1L, "USD", "BRL", 5.15)));
    }

    private static BcbCotacaoResponse cotacao(double venda) {
        return new BcbCotacaoResponse(List.of(new BcbCotacaoResponse.Cotacao(venda, venda, "2026-10-09 13:00:00", "Fechamento")));
    }

    @Test
    void resilienceConfigMatchesRequirements() {
        CircuitBreakerConfig cb = circuitBreakerRegistry.circuitBreaker("bcb-api").getCircuitBreakerConfig();
        assertEquals(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED, cb.getSlidingWindowType());
        assertEquals(5, cb.getSlidingWindowSize());
        assertEquals(5, cb.getMinimumNumberOfCalls());
        assertEquals(50f, cb.getFailureRateThreshold());
        assertEquals(100_000L, cb.getWaitIntervalFunctionInOpenState().apply(1));

        RetryConfig retry = retryRegistry.retry("bcb-api").getRetryConfig();
        assertEquals(3, retry.getMaxAttempts());
        assertEquals(1000L, retry.getIntervalBiFunction().apply(1, null));
    }

    @Test
    void retriesThreeTimesThenFallsBackToDatabase() throws Exception {
        when(bcbClient.getCotacao(eq("'USD'"), anyString(), eq("json")))
                .thenThrow(new ResourceAccessException("Read timed out"));

        CurrencyResponse response = currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(5.15, response.conversionRate());
        assertTrue(response.environment().endsWith(" - Source: Local DB"));
        verify(bcbClient, times(3)).getCotacao(eq("'USD'"), anyString(), eq("json"));
    }

    @Test
    void circuitBreakerOpensAndCallNotPermittedIsNotRetried() throws Exception {
        when(bcbClient.getCotacao(eq("'USD'"), anyString(), eq("json")))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // 1a chamada: 3 falhas. 2a chamada: mais 2 falhas -> 5 falhas na janela (100%) -> circuito ABERTO
        currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");
        currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreakerRegistry.circuitBreaker("bcb-api").getState());

        // Com o circuito aberto o BCB nao e chamado (CallNotPermittedException sem retry) e o banco responde
        CurrencyResponse response = currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(5.15, response.conversionRate());
        verify(bcbClient, times(5)).getCotacao(eq("'USD'"), anyString(), eq("json"));
    }

    @Test
    void cachesValidQuote() throws Exception {
        when(bcbClient.getCotacao(eq("'USD'"), anyString(), eq("json"))).thenReturn(cotacao(5.0));

        currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");
        CurrencyResponse response = currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(5.0, response.conversionRate());
        assertTrue(response.environment().endsWith(" - Source: BCB"));
        verify(bcbClient, times(1)).getCotacao(eq("'USD'"), anyString(), eq("json"));
    }

    @Test
    void doesNotCacheResponseWithoutQuote() throws Exception {
        when(bcbClient.getCotacao(eq("'USD'"), anyString(), eq("json")))
                .thenReturn(new BcbCotacaoResponse(List.of()));

        currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");
        CurrencyResponse response = currencyService.findBySourceCurrencyAndTargetCurrency("USD", "BRL");

        assertEquals(5.15, response.conversionRate());
        verify(bcbClient, times(2)).getCotacao(eq("'USD'"), anyString(), eq("json"));
    }

    @Test
    void returns404WhenNoSourceHasQuote() throws Exception {
        when(bcbClient.getCotacao(eq("'XYZ'"), anyString(), eq("json")))
                .thenReturn(new BcbCotacaoResponse(List.of()));
        when(repository.findBySourceCurrencyAndTargetCurrency("XYZ", "BRL")).thenReturn(Optional.empty());

        mockMvc.perform(get("/currencies").param("source", "XYZ").param("target", "BRL"))
                .andExpect(status().isNotFound());
    }
}
