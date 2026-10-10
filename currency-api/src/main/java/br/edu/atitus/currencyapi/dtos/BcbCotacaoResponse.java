package br.edu.atitus.currencyapi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Resposta do endpoint CotacaoMoedaDia da API PTAX do Banco Central
@JsonIgnoreProperties(ignoreUnknown = true)
public record BcbCotacaoResponse(List<Cotacao> value) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cotacao(
            Double cotacaoCompra,
            Double cotacaoVenda,
            String dataHoraCotacao,
            String tipoBoletim
    ) {
    }
}
