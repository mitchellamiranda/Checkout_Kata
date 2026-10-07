package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutFactoryTest {

    private final PricingRules rules = new PricingRules(List.of(new Item("A", Money.ofPence(50))));
    private final IPricingEngine pricingEngine = new PricingEngine(new PromotionOptimizer());

    @Test
    void createsIsolatedTransactionsUsingTheInjectedEngine() {
        List<Basket> pricedBaskets = new ArrayList<>();
        IPricingEngine recordingEngine = (basket, promotions) -> {
            pricedBaskets.add(basket);
            assertThat(promotions).isSameAs(rules.promotions());
            return pricingEngine.calculate(basket, promotions);
        };
        ICheckoutFactory factory = new CheckoutFactory(rules, recordingEngine);
        ICheckout first = factory.create();
        ICheckout second = factory.create();
        assertThat(first).isNotSameAs(second);
        first.scan("A");
        assertThat(first.getTotal()).isEqualTo(Money.ofPence(50));
        assertThat(second.getTotal()).isEqualTo(Money.ZERO);
        assertThat(pricedBaskets).containsExactly(first.getBasket(), second.getBasket());
    }

    @Test
    void rejectsMissingDependencies() {
        assertThatThrownBy(() -> new CheckoutFactory(null, pricingEngine)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CheckoutFactory(rules, null)).isInstanceOf(NullPointerException.class);
    }
}
