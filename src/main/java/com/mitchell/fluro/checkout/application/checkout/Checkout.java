package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;

import java.util.Objects;

/**
 * A single transaction, confined to its owning thread.
 * Pricing rules are fixed for its lifetime; basket snapshots are immutable.
 */
public final class Checkout {

    private final PricingRules pricingRules;
    private final PricingEngine pricingEngine;
    private Basket basket = Basket.empty();

    public Checkout(PricingRules pricingRules) {
        this(pricingRules, new PricingEngine());
    }

    Checkout(PricingRules pricingRules, PricingEngine pricingEngine) {
        this.pricingRules = Objects.requireNonNull(pricingRules, "Pricing rules are required");
        this.pricingEngine = Objects.requireNonNull(pricingEngine, "Pricing engine is required");
    }

    public void scan(String sku) {
        basket = basket.add(pricingRules.item(sku));
    }

    public Money getTotal() {
        return pricingEngine.calculate(basket);
    }

    public Basket getBasket() {
        return basket;
    }
}
