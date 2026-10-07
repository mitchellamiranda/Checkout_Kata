package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class LargeBasketTest {

    private final IPricingEngine engine = new PricingEngine(new PromotionOptimizer());

    @Test
    void pricesFiveMillionItemsWithoutEnumeratingIndependentCombinations() {
        Basket basket = new Basket(Map.of(
                new Item("A", Money.ofPence(50)), 1_000_000,
                new Item("B", Money.ofPence(75)), 1_000_000,
                new Item("C", Money.ofPence(25)), 1_000_000,
                new Item("D", Money.ofPence(150)), 1_000_000,
                new Item("E", Money.ofPence(200)), 1_000_000));
        List<IPromotion> promotions = List.of(
                new MultiPricePromotion("B", 2, Money.ofPence(125)),
                new BuyNGetOneFreePromotion("C", 3),
                new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300)));
        assertTimeout(Duration.ofSeconds(5),
                () -> assertThat(engine.calculate(basket, promotions)).isEqualTo(Money.ofPence(431_250_000)));
    }

    @Test
    void searchesDeeplyOverlappingOffersWithoutRecursion() {
        Basket basket = new Basket(Map.of(new Item("A", Money.ofPence(100)), 20_000));
        List<IPromotion> promotions = List.of(
                new MultiPricePromotion("A", 2, Money.ofPence(120)),
                new MultiPricePromotion("A", 3, Money.ofPence(160)));
        assertTimeout(Duration.ofSeconds(10),
                () -> assertThat(engine.calculate(basket, promotions)).isEqualTo(Money.ofPence(1_066_680)));
    }

    @Test
    void supportsQuantitiesAndTotalsBeyondSignedIntArithmetic() {
        Basket basket = new Basket(Map.of(new Item("A", Money.ofPence(100)), Integer.MAX_VALUE));
        IPromotion pairs = new MultiPricePromotion("A", 2, Money.ofPence(150));
        assertThat(engine.calculate(basket, List.of(pairs))).isEqualTo(Money.ofPence(161_061_273_550L));
    }
}
