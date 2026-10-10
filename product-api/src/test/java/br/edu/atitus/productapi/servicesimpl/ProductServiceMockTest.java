package br.edu.atitus.productapi.servicesimpl;

import br.edu.atitus.productapi.dtos.ProductResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductServiceMockTest {

    private ProductServiceMock service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceMock();
        ReflectionTestUtils.setField(service, "serverPort", "8000");
        ReflectionTestUtils.setField(service, "promotionMessage", "Christmas Promotion");
    }

    @Test
    void findByIdReturnsFieldsWithTheirCorrectMeaning() throws Exception {
        ProductResponse response = service.findById(1L, "BRL");

        assertEquals("Apple", response.brand());
        assertEquals("iPhone 15", response.model());
        assertEquals("iPhone 15 128GB", response.description());
    }

    @Test
    void findByIdThrowsEntityNotFoundExceptionWhenProductDoesNotExist() {
        assertThrows(EntityNotFoundException.class, () -> service.findById(99L, "BRL"));
    }

    @Test
    void findAllReturnsRequestedSlice() throws Exception {
        Page<ProductResponse> page = service.findAll(PageRequest.of(1, 3), "BRL");

        assertEquals(4, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals(4L, page.getContent().get(0).id());
    }

    @Test
    void findAllReturnsEmptyContentWhenPageIsOutOfRange() throws Exception {
        Page<ProductResponse> page = service.findAll(PageRequest.of(5, 10), "BRL");

        assertEquals(4, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }
}
