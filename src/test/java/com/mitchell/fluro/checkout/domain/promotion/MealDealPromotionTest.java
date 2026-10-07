package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Meal deal: one D and one E cost 300p")
class MealDealPromotionTest {

    @Nested
    @DisplayName("Meal pricing, remainders and configurable quantities")
    class Pricing {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("calculateDataProvider")
        @DisplayName("Pairs available items and leaves remainders at unit price")
        void calculateTest(HashMap<String, Object> dataValues,
                           HashMap<String, Integer> expectedCalls, Money expected) {
            PricingEngine engine = new PricingEngine(new PromotionOptimizer());
            assertThat(engine.calculate((Basket) dataValues.get("basket"),
                    List.of((IPromotion) dataValues.get("promotion")))).isEqualTo(expected);
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", Basket.empty());
            dataDefaults.put("promotion", new MealDealPromotion(
                    Map.of("D", 1, "E", 1), Money.ofPence(300)));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[12][];
            cases[0] = builder.addCase(Money.ZERO);
            dataValues.put("basket", basket(1, 0));
            cases[1] = builder.addCase(Money.ofPence(150));
            dataValues.put("basket", basket(0, 1));
            cases[2] = builder.addCase(Money.ofPence(200));
            dataValues.put("basket", basket(1, 1));
            cases[3] = builder.addCase(Money.ofPence(300));
            dataValues.put("basket", basket(2, 2));
            cases[4] = builder.addCase(Money.ofPence(600));
            dataValues.put("basket", basket(3, 1));
            cases[5] = builder.addCase(Money.ofPence(600));
            dataValues.put("basket", basket(1, 3));
            cases[6] = builder.addCase(Money.ofPence(700));
            dataValues.put("basket", basket(3, 2));
            cases[7] = builder.addCase(Money.ofPence(750));
            dataValues.put("basket", basket(2, 3));
            cases[8] = builder.addCase(Money.ofPence(800));
            dataValues.put("basket", basket(5, 5));
            cases[9] = builder.addCase(Money.ofPence(1500));
            dataValues.put("basket", basketWithUnrelatedItems());
            cases[10] = builder.addCase(Money.ofPence(600));
            dataValues.put("basket", new Basket(Map.of(
                    new Item("D", Money.ofPence(150)), 5,
                    new Item("E", Money.ofPence(200)), 2,
                    new Item("SIDE", Money.ofPence(50)), 7)));
            dataValues.put("promotion", new MealDealPromotion(
                    Map.of("D", 2, "E", 1, "SIDE", 3), Money.ofPence(500)));
            cases[11] = builder.addCase(Money.ofPence(1200));
            return cases;
        }
    }

    @Nested
    @DisplayName("Meal rule excludes unrelated items")
    class EligibleBasket {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("applyDataProvider")
        @DisplayName("Offers one repeatable D and E meal")
        void applyTest(HashMap<String, Object> dataValues,
                       HashMap<String, Integer> expectedCalls, PricingRule expected) {
            IPromotion promotion = (IPromotion) dataValues.get("promotion");
            assertThat(promotion.apply((Basket) dataValues.get("basket")).rules())
                    .containsExactly(expected);
        }

        static Object[][] applyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("promotion", new MealDealPromotion(
                    Map.of("D", 1, "E", 1), Money.ofPence(300)));
            dataDefaults.put("basket", basketWithUnrelatedItems());
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = builder.addCase(new PricingRule(
                    Map.of("D", 1, "E", 1), Money.ofPence(300)));
            return cases;
        }
    }

    @Nested
    @DisplayName("Constructor snapshots mutable configuration")
    class ConfigurationSnapshot {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("constructorDataProvider")
        @DisplayName("Clearing the original quantities does not change the deal")
        void constructorTest(HashMap<String, Object> dataValues,
                             HashMap<String, Integer> expectedCalls, Money expected) {
            Map<String, Integer> quantities = quantities(dataValues);
            IPromotion promotion = new MealDealPromotion(quantities, (Money) dataValues.get("dealPrice"));
            quantities.clear();
            PricingEngine engine = new PricingEngine(new PromotionOptimizer());
            assertThat(engine.calculate((Basket) dataValues.get("basket"), List.of(promotion)))
                    .isEqualTo(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", Map.of("D", 1, "E", 1));
            dataDefaults.put("dealPrice", Money.ofPence(300));
            dataDefaults.put("basket", basket(1, 1));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            dataValues.put("quantities", new HashMap<>(Map.of("D", 1, "E", 1)));
            cases[0] = builder.addCase(Money.ofPence(300));
            return cases;
        }
    }

    @Nested
    @DisplayName("Constructor requires multiple different SKUs")
    class SingleSkuConfiguration {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("constructorDataProvider")
        @DisplayName("Rejects a single SKU with an explanatory message")
        void constructorTest(HashMap<String, Object> dataValues,
                             HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new MealDealPromotion(
                    quantities(dataValues), (Money) dataValues.get("dealPrice")))
                    .isInstanceOf(expected).hasMessageContaining((String) dataValues.get("message"));
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", Map.of("D", 2));
            dataDefaults.put("dealPrice", Money.ofPence(200));
            dataDefaults.put("message", "different SKUs");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = builder.addCase(IllegalArgumentException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Constructor requires positive quantities")
    class InvalidQuantity {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("constructorDataProvider")
        @DisplayName("Rejects zero required items")
        void constructorTest(HashMap<String, Object> dataValues,
                             HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new MealDealPromotion(
                    quantities(dataValues), (Money) dataValues.get("dealPrice")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", Map.of("D", 1, "E", 0));
            dataDefaults.put("dealPrice", Money.ofPence(200));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = builder.addCase(IllegalArgumentException.class);
            return cases;
        }
    }

    private static Basket basket(int dCount, int eCount) {
        Map<Item, Integer> quantities = new HashMap<>();
        if (dCount > 0) {
            quantities.put(new Item("D", Money.ofPence(150)), dCount);
        }
        if (eCount > 0) {
            quantities.put(new Item("E", Money.ofPence(200)), eCount);
        }
        return new Basket(quantities);
    }

    private static Basket basketWithUnrelatedItems() {
        return new Basket(Map.of(new Item("D", Money.ofPence(150)), 2,
                new Item("E", Money.ofPence(200)), 1,
                new Item("A", Money.ofPence(50)), 3));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> quantities(HashMap<String, Object> dataValues) {
        return (Map<String, Integer>) dataValues.get("quantities");
    }
}
