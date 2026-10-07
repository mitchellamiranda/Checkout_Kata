package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;

import java.util.List;

@FunctionalInterface
public interface IPricingEngine {

    /**
     * Returns the lowest total for the supplied repeatable promotions without
     * mutating the basket or depending on promotion registration order.
     */
    Money calculate(Basket basket, List<IPromotion> promotions);
}
