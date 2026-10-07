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

@DisplayName("Buy three C, get a fourth C free")
class BuyNGetOneFreePromotionTest {

    @Nested
    @DisplayName("Pricing complete groups, remainders and transaction prices")
    class Pricing {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("calculateDataProvider")
        @DisplayName("Charges paid items and consumes scanned free items")
        void calculateTest(HashMap<String, Object> dataValues,
                           HashMap<String, Integer> expectedCalls, Money expected) {
            PricingEngine engine = new PricingEngine(new PromotionOptimizer());
            assertThat(engine.calculate((Basket) dataValues.get("basket"),
                    List.of((IPromotion) dataValues.get("promotion")))).isEqualTo(expected);
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", Basket.empty());
            dataDefaults.put("promotion", new BuyNGetOneFreePromotion("C", 3));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[16][];

            cases[0] = builder.addCase(Money.ZERO);
            dataValues.put("basket", basket(1, 25));
            cases[1] = builder.addCase(Money.ofPence(25));
            dataValues.put("basket", basket(2, 25));
            cases[2] = builder.addCase(Money.ofPence(50));
            dataValues.put("basket", basket(3, 25));
            cases[3] = builder.addCase(Money.ofPence(75));
            dataValues.put("basket", basket(4, 25));
            cases[4] = builder.addCase(Money.ofPence(75));
            dataValues.put("basket", basket(5, 25));
            cases[5] = builder.addCase(Money.ofPence(100));
            dataValues.put("basket", basket(6, 25));
            cases[6] = builder.addCase(Money.ofPence(125));
            dataValues.put("basket", basket(7, 25));
            cases[7] = builder.addCase(Money.ofPence(150));
            dataValues.put("basket", basket(8, 25));
            cases[8] = builder.addCase(Money.ofPence(150));
            dataValues.put("basket", basket(9, 25));
            cases[9] = builder.addCase(Money.ofPence(175));
            dataValues.put("basket", basket(11, 25));
            cases[10] = builder.addCase(Money.ofPence(225));
            dataValues.put("basket", basket(12, 25));
            cases[11] = builder.addCase(Money.ofPence(225));
            dataValues.put("basket", basket(13, 25));
            cases[12] = builder.addCase(Money.ofPence(250));
            dataValues.put("basket", basket(4, 40));
            cases[13] = builder.addCase(Money.ofPence(120));
            dataValues.put("basket", basket(5, 25));
            dataValues.put("promotion", new BuyNGetOneFreePromotion("C", 1));
            cases[14] = builder.addCase(Money.ofPence(75));
            dataValues.put("basket", basket(4, 0));
            cases[15] = builder.addCase(Money.ZERO);
            return cases;
        }
    }

    @Nested
    @DisplayName("Eligibility requires the free item to have been scanned")
    class IneligibleBasket {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("applyDataProvider")
        @DisplayName("Does not create an unscanned fourth item")
        void applyTest(HashMap<String, Object> dataValues,
                       HashMap<String, Integer> expectedCalls, PromotionResult expected) {
            IPromotion promotion = (IPromotion) dataValues.get("promotion");
            assertThat(promotion.apply((Basket) dataValues.get("basket"))).isEqualTo(expected);
        }

        static Object[][] applyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("promotion", new BuyNGetOneFreePromotion("C", 3));
            dataDefaults.put("basket", basket(3, 25));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = builder.addCase(PromotionResult.NONE);
            return cases;
        }
    }

    @Nested
    @DisplayName("Eligible baskets expose one repeatable rule")
    class EligibleBasket {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("applyDataProvider")
        @DisplayName("Consumes four items for the price of three")
        void applyTest(HashMap<String, Object> dataValues,
                       HashMap<String, Integer> expectedCalls, PricingRule expected) {
            IPromotion promotion = (IPromotion) dataValues.get("promotion");
            assertThat(promotion.apply((Basket) dataValues.get("basket")).rules())
                    .containsExactly(expected);
        }

        static Object[][] applyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("promotion", new BuyNGetOneFreePromotion("C", 3));
            dataDefaults.put("basket", basket(8, 25));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = builder.addCase(new PricingRule(Map.of("C", 4), Money.ofPence(75)));
            return cases;
        }
    }

    @Nested
    @DisplayName("Constructor rejects invalid and overflowing bundle sizes")
    class InvalidConfiguration {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("constructorDataProvider")
        @DisplayName("Requires a positive representable paid quantity")
        void constructorTest(HashMap<String, Object> dataValues,
                             HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new BuyNGetOneFreePromotion(
                    (String) dataValues.get("sku"), (Integer) dataValues.get("paidQuantity")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("sku", "C");
            dataDefaults.put("paidQuantity", 0);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[3][];
            cases[0] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("paidQuantity", -1);
            cases[1] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("paidQuantity", Integer.MAX_VALUE);
            cases[2] = builder.addCase(ArithmeticException.class);
            return cases;
        }
    }

    private static Basket basket(int quantity, long unitPrice) {
        return new Basket(Map.of(new Item("C", Money.ofPence(unitPrice)), quantity));
    }
}
