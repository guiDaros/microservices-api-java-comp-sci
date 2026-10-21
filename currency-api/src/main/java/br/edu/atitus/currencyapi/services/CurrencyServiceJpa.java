package br.edu.atitus.currencyapi.services;

import br.edu.atitus.currencyapi.dtos.CurrencyRequest;
import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service("currencyServiceJpa")
public class CurrencyServiceJpa implements CurrencyService {

    private final CurrencyRepository repository;
    private final BcbCotacaoService bcbCotacaoService;

    public CurrencyServiceJpa(CurrencyRepository repository, BcbCotacaoService bcbCotacaoService) {
        this.repository = repository;
        this.bcbCotacaoService = bcbCotacaoService;
    }

    @Value("${server.port:8100}")
    private String serverPort;

    // Data fixa (yyyy-MM-dd) para consultar o BCB; vazia = data atual
    @Value("${app.bcb-api.data-cotacao:}")
    private String dataCotacao;

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(
            String sourceCurrency,
            String targetCurrency
    ) throws Exception {
        String environment = "Currency API running in Port: " + serverPort;

        Double bcbRate = findRateInBcb(sourceCurrency, targetCurrency);
        if (bcbRate != null) {
            return new CurrencyResponse(sourceCurrency, targetCurrency, bcbRate, environment + " - Source: BCB");
        }

        // Fallback: se o BCB falhar ou nao tiver cotacao, usa o banco local
        CurrencyEntity entity = repository.findBySourceCurrencyAndTargetCurrency(sourceCurrency, targetCurrency)
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));

        return new CurrencyResponse(
                entity.getSourceCurrency(),
                entity.getTargetCurrency(),
                entity.getConversionRate(),
                environment + " - Source: Local DB"
        );
    }

    // Cadastra uma cotacao no banco local (usada como fallback quando o BCB nao responde)
    @Override
    public CurrencyEntity save(CurrencyRequest request) throws Exception {
        if (request == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requisição inválida");
        if (request.sourceCurrency() == null || !request.sourceCurrency().trim().matches("[A-Za-z]{3}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moeda de origem inválida");
        if (request.targetCurrency() == null || !request.targetCurrency().trim().matches("[A-Za-z]{3}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moeda de destino inválida");
        if (request.conversionRate() == null || request.conversionRate() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxa de conversão inválida");

        String source = request.sourceCurrency().trim().toUpperCase();
        String target = request.targetCurrency().trim().toUpperCase();
        if (repository.findBySourceCurrencyAndTargetCurrency(source, target).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cotação já cadastrada");

        return repository.save(new CurrencyEntity(null, source, target, request.conversionRate()));
    }

    // O BCB cota as moedas em BRL, entao source -> target = cotacao(source) / cotacao(target)
    private Double findRateInBcb(String sourceCurrency, String targetCurrency) {
        try {
            LocalDate data = dataCotacao.isBlank() ? LocalDate.now() : LocalDate.parse(dataCotacao);
            Double sourceInBrl = "BRL".equals(sourceCurrency) ? 1.0 : bcbCotacaoService.getCotacao(sourceCurrency, data);
            Double targetInBrl = "BRL".equals(targetCurrency) ? 1.0 : bcbCotacaoService.getCotacao(targetCurrency, data);
            if (sourceInBrl == null || targetInBrl == null) {
                return null;
            }
            return sourceInBrl / targetInBrl;
        } catch (Exception e) {
            return null;
        }
    }
}
