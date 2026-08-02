package com.example.filtertest.dto;

import com.example.filtertest.domain.Product;

public record ProductResponse(Long id, String name, String description) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id().longValue(), product.name(), product.description());
    }
}
