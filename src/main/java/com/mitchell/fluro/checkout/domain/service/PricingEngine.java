package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;

import java.util.List;
import java.util.Objects;

public final class PricingEngine implements IPricingEngine {

    private final IPromotionOptimizer optimizer;

    public PricingEngine(IPromotionOptimizer optimizer) {
        this.optimizer = Objects.requireNonNull(optimizer, "Promotion optimizer is required");
    }

    @Override
    public Money calculate(Basket basket, List<IPromotion> promotions) {
        Money unitTotal = basket.unitTotal();
        List<PricingRule> rules = promotions.stream()
                .flatMap(promotion -> promotion.apply(basket).rules().stream())
                .distinct()
                .toList();
        return unitTotal.subtract(optimizer.maximumSavings(basket, rules));
    }
}
