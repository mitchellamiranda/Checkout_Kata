package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.pricing.PricingRules;

@FunctionalInterface
public interface ICheckoutFactory {

    /**
     * Creates a fresh, empty transaction with its own
     * basket and pricing rules.
     * Pricing rules are fixed for the lifetime of the transaction.
     */
    ICheckout create(PricingRules pricingRules);
}
