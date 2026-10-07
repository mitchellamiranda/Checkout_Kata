package com.mitchell.fluro.checkout.domain.pricing;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;

import java.util.Map;
import java.util.Objects;

/**
 * One repeatable exchange: consume these quantities and charge this price.
 * Consumed items cannot participate in another exchange.
 */
public record PricingRule(Map<String, Integer> quantities, Money price) {

    public PricingRule {
        quantities = Map.copyOf(quantities);
        Objects.requireNonNull(price, "Rule price is required");
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("A pricing rule must consume at least one item");
        }
        quantities.forEach((sku, quantity) -> {
            Item.requireValidSku(sku);
            if (quantity <= 0) {
                throw new IllegalArgumentException("Rule quantities must be positive");
            }
        });
    }

    public int maximumApplications(Basket basket) {
        return quantities.entrySet().stream()
                .mapToInt(entry -> basket.quantityOf(entry.getKey()) / entry.getValue())
                .min().orElseThrow();
    }

    public Money unitTotal(Basket basket) {
        return quantities.entrySet().stream()
                .map(entry -> basket.item(entry.getKey())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Promotion references SKU absent from basket: " + entry.getKey()))
                        .unitPrice().multiply(entry.getValue()))
                .reduce(Money.ZERO, Money::add);
    }
}
