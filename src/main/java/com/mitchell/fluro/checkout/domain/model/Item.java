package com.mitchell.fluro.checkout.domain.model;

import java.util.Objects;

public record Item(String sku, Money unitPrice) {

    public Item {
        requireValidSku(sku);
        Objects.requireNonNull(unitPrice, "Unit price is required");
    }

    public static String requireValidSku(String sku) {
        Objects.requireNonNull(sku, "SKU is required");
        if (sku.isBlank() || !sku.equals(sku.strip())) {
            throw new IllegalArgumentException("SKU must be nonblank without surrounding whitespace");
        }
        return sku;
    }
}
