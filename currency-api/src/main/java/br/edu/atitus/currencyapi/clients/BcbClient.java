package br.edu.atitus.currencyapi.clients;

import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface BcbClient {

    // Ex.: /CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)?@moeda='USD'&@dataCotacao='10-09-2026'&$format=json
    @GetExchange("/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)")
    BcbCotacaoResponse getCotacao(
            @RequestParam("@moeda") String moeda,
            @RequestParam("@dataCotacao") String dataCotacao,
            @RequestParam("$format") String format
    );
}
