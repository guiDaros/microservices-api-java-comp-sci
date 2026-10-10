package br.edu.atitus.currencyapi.services;

import br.edu.atitus.currencyapi.clients.BcbClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BcbCotacaoService {

    private static final DateTimeFormatter BCB_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final BcbClient bcbClient;

    public BcbCotacaoService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    // Retorna a cotacao de venda (em BRL) da moeda na data, ou null se o BCB nao tiver cotacao.
    // Ordem dos interceptadores: Cache (99) -> Retry (100) -> Circuit Breaker (101) -> chamada HTTP
    // Falhas (excecoes) e respostas sem cotacao (null) nao sao armazenadas no cache.
    @Cacheable(cacheNames = "bcb-api", key = "#moeda + '-' + #data", unless = "#result == null")
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Double getCotacao(String moeda, LocalDate data) {
        var response = bcbClient.getCotacao("'" + moeda + "'", "'" + data.format(BCB_DATE_FORMAT) + "'", "json");
        if (response == null || response.value() == null || response.value().isEmpty()) {
            return null;
        }

        // O ultimo boletim do dia e o mais recente
        Double cotacao = response.value().getLast().cotacaoVenda();
        return (cotacao != null && cotacao > 0) ? cotacao : null;
    }
}
