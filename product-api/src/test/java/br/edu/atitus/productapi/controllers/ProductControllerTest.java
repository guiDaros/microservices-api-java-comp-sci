package br.edu.atitus.productapi.controllers;

import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ProductService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ProductService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getByIdReturns200WithProduct() throws Exception {
        ProductEntity entity = new ProductEntity(1L, "Apple", "iPhone 15", "Smartphone Apple", "USD", 699.00, "img");
        when(service.findById(1L, "BRL"))
                .thenReturn(ProductResponse.fromEntity(entity, "env", "promo", "BRL", 0));

        mockMvc.perform(get("/products/1").param("targetCurrency", "BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.model").value("iPhone 15"))
                .andExpect(jsonPath("$.description").value("Smartphone Apple"))
                .andExpect(jsonPath("$.targetCurrency").value("BRL"));
    }

    @Test
    void getByIdReturns404WhenProductDoesNotExist() throws Exception {
        when(service.findById(99L, "BRL")).thenThrow(new EntityNotFoundException("Produto não encontrado"));

        mockMvc.perform(get("/products/99").param("targetCurrency", "BRL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturns400WhenTargetCurrencyIsMissing() throws Exception {
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("targetCurrency")));

        verifyNoInteractions(service);
    }

    @Test
    void getByIdReturns400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/products/abc").param("targetCurrency", "BRL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("id")));

        verifyNoInteractions(service);
    }

    @Test
    void getAllReturns400WhenTargetCurrencyIsMissing() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("targetCurrency")));

        verifyNoInteractions(service);
    }

    @Test
    void getAllUsesDefaultPagination() throws Exception {
        when(service.findAll(any(Pageable.class), eq("BRL")))
                .thenAnswer(invocation -> new PageImpl<ProductResponse>(List.of(), invocation.getArgument(0), 0));

        mockMvc.perform(get("/products").param("targetCurrency", "BRL"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(captor.capture(), eq("BRL"));
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals(Sort.by("description"), captor.getValue().getSort());
    }

    @Test
    void getAllReturns400WhenSortPropertyIsInvalid() throws Exception {
        when(service.findAll(any(Pageable.class), anyString()))
                .thenThrow(new PropertyReferenceException("xyz", TypeInformation.of(ProductEntity.class), List.of()));

        mockMvc.perform(get("/products").param("targetCurrency", "BRL").param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("xyz")));
    }

    @Test
    void unexpectedErrorsStillReturn500() throws Exception {
        when(service.findById(anyLong(), anyString())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/products/1").param("targetCurrency", "BRL"))
                .andExpect(status().isInternalServerError());
    }
}
