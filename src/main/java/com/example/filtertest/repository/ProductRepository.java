package com.example.filtertest.repository;

import com.example.filtertest.domain.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductRepository extends ReactiveCrudRepository<Product, Integer> {

    // afterId=0 (the default for a first page, since ids are strictly positive) streams the
    // whole table; a caller resuming from a previous page passes back the last id it saw, which
    // this pushes down as an indexed WHERE seek instead of re-scanning and discarding rows.
    @Query("SELECT id, name, description FROM products WHERE id > :afterId ORDER BY id")
    Flux<Product> streamAllAfter(int afterId);

    // Used only when a client-requested `limit` needs bounded raw fetches (see ProductFilterService):
    // an explicit SQL LIMIT guarantees the query itself completes and the connection is returned to
    // the pool, unlike relying on a reactive Flux.take() to cut off an otherwise-unbounded query -
    // R2DBC/Postgres don't reliably cancel an in-flight unbounded result server-side, so the query
    // keeps running and the connection never frees up.
    @Query("SELECT id, name, description FROM products WHERE id > :afterId ORDER BY id LIMIT :pageSize")
    Flux<Product> streamPageAfter(int afterId, int pageSize);
}
