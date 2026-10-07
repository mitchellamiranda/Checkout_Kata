package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;

import java.util.List;

@FunctionalInterface
public interface IPromotionOptimizer {

    /**
     * Returns the greatest achievable savings without consuming an item twice.
     * The result is between zero and the basket's unit total, inclusive.
     */
    Money maximumSavings(Basket basket, List<PricingRule> rules);
}
