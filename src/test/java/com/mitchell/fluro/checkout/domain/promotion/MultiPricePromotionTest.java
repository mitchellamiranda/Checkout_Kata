package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Multiprice: B costs 75p, two cost 125p")
class MultiPricePromotionTest {

    private final IPricingEngine engine = new PricingEngine(new PromotionOptimizer());
    private static final Item B = new Item("B", Money.ofPence(75));
    private final IPromotion promotion = new MultiPricePromotion("B", 2, Money.ofPence(125));

    @ParameterizedTest(name = "{0} items cost {1} pence")
    @CsvSource({"0, 0", "1, 75", "2, 125", "3, 200", "4, 250", "5, 325", "10, 625"})
    void pricesBundlesAndRemainders(int quantity, long expected) {
        Basket basket = new Basket(quantity == 0 ? Map.of() : Map.of(B, quantity));
        assertThat(engine.calculate(basket, List.of(promotion))).isEqualTo(Money.ofPence(expected));
    }

    @Test
    void describesOneRepeatableBundleWithoutMutatingTheBasket() {
        Basket basket = new Basket(Map.of(B, 6));
        assertThat(promotion.apply(basket).rules())
                .containsExactly(new PricingRule(Map.of("B", 2), Money.ofPence(125)));
        assertThat(basket.quantityOf("B")).isEqualTo(6);
        assertThat(promotion.apply(Basket.empty())).isEqualTo(PromotionResult.NONE);
    }

    @Test
    void supportsDifferentSkusQuantitiesAndPrices() {
        Item apple = new Item("APPLE", Money.ofPence(50));
        IPromotion revised = new MultiPricePromotion("APPLE", 3, Money.ofPence(130));
        assertThat(engine.calculate(new Basket(Map.of(apple, 4)), List.of(revised)))
                .isEqualTo(Money.ofPence(180));
        assertThat(promotion.apply(new Basket(Map.of(apple, 4)))).isEqualTo(PromotionResult.NONE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidBundleSizes(int quantity) {
        assertThatThrownBy(() -> new MultiPricePromotion("B", quantity, Money.ofPence(125)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
