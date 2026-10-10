package br.edu.atitus.currencyapi.controllers;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrencyControllerTest {

    private CurrencyService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(CurrencyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CurrencyController(service)).build();
    }

    @Test
    void returns200WithConversionRate() throws Exception {
        when(service.findBySourceCurrencyAndTargetCurrency("USD", "BRL"))
                .thenReturn(new CurrencyResponse("USD", "BRL", 5.15, "Currency API running in Port: 8100"));

        mockMvc.perform(get("/currencies").param("source", "USD").param("target", "BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCurrency").value("USD"))
                .andExpect(jsonPath("$.targetCurrency").value("BRL"))
                .andExpect(jsonPath("$.conversionRate").value(5.15));
    }

    @Test
    void returns404WhenPairDoesNotExist() throws Exception {
        when(service.findBySourceCurrencyAndTargetCurrency("EUR", "BRL"))
                .thenThrow(new EntityNotFoundException("Cotação não encontrada"));

        mockMvc.perform(get("/currencies").param("source", "EUR").param("target", "BRL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400WhenSourceIsMissing() throws Exception {
        mockMvc.perform(get("/currencies").param("target", "BRL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("source")));

        verifyNoInteractions(service);
    }

    @Test
    void returns400WhenTargetIsMissing() throws Exception {
        mockMvc.perform(get("/currencies").param("source", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("target")));

        verifyNoInteractions(service);
    }

    @Test
    void unexpectedErrorsStillReturn500() throws Exception {
        when(service.findBySourceCurrencyAndTargetCurrency(anyString(), anyString()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/currencies").param("source", "USD").param("target", "BRL"))
                .andExpect(status().isInternalServerError());
    }
}
