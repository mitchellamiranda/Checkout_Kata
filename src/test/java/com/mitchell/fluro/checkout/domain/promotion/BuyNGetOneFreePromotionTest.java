package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Buy three C, get a fourth C free")
class BuyNGetOneFreePromotionTest {

    private static final Item C = new Item("C", Money.ofPence(25));
    private final IPromotion promotion = new BuyNGetOneFreePromotion("C", 3);

    @ParameterizedTest(name = "{0} items cost {1} pence")
    @CsvSource({"0, 0", "1, 25", "2, 50", "3, 75", "4, 75", "5, 100",
            "6, 125", "7, 150", "8, 150", "9, 175", "11, 225", "12, 225", "13, 250"})
    void pricesCompleteGroupsOfFourAndRemainders(int quantity, long expected) {
        Basket basket = new Basket(quantity == 0 ? Map.of() : Map.of(C, quantity));
        assertThat(new PricingEngine().calculate(basket, List.of(promotion))).isEqualTo(Money.ofPence(expected));
    }

    @Test
    void consumesTheFreeItemRatherThanCreatingUnscannedItems() {
        assertThat(promotion.apply(new Basket(Map.of(C, 3)))).isEqualTo(PromotionResult.NONE);
        assertThat(promotion.apply(new Basket(Map.of(C, 8))).rules())
                .containsExactly(new PricingRule(Map.of("C", 4), Money.ofPence(75)));
    }

    @Test
    void derivesTheBundlePriceFromTheTransactionNotHardcodedPrices() {
        Item changedC = new Item("C", Money.ofPence(40));
        assertThat(new PricingEngine().calculate(new Basket(Map.of(changedC, 4)), List.of(promotion)))
                .isEqualTo(Money.ofPence(120));
    }

    @Test
    void supportsBuyOneGetOneFreeAndAlreadyFreeItems() {
        IPromotion buyOne = new BuyNGetOneFreePromotion("C", 1);
        assertThat(new PricingEngine().calculate(new Basket(Map.of(C, 5)), List.of(buyOne)))
                .isEqualTo(Money.ofPence(75));
        assertThat(new PricingEngine().calculate(new Basket(Map.of(new Item("C", Money.ZERO), 4)),
                List.of(promotion))).isEqualTo(Money.ZERO);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidPaidQuantities(int quantity) {
        assertThatThrownBy(() -> new BuyNGetOneFreePromotion("C", quantity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUnrepresentableBundleSize() {
        assertThatThrownBy(() -> new BuyNGetOneFreePromotion("C", Integer.MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);
    }
}
