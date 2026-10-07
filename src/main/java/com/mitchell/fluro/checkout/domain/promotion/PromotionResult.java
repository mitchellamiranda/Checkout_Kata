package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;

import java.util.List;

public record PromotionResult(List<PricingRule> rules) {

    public static final PromotionResult NONE = new PromotionResult(List.of());

    public PromotionResult {
        rules = List.copyOf(rules);
    }

    public static PromotionResult eligible(PricingRule rule, Basket basket) {
        return rule.maximumApplications(basket) > 0
                ? new PromotionResult(List.of(rule))
                : NONE;
    }
}
