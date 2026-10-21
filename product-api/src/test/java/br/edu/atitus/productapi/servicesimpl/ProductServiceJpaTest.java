package br.edu.atitus.productapi.servicesimpl;

import br.edu.atitus.productapi.dtos.CurrencyResponse;
import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.repositories.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceJpaTest {

    private ProductRepository repository;
    private CurrencyClientService currencyClientService;
    private ProductServiceJpa service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        currencyClientService = mock(CurrencyClientService.class);
        service = new ProductServiceJpa(repository, currencyClientService);
        ReflectionTestUtils.setField(service, "serverPort", "8000");
        ReflectionTestUtils.setField(service, "promotionMessage", "Promo");
        ProductEntity product = new ProductEntity(1L, "Apple", "iPhone 15", "Smartphone", "USD", 100.0, "img");
        when(repository.findById(1L)).thenReturn(Optional.of(product));
    }

    @Test
    void convertsPriceWhenCurrencyApiResponds() throws Exception {
        when(currencyClientService.getCurrency("USD", "BRL"))
                .thenReturn(new CurrencyResponse("USD", "BRL", 5.0, "Currency API running in Port: 8100"));

        ProductResponse response = service.findById(1L, "BRL");

        assertEquals(500.0, response.convertedPrice());
        assertEquals(100.0, response.price());
        assertEquals("Product API running in port 8000 - Currency API running in Port: 8100", response.environment());
    }

    @Test
    void returnsFallbackWhenCurrencyApiFails() throws Exception {
        when(currencyClientService.getCurrency("USD", "BRL"))
                .thenThrow(new RuntimeException("Connection refused"));

        ProductResponse response = service.findById(1L, "BRL");

        assertEquals(-1, response.convertedPrice());
        assertEquals(100.0, response.price());
        assertTrue(response.environment().endsWith(" - ERROR: Connection refused"));
    }

    @Test
    void doesNotMaskProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(99L, "BRL"));
        verifyNoInteractions(currencyClientService);
    }
}
