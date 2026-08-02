package com.example.filtertest.controller;

import com.example.filtertest.dto.ProductResponse;
import com.example.filtertest.service.ProductFilterService;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/shop/product")
class ProductController {

    private final ProductFilterService productFilterService;
    private final JsonArrayBodyWriter jsonArrayBodyWriter;

    ProductController(ProductFilterService productFilterService, JsonArrayBodyWriter jsonArrayBodyWriter) {
        this.productFilterService = productFilterService;
        this.jsonArrayBodyWriter = jsonArrayBodyWriter;
    }

    @GetMapping
    Mono<Void> byRegex(@RequestParam String nameFilter,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Integer limit,
            ServerHttpResponse response) {
        Flux<ProductResponse> body = productFilterService.filterByRegex(nameFilter, offset, limit)
                .map(ProductResponse::from);
        return writeJsonArray(body, response);
    }

    @GetMapping("/re2")
    Mono<Void> byRe2j(@RequestParam String nameFilter,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Integer limit,
            ServerHttpResponse response) {
        Flux<ProductResponse> body = productFilterService.filterByRe2j(nameFilter, offset, limit)
                .map(ProductResponse::from);
        return writeJsonArray(body, response);
    }

    private Mono<Void> writeJsonArray(Flux<ProductResponse> body, ServerHttpResponse response) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(jsonArrayBodyWriter.writeArray(body, response.bufferFactory()));
    }
}
