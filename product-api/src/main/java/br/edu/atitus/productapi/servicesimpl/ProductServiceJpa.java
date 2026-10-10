package br.edu.atitus.productapi.servicesimpl;

import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.repositories.ProductRepository;
import br.edu.atitus.productapi.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceJpa implements ProductService {

    private final ProductRepository repository;
    private final CurrencyClientService currencyClientService;

    public ProductServiceJpa(ProductRepository repository, CurrencyClientService currencyClientService) {
        this.repository = repository;
        this.currencyClientService = currencyClientService;
    }

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.promotion.message:Nenhuma Promoção Ativa}")
    private String promotionMessage;


    @Override
    public ProductResponse findById(Long id, String targetCurrency) throws Exception {
        var product = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));
        return toResponse(product, targetCurrency);
    }

    @Override
    public Page<ProductResponse> findAll(Pageable pageable, String targetCurrency) throws Exception {
        var products = repository.findAll(pageable);
        return products.map(entity -> toResponse(entity, targetCurrency));
    }

    @Override
    public ProductEntity save(ProductRequest request) throws Exception {
        return null;
    }

    private ProductResponse toResponse(ProductEntity entity, String targetCurrency) {
        String environment = "Product API running in port " + serverPort;
        double convertedPrice;

        // Fallback: se a cotacao falhar, o produto ainda e retornado com convertedPrice = -1
        try {
            var currency = currencyClientService.getCurrency(entity.getCurrency(), targetCurrency);
            convertedPrice = entity.getPrice() * currency.conversionRate();
            environment += " - " + currency.environment();
        } catch (Exception e) {
            convertedPrice = -1;
            environment += " - ERROR: " + e.getMessage();
        }

        return ProductResponse.fromEntity(
                entity,
                environment,
                promotionMessage,
                targetCurrency,
                convertedPrice
        );
    }
}
