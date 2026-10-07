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

@DisplayName("Multiprice: B costs 75p, two cost 125p")
class MultiPricePromotionTest {

    @Nested
    @DisplayName("Bundle and remainder pricing with configurable SKUs")
    class Pricing {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("calculateDataProvider")
        @DisplayName("Prices bundles and remaining single items")
        void calculateTest(HashMap<String, Object> dataValues,
                           HashMap<String, Integer> expectedCalls, Money expected) {
            PricingEngine engine = new PricingEngine(new PromotionOptimizer());
            assertThat(engine.calculate((Basket) dataValues.get("basket"),
                    List.of((IPromotion) dataValues.get("promotion")))).isEqualTo(expected);
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", Basket.empty());
            dataDefaults.put("promotion", new MultiPricePromotion("B", 2, Money.ofPence(125)));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[8][];
            cases[0] = builder.addCase(Money.ZERO);
            dataValues.put("basket", basket(1));
            cases[1] = builder.addCase(Money.ofPence(75));
            dataValues.put("basket", basket(2));
            cases[2] = builder.addCase(Money.ofPence(125));
            dataValues.put("basket", basket(3));
            cases[3] = builder.addCase(Money.ofPence(200));
            dataValues.put("basket", basket(4));
            cases[4] = builder.addCase(Money.ofPence(250));
            dataValues.put("basket", basket(5));
            cases[5] = builder.addCase(Money.ofPence(325));
            dataValues.put("basket", basket(10));
            cases[6] = builder.addCase(Money.ofPence(625));
            dataValues.put("basket", new Basket(Map.of(new Item("APPLE", Money.ofPence(50)), 4)));
            dataValues.put("promotion", new MultiPricePromotion("APPLE", 3, Money.ofPence(130)));
            cases[7] = builder.addCase(Money.ofPence(180));
            return cases;
        }
    }

    @Nested
    @DisplayName("Eligible baskets retain their quantities")
    class EligibleBasket {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("applyDataProvider")
        @DisplayName("Describes one repeatable bundle without mutating the basket")
        void applyTest(HashMap<String, Object> dataValues,
                       HashMap<String, Integer> expectedCalls, PricingRule expected) {
            IPromotion promotion = (IPromotion) dataValues.get("promotion");
            Basket basket = (Basket) dataValues.get("basket");
            assertThat(promotion.apply(basket).rules()).containsExactly(expected);
            assertThat(basket.quantityOf((String) dataValues.get("sku")))
                    .isEqualTo((Integer) dataValues.get("originalQuantity"));
        }

        static Object[][] applyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("promotion", new MultiPricePromotion("B", 2, Money.ofPence(125)));
            dataDefaults.put("basket", basket(6));
            dataDefaults.put("sku", "B");
            dataDefaults.put("originalQuantity", 6);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = builder.addCase(new PricingRule(Map.of("B", 2), Money.ofPence(125)));
            return cases;
        }
    }

    @Nested
    @DisplayName("Empty and unrelated baskets")
    class IneligibleBasket {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("applyDataProvider")
        @DisplayName("Does not offer bundles for absent SKUs")
        void applyTest(HashMap<String, Object> dataValues,
                       HashMap<String, Integer> expectedCalls, PromotionResult expected) {
            IPromotion promotion = (IPromotion) dataValues.get("promotion");
            assertThat(promotion.apply((Basket) dataValues.get("basket"))).isEqualTo(expected);
        }

        static Object[][] applyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("promotion", new MultiPricePromotion("B", 2, Money.ofPence(125)));
            dataDefaults.put("basket", Basket.empty());
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = builder.addCase(PromotionResult.NONE);
            dataValues.put("basket", new Basket(Map.of(new Item("APPLE", Money.ofPence(50)), 4)));
            cases[1] = builder.addCase(PromotionResult.NONE);
            return cases;
        }
    }

    @Nested
    @DisplayName("Constructor validates bundle sizes")
    class InvalidConfiguration {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("constructorDataProvider")
        @DisplayName("Rejects zero and negative quantities")
        void constructorTest(HashMap<String, Object> dataValues,
                             HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new MultiPricePromotion((String) dataValues.get("sku"),
                    (Integer) dataValues.get("quantity"), (Money) dataValues.get("bundlePrice")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("sku", "B");
            dataDefaults.put("quantity", 0);
            dataDefaults.put("bundlePrice", Money.ofPence(125));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("quantity", -1);
            cases[1] = builder.addCase(IllegalArgumentException.class);
            return cases;
        }
    }

    private static Basket basket(int quantity) {
        return new Basket(Map.of(new Item("B", Money.ofPence(75)), quantity));
    }
}
