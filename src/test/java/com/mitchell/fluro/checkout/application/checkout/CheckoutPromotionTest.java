package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutPromotionTest {

    @Nested
    @DisplayName("The exercise price list")
    class ExercisePriceList {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Combines all promotion types for the sample baskets")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Money expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");

            assertThat(checkout.getTotal()).isEqualTo(expected);
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[15][];

            dataValues.put("checkout", scannedCheckout(exerciseRules(), ""));
            cases[0] = builder.addCase(Money.ZERO);
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "A"));
            cases[1] = builder.addCase(Money.ofPence(50));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "B"));
            cases[2] = builder.addCase(Money.ofPence(75));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "C"));
            cases[3] = builder.addCase(Money.ofPence(25));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "D"));
            cases[4] = builder.addCase(Money.ofPence(150));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "E"));
            cases[5] = builder.addCase(Money.ofPence(200));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "BB"));
            cases[6] = builder.addCase(Money.ofPence(125));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "CCC"));
            cases[7] = builder.addCase(Money.ofPence(75));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "CCCC"));
            cases[8] = builder.addCase(Money.ofPence(75));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "DE"));
            cases[9] = builder.addCase(Money.ofPence(300));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "BAB"));
            cases[10] = builder.addCase(Money.ofPence(175));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "ABCDE"));
            cases[11] = builder.addCase(Money.ofPence(450));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "ABBCCCCDE"));
            cases[12] = builder.addCase(Money.ofPence(550));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "AABBBBCCCCCCCCDDEE"));
            cases[13] = builder.addCase(Money.ofPence(1100));
            dataValues.put("checkout", scannedCheckout(exerciseRules(), "BBBCCCCCDDE"));
            cases[14] = builder.addCase(Money.ofPence(750));
            return cases;
        }
    }

    @Nested
    @DisplayName("Incremental promotion totals")
    class IncrementalPricing {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Reprices after every scan without consuming offers")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          IncrementalExpected expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");
            String bundleSku = (String) dataValues.get("bundleSku");
            String regularSku = (String) dataValues.get("regularSku");

            checkout.scan(bundleSku);
            assertThat(checkout.getTotal()).isEqualTo(expected.firstTotal());
            checkout.scan(regularSku);
            assertThat(checkout.getTotal()).isEqualTo(expected.secondTotal());
            checkout.scan(bundleSku);
            assertThat(checkout.getTotal()).isEqualTo(expected.bundleTotal());
            assertThat(checkout.getTotal()).isEqualTo(expected.bundleTotal());
            checkout.scan(bundleSku);
            assertThat(checkout.getTotal()).isEqualTo(expected.finalTotal());
            assertThat(checkout.getBasket().quantityOf(bundleSku)).isEqualTo(expected.bundleQuantity());
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            dataDefaults.put("bundleSku", "B");
            dataDefaults.put("regularSku", "A");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("checkout", scannedCheckout(exerciseRules(), ""));
            cases[0] = builder.addCase(new IncrementalExpected(
                    Money.ofPence(75), Money.ofPence(125), Money.ofPence(175), Money.ofPence(250), 3));
            return cases;
        }
    }

    @Nested
    @DisplayName("Scan-order independence across all 50 deterministic shuffles")
    class ShuffledScanOrder {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Prices the same basket for seeds 0 through 49")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Money expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");

            assertThat(checkout.getTotal()).as("shuffle seed %s", dataValues.get("seed")).isEqualTo(expected);
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            dataDefaults.put("seed", 0);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[50][];

            for (int seed = 0; seed < cases.length; seed++) {
                dataValues.put("seed", seed);
                dataValues.put("checkout", shuffledCheckout(seed));
                cases[seed] = builder.addCase(Money.ofPence(1350));
            }
            return cases;
        }
    }

    @Nested
    @DisplayName("Multiprice competing with buy-three-get-one-free")
    class MultipriceAgainstFreeItem {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Selects the cheaper multiprice offer for four C items")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Money expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");

            assertThat(checkout.getTotal()).isEqualTo(expected);
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            PricingRules rules = new PricingRules(items(), List.of(
                    new BuyNGetOneFreePromotion("C", 3),
                    new MultiPricePromotion("C", 2, Money.ofPence(30))));
            dataValues.put("checkout", scannedCheckout(rules, "CCCC"));
            cases[0] = builder.addCase(Money.ofPence(60));
            return cases;
        }
    }

    @Nested
    @DisplayName("Meal deals competing with free items")
    class MealDealAgainstFreeItem {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Chooses the cheaper allocation for each custom meal price")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Money expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");

            assertThat(checkout.getTotal()).isEqualTo(expected);
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];

            dataValues.put("checkout", scannedCheckout(mealAndFreeItemRules(Money.ofPence(155)), "CCCCD"));
            cases[0] = builder.addCase(Money.ofPence(225));
            dataValues.put("checkout", scannedCheckout(mealAndFreeItemRules(Money.ofPence(100)), "CCCCD"));
            cases[1] = builder.addCase(Money.ofPence(175));
            return cases;
        }
    }

    @Nested
    @DisplayName("Meal deals competing with multiprice bundles")
    class MealDealAgainstMultiprice {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Allocates shared meal items to the cheapest combination of offers")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Money expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");

            assertThat(checkout.getTotal()).isEqualTo(expected);
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];

            dataValues.put("checkout", scannedCheckout(mealAndMultipriceRules(), "DDE"));
            cases[0] = builder.addCase(Money.ofPence(400));
            dataValues.put("checkout", scannedCheckout(mealAndMultipriceRules(), "DDEE"));
            cases[1] = builder.addCase(Money.ofPence(600));
            return cases;
        }
    }

    private static List<Item> items() {
        return List.of(
                new Item("A", Money.ofPence(50)), new Item("B", Money.ofPence(75)),
                new Item("C", Money.ofPence(25)), new Item("D", Money.ofPence(150)),
                new Item("E", Money.ofPence(200)));
    }

    private static PricingRules exerciseRules() {
        return new PricingRules(items(), List.of(
                new MultiPricePromotion("B", 2, Money.ofPence(125)),
                new BuyNGetOneFreePromotion("C", 3),
                new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300))));
    }

    private static PricingRules mealAndFreeItemRules(Money mealPrice) {
        return new PricingRules(items(), List.of(
                new BuyNGetOneFreePromotion("C", 3),
                new MealDealPromotion(Map.of("C", 1, "D", 1), mealPrice)));
    }

    private static PricingRules mealAndMultipriceRules() {
        return new PricingRules(items(), List.of(
                new MultiPricePromotion("D", 2, Money.ofPence(200)),
                new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300))));
    }

    private static ICheckout scannedCheckout(PricingRules rules, String scanned) {
        ICheckout checkout = new Checkout(rules, new PricingEngine(new PromotionOptimizer()));
        scanned.chars().mapToObj(character -> String.valueOf((char) character)).forEach(checkout::scan);
        return checkout;
    }

    private static ICheckout shuffledCheckout(int seed) {
        List<String> scanned = new ArrayList<>("AABBBBBCCCCCCCCCDDDEE".chars()
                .mapToObj(character -> String.valueOf((char) character)).toList());
        Collections.shuffle(scanned, new Random(seed));
        ICheckout checkout = new Checkout(exerciseRules(), new PricingEngine(new PromotionOptimizer()));
        scanned.forEach(checkout::scan);
        return checkout;
    }

    record IncrementalExpected(Money firstTotal, Money secondTotal, Money bundleTotal,
                               Money finalTotal, int bundleQuantity) {
    }
}
