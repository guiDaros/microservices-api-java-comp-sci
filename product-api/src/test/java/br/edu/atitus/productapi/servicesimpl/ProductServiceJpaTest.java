package br.edu.atitus.productapi.servicesimpl;

import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.repositories.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceJpaTest {

    private ProductRepository repository;
    private ProductServiceJpa service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductServiceJpa(repository);
        ReflectionTestUtils.setField(service, "serverPort", "8000");
        ReflectionTestUtils.setField(service, "promotionMessage", "Christmas Promotion");
    }

    @Test
    void findByIdReturnsMappedProduct() throws Exception {
        ProductEntity entity = new ProductEntity(
                1L, "Apple", "iPhone 15", "Smartphone Apple", "USD", 699.00, "https://example.com/iphone-15.jpg");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ProductResponse response = service.findById(1L, "BRL");

        assertEquals(1L, response.id());
        assertEquals("Apple", response.brand());
        assertEquals("iPhone 15", response.model());
        assertEquals("Smartphone Apple", response.description());
        assertEquals("Product API running in port 8000", response.environment());
        assertEquals("Christmas Promotion", response.promotionMessage());
        assertEquals("BRL", response.targetCurrency());
        assertEquals(0, response.convertedPrice());
    }

    @Test
    void findByIdThrowsEntityNotFoundExceptionWhenProductDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.findById(99L, "BRL")
        );

        assertEquals("Produto não encontrado", exception.getMessage());
    }

    @Test
    void findAllMapsRepositoryPageKeepingPaginationMetadata() throws Exception {
        Pageable pageable = PageRequest.of(1, 2, Sort.by("description"));
        List<ProductEntity> entities = List.of(
                new ProductEntity(3L, "Apple", "iPhone 16", "Smartphone Apple A18", "USD", 799.00, "img3"),
                new ProductEntity(4L, "Samsung", "Galaxy S24", "Smartphone Samsung", "USD", 799.00, "img4")
        );
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(entities, pageable, 14));

        Page<ProductResponse> page = service.findAll(pageable, "EUR");

        assertEquals(14, page.getTotalElements());
        assertEquals(1, page.getNumber());
        assertEquals(2, page.getSize());
        assertEquals(2, page.getContent().size());
        assertEquals("Apple", page.getContent().get(0).brand());
        assertEquals("Galaxy S24", page.getContent().get(1).model());
        assertEquals("EUR", page.getContent().get(1).targetCurrency());
        assertEquals("Christmas Promotion", page.getContent().get(0).promotionMessage());
    }
}
