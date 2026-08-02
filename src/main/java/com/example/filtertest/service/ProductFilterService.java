package com.example.filtertest.service;

import com.example.filtertest.config.MatcherProperties;
import com.example.filtertest.domain.Product;
import com.example.filtertest.repository.ProductRepository;
import com.example.filtertest.service.matcher.NameMatcher;
import com.example.filtertest.service.matcher.Re2jNameMatcher;
import com.example.filtertest.service.matcher.RegexNameMatcher;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

@Service
public class ProductFilterService {

    // Raw rows fetched per bounded page when a client requests `limit`. Each page is a real SQL
    // LIMIT query, so it always completes and returns its connection promptly regardless of
    // selectivity; a small in-memory list per page is fine, unlike materializing the whole table.
    private static final int PAGE_FETCH_SIZE = 5000;

    private final ProductRepository productRepository;
    private final MatcherProperties matcherProperties;
    private final Scheduler matcherScheduler;

    ProductFilterService(ProductRepository productRepository, MatcherProperties matcherProperties,
            Scheduler matcherScheduler) {
        this.productRepository = productRepository;
        this.matcherProperties = matcherProperties;
        this.matcherScheduler = matcherScheduler;
    }

    public Flux<Product> filterByRegex(String nameFilter) {
        return filterByRegex(nameFilter, 0, null);
    }

    public Flux<Product> filterByRegex(String nameFilter, int afterId, Integer limit) {
        NameMatcher matcher = RegexNameMatcher.compile(nameFilter, matcherProperties.matchTimeout());
        return filtered(matcher, afterId, limit);
    }

    public Flux<Product> filterByRe2j(String nameFilter) {
        return filterByRe2j(nameFilter, 0, null);
    }

    public Flux<Product> filterByRe2j(String nameFilter, int afterId, Integer limit) {
        NameMatcher matcher = Re2jNameMatcher.compile(nameFilter);
        return filtered(matcher, afterId, limit);
    }

    private Flux<Product> filtered(NameMatcher matcher, int afterId, Integer limit) {
        // publishOn moves the CPU-bound match evaluation onto a dedicated scheduler, off the Netty
        // event-loop threads (so one request's filtering can't stall other requests' I/O) and off
        // Reactor's shared global Schedulers.parallel() (so this workload doesn't contend with any
        // other CPU-bound work elsewhere in the app for the same fixed-size pool).
        if (limit == null) {
            return productRepository.streamAllAfter(afterId)
                    .publishOn(matcherScheduler)
                    .filter(product -> !matcher.excludes(product.name()));
        }
        return fetchLimitedPages(matcher, afterId, limit);
    }

    // Deliberately does NOT implement `limit` as an unbounded stream + Flux.take(): R2DBC/Postgres
    // don't reliably cancel an in-flight query with no SQL-level bound just because the reactive
    // subscriber stopped requesting, so the query keeps running server-side and its connection
    // never returns to the pool. Fetching bounded pages (each a real SQL LIMIT) and recursing only
    // if more rows are needed guarantees every query issued actually completes.
    private Flux<Product> fetchLimitedPages(NameMatcher matcher, int afterId, int remaining) {
        if (remaining <= 0) {
            return Flux.empty();
        }
        return productRepository.streamPageAfter(afterId, PAGE_FETCH_SIZE)
                .publishOn(matcherScheduler)
                .collectList()
                .flatMapMany(rawPage -> {
                    List<Product> matches = rawPage.stream()
                            .filter(product -> !matcher.excludes(product.name()))
                            .toList();
                    List<Product> taken = matches.size() > remaining ? matches.subList(0, remaining) : matches;
                    Flux<Product> page = Flux.fromIterable(taken);

                    boolean tableExhausted = rawPage.size() < PAGE_FETCH_SIZE;
                    if (tableExhausted || taken.size() >= remaining) {
                        return page;
                    }
                    int nextAfterId = rawPage.get(rawPage.size() - 1).id();
                    int stillNeeded = remaining - taken.size();
                    return page.concatWith(Flux.defer(() -> fetchLimitedPages(matcher, nextAfterId, stillNeeded)));
                });
    }
}
