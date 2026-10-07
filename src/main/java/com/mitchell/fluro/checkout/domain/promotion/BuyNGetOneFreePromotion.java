package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;

import java.util.List;
import java.util.Map;

public final class BuyNGetOneFreePromotion implements Promotion {

    private final String sku;
    private final int paidQuantity;
    private final int bundleQuantity;

    public BuyNGetOneFreePromotion(String sku, int paidQuantity) {
        this.sku = Item.requireValidSku(sku);
        if (paidQuantity <= 0) {
            throw new IllegalArgumentException("Paid quantity must be positive");
        }
        this.paidQuantity = paidQuantity;
        this.bundleQuantity = Math.addExact(paidQuantity, 1);
    }

    @Override
    public PromotionResult apply(Basket basket) {
        if (basket.quantityOf(sku) < bundleQuantity) {
            return PromotionResult.NONE;
        }
        Money price = basket.item(sku).orElseThrow().unitPrice().multiply(paidQuantity);
        return new PromotionResult(List.of(new PricingRule(Map.of(sku, bundleQuantity), price)));
    }
}
