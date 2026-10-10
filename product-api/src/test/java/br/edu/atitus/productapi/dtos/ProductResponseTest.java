package br.edu.atitus.productapi.dtos;

import br.edu.atitus.productapi.entities.ProductEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductResponseTest {

    @Test
    void fromEntityMapsEachFieldToItsOwnComponent() {
        ProductEntity entity = new ProductEntity(
                7L, "Apple", "iPhone 15", "Smartphone Apple", "USD", 699.00, "https://example.com/iphone.jpg");

        ProductResponse response = ProductResponse.fromEntity(
                entity, "Product API running in port 8000", "Christmas Promotion", "BRL", 0);

        assertEquals(7L, response.id());
        assertEquals("Apple", response.brand());
        assertEquals("iPhone 15", response.model());
        assertEquals("Smartphone Apple", response.description());
        assertEquals("USD", response.currency());
        assertEquals(699.00, response.price());
        assertEquals("https://example.com/iphone.jpg", response.image());
        assertEquals("Product API running in port 8000", response.environment());
        assertEquals("Christmas Promotion", response.promotionMessage());
        assertEquals("BRL", response.targetCurrency());
        assertEquals(0, response.convertedPrice());
    }
}
