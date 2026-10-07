package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;

public final class PricingEngine {

    public Money calculate(Basket basket) {
        return basket.unitTotal();
    }
}
