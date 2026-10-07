package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;

import java.util.Map;

public final class MealDealPromotion implements IPromotion {

    private final PricingRule rule;

    public MealDealPromotion(Map<String, Integer> quantities, Money dealPrice) {
        rule = new PricingRule(quantities, dealPrice);
        if (rule.quantities().size() < 2) {
            throw new IllegalArgumentException("A meal deal must contain at least two different SKUs");
        }
    }

    @Override
    public PromotionResult apply(Basket basket) {
        return PromotionResult.eligible(rule, basket);
    }
}
