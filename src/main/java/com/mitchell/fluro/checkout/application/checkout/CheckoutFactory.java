package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;

import java.util.Objects;

public final class CheckoutFactory implements ICheckoutFactory {

    private final PricingRules pricingRules;
    private final IPricingEngine pricingEngine;

    public CheckoutFactory(PricingRules pricingRules, IPricingEngine pricingEngine) {
        this.pricingRules = Objects.requireNonNull(pricingRules, "Pricing rules are required");
        this.pricingEngine = Objects.requireNonNull(pricingEngine, "Pricing engine is required");
    }

    @Override
    public ICheckout create() {
        return new Checkout(pricingRules, pricingEngine);
    }
}
