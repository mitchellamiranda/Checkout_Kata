package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;

import java.util.Objects;

/**
 * A single transaction, confined to its owning thread.
 * Pricing rules are fixed for its lifetime; basket snapshots are immutable.
 */
public final class Checkout implements ICheckout {

    private final PricingRules pricingRules;
    private final IPricingEngine pricingEngine;
    private Basket basket = Basket.empty();

    public Checkout(PricingRules pricingRules, IPricingEngine pricingEngine) {
        this.pricingRules = Objects.requireNonNull(pricingRules, "Pricing rules are required");
        this.pricingEngine = Objects.requireNonNull(pricingEngine, "Pricing engine is required");
    }

    @Override
    public void scan(String sku) {
        basket = basket.add(pricingRules.item(sku));
    }

    @Override
    public Money getTotal() {
        return pricingEngine.calculate(basket, pricingRules.promotions());
    }

    @Override
    public Basket getBasket() {
        return basket;
    }
}
