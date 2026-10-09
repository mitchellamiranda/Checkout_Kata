package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
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

class CheckoutFactoryTest {

    @Nested
    @DisplayName("Isolated transactions with caller-supplied pricing")
    class TransactionCreation {

        @ParameterizedTest
        @MethodSource("createDataProvider")
        @DisplayName("Creates distinct baskets sharing the supplied rules and injected engine")
        void createTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                        TotalsExpected expected) {
            ICheckoutFactory factory = (ICheckoutFactory) dataValues.get("factory");
            PricingRules rules = (PricingRules) dataValues.get("rules");
            List<?> pricedPromotions = (List<?>) dataValues.get("pricedPromotions");

            ICheckout first = factory.create(rules);
            ICheckout second = factory.create(rules);

            assertThat(first).isNotSameAs(second);
            first.scan((String) dataValues.get("sku"));
            assertThat(first.getTotal()).isEqualTo(expected.firstTotal());
            assertThat(second.getTotal()).isEqualTo(expected.secondTotal());
            assertThat(dataValues.get("pricedBaskets")).isEqualTo(List.of(first.getBasket(), second.getBasket()));
            assertThat(pricedPromotions.get(0)).isSameAs(rules.promotions());
            assertThat(pricedPromotions.get(1)).isSameAs(rules.promotions());
            assertThat(((AtomicInteger) dataValues.get("calculateCalls")).get())
                    .isEqualTo(expectedCalls.get("calculate"));
        }

        static Object[][] createDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("factory", null);
            dataDefaults.put("rules", null);
            dataDefaults.put("sku", "A");
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

            PricingRules rules = rules();
            dataValues.put("rules", rules);
            dataValues.put("factory", new CheckoutFactory(recordingEngine(dataValues)));
            cases[0] = builder.addCase(new TotalsExpected(Money.ofPence(50), Money.ZERO));
            return cases;
        }
    }

    @Nested
    @DisplayName("Each transaction retains its own prices, promotions and catalogue")
    class ChangingRules {

        @ParameterizedTest
        @MethodSource("createDataProvider")
        void createTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                        TotalsExpected expected) {
            ICheckoutFactory factory = new CheckoutFactory(pricingEngine());
            PricingRules originalRules = (PricingRules) dataValues.get("originalRules");
            PricingRules revisedRules = (PricingRules) dataValues.get("revisedRules");
            String revisedSku = (String) dataValues.get("revisedSku");
            ICheckout first = factory.create(originalRules);
            first.scan("A");
            first.scan("A");
            assertThat(first.getTotal()).isEqualTo(expected.firstTotal());

            ICheckout second = factory.create(revisedRules);
            assertThat(second.getBasket()).isEqualTo(Basket.empty());
            second.scan(revisedSku);
            second.scan(revisedSku);

            assertThat(second.getTotal()).isEqualTo(expected.secondTotal());
            assertThat(first.getTotal()).isEqualTo(expected.firstTotal());
            first.scan("A");
            first.scan("A");
            assertThat(first.getTotal()).isEqualTo(expected.firstTotal().multiply(2));
            assertThat(second.getTotal()).isEqualTo(expected.secondTotal());
            if (!revisedSku.equals("A")) {
                assertThatThrownBy(() -> first.scan(revisedSku))
                        .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown SKU: " + revisedSku);
                assertThatThrownBy(() -> second.scan("A"))
                        .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown SKU: A");
            }
        }

        static Object[][] createDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("originalRules", rules());
            dataDefaults.put("revisedSku", "A");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[4][];

            dataValues.put("revisedRules", new PricingRules(List.of(new Item("A", Money.ofPence(60)))));
            cases[0] = builder.addCase(new TotalsExpected(Money.ofPence(100), Money.ofPence(120)));
            PricingRules promotionalRules = new PricingRules(List.of(new Item("A", Money.ofPence(50))),
                    List.of(new MultiPricePromotion("A", 2, Money.ofPence(75))));
            dataValues.put("revisedRules", promotionalRules);
            cases[1] = builder.addCase(new TotalsExpected(Money.ofPence(100), Money.ofPence(75)));
            dataValues.put("originalRules", promotionalRules);
            dataValues.put("revisedRules", rules());
            cases[2] = builder.addCase(new TotalsExpected(Money.ofPence(75), Money.ofPence(100)));
            dataValues.put("revisedSku", "X");
            dataValues.put("revisedRules", new PricingRules(List.of(new Item("X", Money.ofPence(42)))));
            cases[3] = builder.addCase(new TotalsExpected(Money.ofPence(100), Money.ofPence(84)));
            return cases;
        }
    }

    @Nested
    @DisplayName("Rules are required for every transaction")
    class MissingRules {

        @ParameterizedTest
        @MethodSource("createDataProvider")
        void createTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                        String expected) {
            ICheckoutFactory factory = (ICheckoutFactory) dataValues.get("factory");
            assertThatThrownBy(() -> factory.create((PricingRules) dataValues.get("rules")))
                    .isInstanceOf(NullPointerException.class).hasMessage(expected);
            assertThat(((AtomicInteger) dataValues.get("calculateCalls")).get())
                    .isEqualTo(expectedCalls.get("calculate"));
        }

        static Object[][] createDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rules", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("calculate", 0);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            dataValues.put("factory", new CheckoutFactory(recordingEngine(dataValues)));
            return new Object[][]{builder.addCase("Pricing rules are required")};
        }
    }

    @Nested
    @DisplayName("Required factory dependencies")
    class MissingDependencies {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Rejects a missing pricing engine")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new CheckoutFactory(
                    (IPricingEngine) dataValues.get("pricingEngine")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("pricingEngine", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            cases[0] = builder.addCase(NullPointerException.class);
            return cases;
        }
    }

    private static PricingRules rules() {
        return new PricingRules(List.of(new Item("A", Money.ofPence(50))));
    }

    private static IPricingEngine pricingEngine() {
        return new PricingEngine(new PromotionOptimizer());
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

    record TotalsExpected(Money firstTotal, Money secondTotal) {
    }
}
