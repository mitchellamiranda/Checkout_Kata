package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;

import java.util.Objects;

public final class CheckoutFactory {

    private final PricingRules pricingRules;
    private final PricingEngine pricingEngine;

    public CheckoutFactory(PricingRules pricingRules, PricingEngine pricingEngine) {
        this.pricingRules = Objects.requireNonNull(pricingRules, "Pricing rules are required");
        this.pricingEngine = Objects.requireNonNull(pricingEngine, "Pricing engine is required");
    }

    public Checkout create() {
        return new Checkout(pricingRules, pricingEngine);
    }
}
