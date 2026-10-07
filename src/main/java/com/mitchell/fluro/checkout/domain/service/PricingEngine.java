package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;

import java.util.List;

public final class PricingEngine {

    private final PromotionOptimizer optimizer = new PromotionOptimizer();

    public Money calculate(Basket basket, List<IPromotion> promotions) {
        Money unitTotal = basket.unitTotal();
        List<PricingRule> rules = promotions.stream()
                .flatMap(promotion -> promotion.apply(basket).rules().stream())
                .distinct()
                .toList();
        return unitTotal.subtract(optimizer.maximumSavings(basket, rules));
    }
}
