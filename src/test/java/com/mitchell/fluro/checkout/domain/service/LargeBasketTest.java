package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class LargeBasketTest {

    @Nested
    class BoundedSearch {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           Money expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            assertTimeout((Duration) dataValues.get("timeout"),
                    () -> assertThat(engine.calculate(basket, promotions)).isEqualTo(expected));
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            dataValues.put("scenario", "five million independent items");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(
                new Item("A", Money.ofPence(50)), 1_000_000,
                new Item("B", Money.ofPence(75)), 1_000_000,
                new Item("C", Money.ofPence(25)), 1_000_000,
                new Item("D", Money.ofPence(150)), 1_000_000,
                new Item("E", Money.ofPence(200)), 1_000_000)));
            dataValues.put("promotions", List.of(
                new MultiPricePromotion("B", 2, Money.ofPence(125)),
                new BuyNGetOneFreePromotion("C", 3),
                new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300))));
            dataValues.put("timeout", Duration.ofSeconds(5));
            cases[0] = tcb.addCase(Money.ofPence(431_250_000));

            dataValues.put("scenario", "twenty thousand deeply overlapping items");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(100)), 20_000)));
            dataValues.put("promotions", List.of(
                new MultiPricePromotion("A", 2, Money.ofPence(120)),
                new MultiPricePromotion("A", 3, Money.ofPence(160))));
            dataValues.put("timeout", Duration.ofSeconds(10));
            cases[1] = tcb.addCase(Money.ofPence(1_066_680));
            return cases;
        }
    }

    @Nested
    class BeyondSignedIntArithmetic {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           Money expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            assertThat(engine.calculate(basket, promotions)).isEqualTo(expected);
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(100)), Integer.MAX_VALUE)));
            dataValues.put("promotions", List.of(new MultiPricePromotion("A", 2, Money.ofPence(150))));
            cases[0] = tcb.addCase(Money.ofPence(161_061_273_550L));
            return cases;
        }
    }
}
