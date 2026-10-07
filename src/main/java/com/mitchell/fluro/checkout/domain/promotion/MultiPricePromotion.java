package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;

import java.util.Map;

public final class MultiPricePromotion implements Promotion {

    private final PricingRule rule;

    public MultiPricePromotion(String sku, int quantity, Money bundlePrice) {
        rule = new PricingRule(Map.of(sku, quantity), bundlePrice);
    }

    @Override
    public PromotionResult apply(Basket basket) {
        return PromotionResult.eligible(rule, basket);
    }
}
