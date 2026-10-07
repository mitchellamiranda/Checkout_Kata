package com.mitchell.fluro.checkout.domain.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record Basket(Map<Item, Integer> quantities) {

    public Basket {
        quantities = Map.copyOf(quantities);
        Set<String> skus = new HashSet<>();
        quantities.forEach((item, quantity) -> {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Basket quantities must be positive");
            }
            if (!skus.add(item.sku())) {
                throw new IllegalArgumentException("Duplicate SKU in basket: " + item.sku());
            }
        });
    }

    public static Basket empty() {
        return new Basket(Map.of());
    }

    public Basket add(Item item) {
        item(item.sku()).ifPresent(existing -> {
            if (!existing.equals(item)) {
                throw new IllegalArgumentException("Conflicting price for SKU: " + item.sku());
            }
        });
        Map<Item, Integer> updated = new HashMap<>(quantities);
        updated.merge(item, 1, Math::addExact);
        return new Basket(updated);
    }

    public Optional<Item> item(String sku) {
        Item.requireValidSku(sku);
        return quantities.keySet().stream()
                .filter(item -> item.sku().equals(sku))
                .findFirst();
    }

    public int quantityOf(String sku) {
        return item(sku).map(quantities::get).orElse(0);
    }

    public Money unitTotal() {
        return quantities.entrySet().stream()
                .map(entry -> entry.getKey().unitPrice().multiply(entry.getValue()))
                .reduce(Money.ZERO, Money::add);
    }
}
