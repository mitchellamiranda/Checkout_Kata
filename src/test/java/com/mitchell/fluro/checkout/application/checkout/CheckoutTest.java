package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutTest {

    @Nested
    @DisplayName("Unit pricing in any scan order")
    class UnitPricing {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Totals empty, single-item and reordered baskets")
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
            Object[][] cases = new Object[11][];

            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), ""));
            cases[0] = builder.addCase(Money.ZERO);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            cases[1] = builder.addCase(Money.ofPence(50));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "B"));
            cases[2] = builder.addCase(Money.ofPence(75));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "C"));
            cases[3] = builder.addCase(Money.ofPence(25));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "D"));
            cases[4] = builder.addCase(Money.ofPence(150));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "E"));
            cases[5] = builder.addCase(Money.ofPence(200));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "AAA"));
            cases[6] = builder.addCase(Money.ofPence(150));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "ABCDE"));
            cases[7] = builder.addCase(Money.ofPence(500));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "BAB"));
            cases[8] = builder.addCase(Money.ofPence(200));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "BBA"));
            cases[9] = builder.addCase(Money.ofPence(200));
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "ABB"));
            cases[10] = builder.addCase(Money.ofPence(200));
            return cases;
        }
    }

    @Nested
    @DisplayName("Repeatable totals and immutable snapshots")
    class TransactionLifecycle {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Scanning continues after repeated totals without changing the old snapshot")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          LifecycleExpected expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");
            String nextSku = (String) dataValues.get("nextSku");
            Basket snapshot = checkout.getBasket();

            assertThat(checkout.getTotal()).isEqualTo(expected.initialTotal());
            assertThat(checkout.getTotal()).isEqualTo(expected.initialTotal());
            checkout.scan(nextSku);
            assertThat(checkout.getTotal()).isEqualTo(expected.finalTotal());
            assertThat(snapshot.quantityOf(nextSku)).isEqualTo(expected.snapshotQuantity());
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            dataDefaults.put("nextSku", "B");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            cases[0] = builder.addCase(new LifecycleExpected(Money.ofPence(50), Money.ofPence(125), 0));
            return cases;
        }
    }

    @Nested
    @DisplayName("Invalid scans")
    class InvalidScans {

        @ParameterizedTest
        @MethodSource("scanDataProvider")
        @DisplayName("Rejects invalid SKUs without replacing the basket")
        void scanTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                      Class<? extends Throwable> expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");
            Basket before = checkout.getBasket();

            assertThatThrownBy(() -> checkout.scan((String) dataValues.get("sku"))).isInstanceOf(expected);
            assertThat(checkout.getBasket()).isSameAs(before);
        }

        static Object[][] scanDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            dataDefaults.put("sku", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[7][];

            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            cases[0] = builder.addCase(NullPointerException.class);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            dataValues.put("sku", "");
            cases[1] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            dataValues.put("sku", " ");
            cases[2] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            dataValues.put("sku", "a");
            cases[3] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            dataValues.put("sku", "Z");
            cases[4] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            dataValues.put("sku", " A");
            cases[5] = builder.addCase(IllegalArgumentException.class);
            dataValues.put("checkout", scannedCheckout(rules(), pricingEngine(), "A"));
            dataValues.put("sku", "A ");
            cases[6] = builder.addCase(IllegalArgumentException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Transaction-specific prices and baskets")
    class IsolatedTransactions {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("A revised catalogue does not affect another transaction")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          TotalsExpected expected) {
            ICheckout original = (ICheckout) dataValues.get("original");
            ICheckout revised = (ICheckout) dataValues.get("revised");

            assertThat(original.getTotal()).isEqualTo(expected.firstTotal());
            assertThat(revised.getTotal()).isEqualTo(expected.secondTotal());
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("original", null);
            dataDefaults.put("revised", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            IPricingEngine engine = pricingEngine();
            dataValues.put("original", scannedCheckout(rules(), engine, "A"));
            dataValues.put("revised", scannedCheckout(
                    new PricingRules(List.of(new Item("A", Money.ofPence(10)))), engine, "AA"));
            cases[0] = builder.addCase(new TotalsExpected(Money.ofPence(50), Money.ofPence(20)));
            return cases;
        }
    }

    @Nested
    @DisplayName("Required constructor dependencies")
    class MissingDependencies {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Rejects missing rules or pricing engine")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new Checkout(
                    (PricingRules) dataValues.get("rules"), (IPricingEngine) dataValues.get("pricingEngine")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rules", null);
            dataDefaults.put("pricingEngine", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];

            dataValues.put("pricingEngine", pricingEngine());
            cases[0] = builder.addCase(NullPointerException.class);
            dataValues.put("rules", rules());
            cases[1] = builder.addCase(NullPointerException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Constructor-injected pricing")
    class InjectedPricing {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Passes each current snapshot and the same rules to the replacement engine")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          TotalsExpected expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");
            PricingRules pricingRules = (PricingRules) dataValues.get("rules");
            List<?> pricedPromotions = (List<?>) dataValues.get("pricedPromotions");
            Basket first = checkout.getBasket();

            assertThat(checkout.getTotal()).isEqualTo(expected.firstTotal());
            checkout.scan((String) dataValues.get("nextSku"));
            assertThat(checkout.getTotal()).isEqualTo(expected.secondTotal());
            assertThat(dataValues.get("pricedBaskets")).isEqualTo(List.of(first, checkout.getBasket()));
            assertThat(pricedPromotions.get(0)).isSameAs(pricingRules.promotions());
            assertThat(pricedPromotions.get(1)).isSameAs(pricingRules.promotions());
            assertThat(((AtomicInteger) dataValues.get("calculateCalls")).get())
                    .isEqualTo(expectedCalls.get("calculate"));
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            dataDefaults.put("rules", null);
            dataDefaults.put("nextSku", "B");
            dataDefaults.put("pricedBaskets", null);
            dataDefaults.put("pricedPromotions", null);
            dataDefaults.put("calculateCalls", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("calculate", 2);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            PricingRules pricingRules = rules();
            dataValues.put("rules", pricingRules);
            dataValues.put("checkout", scannedCheckout(pricingRules, recordingEngine(dataValues), "A"));
            cases[0] = builder.addCase(new TotalsExpected(Money.ofPence(50), Money.ofPence(125)));
            return cases;
        }
    }

    @Nested
    @DisplayName("Injected pricing failures")
    class PricingFailures {

        @ParameterizedTest
        @MethodSource("getTotalDataProvider")
        @DisplayName("Propagates the exact engine failure without replacing the basket")
        void getTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          IllegalStateException expected) {
            ICheckout checkout = (ICheckout) dataValues.get("checkout");
            Basket before = checkout.getBasket();

            assertThatThrownBy(checkout::getTotal).isSameAs(expected);
            assertThat(checkout.getBasket()).isSameAs(before);
            assertThat(((AtomicInteger) dataValues.get("calculateCalls")).get())
                    .isEqualTo(expectedCalls.get("calculate"));
        }

        static Object[][] getTotalDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("checkout", null);
            dataDefaults.put("calculateCalls", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("calculate", 1);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            IllegalStateException failure = new IllegalStateException("Pricing unavailable");
            AtomicInteger calculateCalls = new AtomicInteger();
            IPricingEngine failingEngine = (basket, promotions) -> {
                calculateCalls.incrementAndGet();
                throw failure;
            };
            dataValues.put("calculateCalls", calculateCalls);
            dataValues.put("checkout", scannedCheckout(rules(), failingEngine, "A"));
            cases[0] = builder.addCase(failure);
            return cases;
        }
    }

    private static PricingRules rules() {
        return new PricingRules(List.of(
                new Item("A", Money.ofPence(50)),
                new Item("B", Money.ofPence(75)),
                new Item("C", Money.ofPence(25)),
                new Item("D", Money.ofPence(150)),
                new Item("E", Money.ofPence(200))));
    }

    private static IPricingEngine pricingEngine() {
        return new PricingEngine(new PromotionOptimizer());
    }

    private static ICheckout scannedCheckout(PricingRules rules, IPricingEngine engine, String scanned) {
        ICheckout checkout = new Checkout(rules, engine);
        scanned.chars().mapToObj(character -> String.valueOf((char) character)).forEach(checkout::scan);
        return checkout;
    }

    private static IPricingEngine recordingEngine(HashMap<String, Object> dataValues) {
        List<Basket> pricedBaskets = new ArrayList<>();
        List<List<IPromotion>> pricedPromotions = new ArrayList<>();
        AtomicInteger calculateCalls = new AtomicInteger();
        IPricingEngine delegate = pricingEngine();
        dataValues.put("pricedBaskets", pricedBaskets);
        dataValues.put("pricedPromotions", pricedPromotions);
        dataValues.put("calculateCalls", calculateCalls);
        return (basket, promotions) -> {
            calculateCalls.incrementAndGet();
            pricedBaskets.add(basket);
            pricedPromotions.add(promotions);
            return delegate.calculate(basket, promotions);
        };
    }

    record LifecycleExpected(Money initialTotal, Money finalTotal, int snapshotQuantity) {
    }

    record TotalsExpected(Money firstTotal, Money secondTotal) {
    }
}
