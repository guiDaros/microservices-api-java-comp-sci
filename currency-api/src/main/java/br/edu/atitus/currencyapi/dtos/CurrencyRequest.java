package br.edu.atitus.currencyapi.dtos;

public record CurrencyRequest(
        String sourceCurrency,
        String targetCurrency,
        Double conversionRate
) {
}
