package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import java.util.Objects;

public final class CheckoutFactory implements ICheckoutFactory {

    private final IPricingEngine pricingEngine;

    public CheckoutFactory(IPricingEngine pricingEngine) {
        this.pricingEngine = Objects.requireNonNull(
                pricingEngine, "Pricing engine is required");
    }

    @Override
    public ICheckout create(PricingRules pricingRules) {
        return new Checkout(pricingRules, pricingEngine);
    }
}