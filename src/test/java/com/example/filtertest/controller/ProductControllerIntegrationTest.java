package com.example.filtertest.controller;

import com.example.filtertest.domain.Product;
import com.example.filtertest.dto.ProductResponse;
import com.example.filtertest.repository.ProductRepository;
import com.example.filtertest.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    private static final List<Product> FIXTURES = List.of(
            new Product(1, "Eagle", "d1"),
            new Product(2, "Falcon", "d2"),
            new Product(3, "Ibis", "d3"),
            new Product(4, "Vortex", "d4"),
            new Product(5, "Zephyr", "d5"));

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void seed() {
        productRepository.deleteAll()
                .thenMany(productRepository.saveAll(Flux.fromIterable(FIXTURES)))
                .blockLast();
    }

    @Test
    void regexEndpoint_excludesNamesStartingWithE() {
        assertNamesExcludingE("/shop/product");
    }

    @Test
    void re2jEndpoint_excludesNamesStartingWithE() {
        assertNamesExcludingE("/shop/product/re2");
    }

    private void assertNamesExcludingE(String path) {
        webTestClient.get()
                .uri(uri -> uri.path(path).queryParam("nameFilter", "^E.*$").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .value(products -> assertThat(products).extracting(ProductResponse::name)
                        .containsExactlyInAnyOrder("Falcon", "Ibis", "Vortex", "Zephyr"));
    }

    @Test
    void regexEndpoint_excludesNamesContainingEvaCharacters() {
        assertOnlyIbisRemains("/shop/product");
    }

    @Test
    void re2jEndpoint_excludesNamesContainingEvaCharacters() {
        assertOnlyIbisRemains("/shop/product/re2");
    }

    private void assertOnlyIbisRemains(String path) {
        webTestClient.get()
                .uri(uri -> uri.path(path).queryParam("nameFilter", "^.*[eva].*$").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .value(products -> assertThat(products).extracting(ProductResponse::name)
                        .containsExactly("Ibis"));
    }

    @Test
    void regexEndpoint_limitCapsResultsAfterFiltering() {
        webTestClient.get()
                .uri(uri -> uri.path("/shop/product")
                        .queryParam("nameFilter", "^E.*$")
                        .queryParam("limit", "2")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .value(products -> assertThat(products).extracting(ProductResponse::name)
                        .containsExactly("Falcon", "Ibis"));
    }

    @Test
    void regexEndpoint_offset_resumesFromCursor() {
        webTestClient.get()
                .uri(uri -> uri.path("/shop/product")
                        .queryParam("nameFilter", "^E.*$")
                        .queryParam("offset", "3")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .value(products -> assertThat(products).extracting(ProductResponse::name)
                        .containsExactly("Vortex", "Zephyr"));
    }

    @Test
    void regexEndpoint_missingNameFilter_returns400() {
        webTestClient.get().uri("/shop/product")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void re2jEndpoint_missingNameFilter_returns400() {
        webTestClient.get().uri("/shop/product/re2")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void regexEndpoint_invalidRegex_returns400() {
        webTestClient.get()
                .uri(uri -> uri.path("/shop/product").queryParam("nameFilter", "(unclosed").build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void re2jEndpoint_backreferenceUnsupportedByRe2j_returns400() {
        webTestClient.get()
                .uri(uri -> uri.path("/shop/product/re2").queryParam("nameFilter", "(a)\\1").build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void regexEndpoint_filterMatchingEverything_returnsEmptyResult() {
        webTestClient.get()
                .uri(uri -> uri.path("/shop/product").queryParam("nameFilter", ".*").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .hasSize(0);
    }

    @Test
    void re2jEndpoint_filterMatchingEverything_returnsEmptyResult() {
        webTestClient.get()
                .uri(uri -> uri.path("/shop/product/re2").queryParam("nameFilter", ".*").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .hasSize(0);
    }
}
