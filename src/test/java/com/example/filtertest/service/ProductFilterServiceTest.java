package com.example.filtertest.service;

import com.example.filtertest.config.MatcherProperties;
import com.example.filtertest.domain.Product;
import com.example.filtertest.repository.ProductRepository;
import java.time.Duration;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductFilterServiceTest {

    private static final List<Product> PRODUCTS = List.of(
            new Product(1, "Eagle", "d1"),
            new Product(2, "Falcon", "d2"),
            new Product(3, "Ibis", "d3"));

    private final ProductRepository productRepository = mock(ProductRepository.class);

    // Schedulers.immediate() keeps this test synchronous and deterministic rather than exercising
    // the real dedicated matcherScheduler bean, which is wired and tested via Spring context tests.
    private final ProductFilterService service = new ProductFilterService(
            productRepository, new MatcherProperties(Duration.ofMillis(50)), Schedulers.immediate());

    @Test
    void filterByRegex_excludesNamesMatchingThePattern() {
        when(productRepository.streamAllAfter(0)).thenReturn(Flux.fromIterable(PRODUCTS));

        StepVerifier.create(service.filterByRegex("^E.*$").map(Product::name))
                .expectNext("Falcon", "Ibis")
                .verifyComplete();
    }

    @Test
    void filterByRe2j_excludesNamesMatchingThePattern() {
        when(productRepository.streamAllAfter(0)).thenReturn(Flux.fromIterable(PRODUCTS));

        StepVerifier.create(service.filterByRe2j("^E.*$").map(Product::name))
                .expectNext("Falcon", "Ibis")
                .verifyComplete();
    }

    @Test
    void filterByRegex_appliesLimitAfterFiltering() {
        when(productRepository.streamPageAfter(eq(0), anyInt())).thenReturn(Flux.fromIterable(PRODUCTS));

        StepVerifier.create(service.filterByRegex("^E.*$", 0, 1).map(Product::name))
                .expectNext("Falcon")
                .verifyComplete();
    }

    @Test
    void filterByRegex_limitSpanningMultiplePages_fetchesBoundedPagesUntilSatisfied() {
        // First page is entirely excluded but full-sized, forcing a second, bounded fetch rather
        // than ever leaving an unbounded query in flight (see ProductFilterService.fetchLimitedPages).
        when(productRepository.streamPageAfter(eq(0), anyInt())).thenAnswer(invocation -> {
            int pageSize = invocation.getArgument(1);
            List<Product> excludedFullPage = IntStream.rangeClosed(1, pageSize)
                    .mapToObj(i -> new Product(i, "Eagle-" + i, "d"))
                    .toList();
            return Flux.fromIterable(excludedFullPage);
        });
        when(productRepository.streamPageAfter(intThat(offset -> offset > 0), anyInt()))
                .thenReturn(Flux.fromIterable(List.of(
                        new Product(100_001, "Falcon-1", "d"),
                        new Product(100_002, "Falcon-2", "d"),
                        new Product(100_003, "Falcon-3", "d"))));

        StepVerifier.create(service.filterByRegex("^E.*$", 0, 3).map(Product::name))
                .expectNext("Falcon-1", "Falcon-2", "Falcon-3")
                .verifyComplete();
    }

    @Test
    void filterByRegex_offset_delegatesToRepositoryCursor() {
        when(productRepository.streamAllAfter(2)).thenReturn(Flux.fromIterable(List.of(PRODUCTS.get(2))));

        StepVerifier.create(service.filterByRegex("^E.*$", 2, null).map(Product::name))
                .expectNext("Ibis")
                .verifyComplete();
    }

    @Test
    void filterByRegex_invalidPattern_throwsBeforeTouchingRepository() {
        assertThatThrownBy(() -> service.filterByRegex("(unclosed"))
                .isInstanceOf(PatternSyntaxException.class);
    }

    @Test
    void filterByRe2j_invalidPattern_throwsBeforeTouchingRepository() {
        assertThatThrownBy(() -> service.filterByRe2j("(unclosed"))
                .isInstanceOf(com.google.re2j.PatternSyntaxException.class);
    }
}
