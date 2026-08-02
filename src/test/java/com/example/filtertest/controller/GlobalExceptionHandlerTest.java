package com.example.filtertest.controller;

import com.example.filtertest.domain.Product;
import com.example.filtertest.service.ProductFilterService;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ProductController.class)
@Import(JsonArrayBodyWriter.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductFilterService productFilterService;

    @Test
    void missingNameFilter_returns400() {
        webTestClient.get().uri("/shop/product")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void invalidRegex_returns400() {
        when(productFilterService.filterByRegex(anyString(), anyInt(), any()))
                .thenThrow(new PatternSyntaxException("invalid", "(unclosed", -1));

        webTestClient.get()
                .uri(uri -> uri.path("/shop/product").queryParam("nameFilter", "(unclosed").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void invalidRe2jRegex_returns400() {
        when(productFilterService.filterByRe2j(anyString(), anyInt(), any()))
                .thenThrow(new com.google.re2j.PatternSyntaxException("missing closing )", "(a"));

        webTestClient.get()
                .uri(uri -> uri.path("/shop/product/re2").queryParam("nameFilter", "(a").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void unexpectedRepositoryError_returns500() {
        when(productFilterService.filterByRegex(anyString(), anyInt(), any()))
                .thenReturn(Flux.<Product>error(new RuntimeException("boom")));

        webTestClient.get()
                .uri(uri -> uri.path("/shop/product").queryParam("nameFilter", ".*").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500);
    }
}
