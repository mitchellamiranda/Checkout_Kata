package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Meal deal: one D and one E cost 300p")
class MealDealPromotionTest {

    private static final Item D = new Item("D", Money.ofPence(150));
    private static final Item E = new Item("E", Money.ofPence(200));
    private final Promotion promotion = new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300));

    @ParameterizedTest(name = "{0} D and {1} E cost {2} pence")
    @CsvSource({"0, 0, 0", "1, 0, 150", "0, 1, 200", "1, 1, 300", "2, 2, 600",
            "3, 1, 600", "1, 3, 700", "3, 2, 750", "2, 3, 800", "5, 5, 1500"})
    void pairsAvailableItemsAndChargesRemainders(int dCount, int eCount, long expected) {
        Map<Item, Integer> quantities = new HashMap<>();
        if (dCount > 0) {
            quantities.put(D, dCount);
        }
        if (eCount > 0) {
            quantities.put(E, eCount);
        }
        assertThat(new PricingEngine().calculate(new Basket(quantities), List.of(promotion)))
                .isEqualTo(Money.ofPence(expected));
    }

    @Test
    void leavesUnrelatedItemsAtTheirUnitPrice() {
        Basket basket = new Basket(Map.of(D, 2, E, 1, new Item("A", Money.ofPence(50)), 3));
        assertThat(new PricingEngine().calculate(basket, List.of(promotion))).isEqualTo(Money.ofPence(600));
        assertThat(promotion.apply(basket).rules())
                .containsExactly(new PricingRule(Map.of("D", 1, "E", 1), Money.ofPence(300)));
    }

    @Test
    void supportsDifferentQuantitiesAndMoreThanTwoSkus() {
        Item side = new Item("SIDE", Money.ofPence(50));
        Promotion familyMeal = new MealDealPromotion(Map.of("D", 2, "E", 1, "SIDE", 3), Money.ofPence(500));
        Basket basket = new Basket(Map.of(D, 5, E, 2, side, 7));
        assertThat(new PricingEngine().calculate(basket, List.of(familyMeal))).isEqualTo(Money.ofPence(1200));
    }

    @Test
    void copiesTheConfiguration() {
        Map<String, Integer> quantities = new HashMap<>(Map.of("D", 1, "E", 1));
        Promotion copied = new MealDealPromotion(quantities, Money.ofPence(300));
        quantities.clear();
        assertThat(new PricingEngine().calculate(new Basket(Map.of(D, 1, E, 1)), List.of(copied)))
                .isEqualTo(Money.ofPence(300));
    }

    @Test
    void rejectsSingleSkuAndInvalidQuantities() {
        assertThatThrownBy(() -> new MealDealPromotion(Map.of("D", 2), Money.ofPence(200)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("different SKUs");
        assertThatThrownBy(() -> new MealDealPromotion(Map.of("D", 1, "E", 0), Money.ofPence(200)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
