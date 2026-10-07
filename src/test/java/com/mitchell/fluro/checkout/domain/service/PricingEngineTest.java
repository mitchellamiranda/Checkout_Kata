package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.promotion.PromotionResult;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingEngineTest {

    @Nested
    class CandidateAllocation {
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
            Object[][] cases = new Object[6][];
            Item a = new Item("A", Money.ofPence(100));
            Item b = new Item("B", Money.ofPence(100));

            dataValues.put("scenario", "does not spend an item twice across different offers");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 2, b, 1)));
            dataValues.put("promotions", List.of(offer(Map.of("A", 2), 100),
                    offer(Map.of("A", 1, "B", 1), 75)));
            cases[0] = tcb.addCase(Money.ofPence(175));

            dataValues.put("scenario", "combines different offers for the same SKU");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 5)));
            dataValues.put("promotions", List.of(offer(Map.of("A", 2), 120), offer(Map.of("A", 3), 160)));
            cases[1] = tcb.addCase(Money.ofPence(280));

            dataValues.put("scenario", "keeps unit pricing for more expensive or equal offers");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 2)));
            dataValues.put("promotions", List.of(offer(Map.of("A", 2), 300), offer(Map.of("A", 2), 200)));
            cases[2] = tcb.addCase(Money.ofPence(200));

            IPromotion freePair = offer(Map.of("A", 2), 0);
            dataValues.put("scenario", "accepts zero prices and deduplicates identical rules");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 5)));
            dataValues.put("promotions", List.of(freePair, freePair));
            cases[3] = tcb.addCase(Money.ofPence(100));

            IPromotion alternativeBundles = ignored -> new PromotionResult(List.of(
                    new PricingRule(Map.of("A", 2), Money.ofPence(120)),
                    new PricingRule(Map.of("A", 3), Money.ofPence(160))));
            dataValues.put("scenario", "custom strategy returns multiple alternatives");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 5)));
            dataValues.put("promotions", List.of(alternativeBundles));
            cases[4] = tcb.addCase(Money.ofPence(280));

            IPromotion infeasible = ignored -> new PromotionResult(List.of(
                    new PricingRule(Map.of("A", 2), Money.ZERO)));
            dataValues.put("scenario", "ignores an infeasible candidate for an existing SKU");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 1)));
            dataValues.put("promotions", List.of(infeasible));
            cases[5] = tcb.addCase(a.unitPrice());
            return cases;
        }
    }

    @Nested
    class PromotionOrderIndependence {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @DisplayName("Finds the global optimum regardless of promotion order or bridged groups")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           Money expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            List<IPromotion> reversedPromotions = (List<IPromotion>) dataValues.get("reversedPromotions");
            assertThat(engine.calculate(basket, promotions)).isEqualTo(expected);
            assertThat(engine.calculate(basket, reversedPromotions)).isEqualTo(expected);
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            Item a = new Item("A", Money.ofPence(100));
            IPromotion fourFor250 = offer(Map.of("A", 4), 250);
            IPromotion threeFor200 = offer(Map.of("A", 3), 200);
            dataValues.put("scenario", "global optimum rather than biggest discount first");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 6)));
            dataValues.put("promotions", List.of(fourFor250, threeFor200));
            dataValues.put("reversedPromotions", List.of(threeFor200, fourFor250));
            cases[0] = tcb.addCase(Money.ofPence(400));

            Item b = new Item("B", Money.ofPence(100));
            Item c = new Item("C", Money.ofPence(100));
            Item d = new Item("D", Money.ofPence(100));
            IPromotion ab = offer(Map.of("A", 1, "B", 1), 150);
            IPromotion cd = offer(Map.of("C", 1, "D", 1), 150);
            IPromotion bc = offer(Map.of("B", 1, "C", 1), 1);
            dataValues.put("scenario", "merges previously separate groups when an offer bridges them");
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(a, 1, b, 1, c, 1, d, 1)));
            dataValues.put("promotions", List.of(ab, cd, bc));
            dataValues.put("reversedPromotions", List.of(bc, cd, ab));
            cases[1] = tcb.addCase(Money.ofPence(201));
            return cases;
        }
    }

    @Nested
    class AbsentCandidateSku {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           String expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            assertThatThrownBy(() -> engine.calculate(basket, promotions))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(expected);
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            IPromotion malformed = ignored -> new PromotionResult(List.of(
                    new PricingRule(Map.of("Z", 1), Money.ZERO)));
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(100)), 1)));
            dataValues.put("promotions", List.of(malformed));
            cases[0] = tcb.addCase("absent from basket");
            return cases;
        }
    }

    @Nested
    class StrategyFailure {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           String expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            assertThatThrownBy(() -> engine.calculate(basket, promotions))
                    .isInstanceOf(IllegalStateException.class).hasMessage(expected);
            assertThat(((int[]) dataValues.get("promotionCalls"))[0]).isEqualTo(expectedCalls.get("promotion"));
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            int[] promotionCalls = {0};
            IPromotion failing = ignored -> {
                promotionCalls[0]++;
                throw new IllegalStateException("Invalid promotion configuration");
            };
            dataValues.put("engine", new PricingEngine(new PromotionOptimizer()));
            dataValues.put("basket", Basket.empty());
            dataValues.put("promotions", List.of(failing));
            dataValues.put("promotionCalls", promotionCalls);
            expectedCalls.put("promotion", 1);
            cases[0] = tcb.addCase("Invalid promotion configuration");
            return cases;
        }
    }

    @Nested
    class InjectedOptimizer {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           Money expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            List<Basket> receivedBaskets = (List<Basket>) dataValues.get("receivedBaskets");
            List<List<PricingRule>> receivedRules = (List<List<PricingRule>>) dataValues.get("receivedRules");
            PricingRule rule = (PricingRule) dataValues.get("rule");
            assertThat(engine.calculate(basket, promotions)).isEqualTo(expected);
            assertThat(receivedBaskets).hasSize(expectedCalls.get("optimizer"));
            assertThat(receivedBaskets.getFirst()).isSameAs(basket);
            assertThat(receivedRules).containsExactly(List.of(rule));
            assertThat(receivedRules).hasSize(expectedCalls.get("optimizer"));
            assertThat(((int[]) dataValues.get("promotionCalls"))[0]).isEqualTo(expectedCalls.get("promotion"));
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            Basket basket = new Basket(Map.of(new Item("A", Money.ofPence(100)), 2));
            PricingRule rule = new PricingRule(Map.of("A", 2), Money.ofPence(150));
            int[] promotionCalls = {0};
            IPromotion promotion = ignored -> {
                promotionCalls[0]++;
                return new PromotionResult(List.of(rule, rule));
            };
            List<Basket> receivedBaskets = new ArrayList<>();
            List<List<PricingRule>> receivedRules = new ArrayList<>();
            IPromotionOptimizer optimizer = (receivedBasket, candidates) -> {
                receivedBaskets.add(receivedBasket);
                receivedRules.add(candidates);
                return Money.ofPence(50);
            };
            dataValues.put("engine", new PricingEngine(optimizer));
            dataValues.put("basket", basket);
            dataValues.put("promotions", List.of(promotion, promotion));
            dataValues.put("rule", rule);
            dataValues.put("receivedBaskets", receivedBaskets);
            dataValues.put("receivedRules", receivedRules);
            dataValues.put("promotionCalls", promotionCalls);
            expectedCalls.put("optimizer", 1);
            expectedCalls.put("promotion", 2);
            cases[0] = tcb.addCase(Money.ofPence(150));
            return cases;
        }
    }

    @ParameterizedTest
    @MethodSource("constructorDataProvider")
    void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                         String expected) {
        assertThatThrownBy(() -> new PricingEngine((IPromotionOptimizer) dataValues.get("optimizer")))
                .isInstanceOf(NullPointerException.class).hasMessage(expected);
    }

    static Object[][] constructorDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        dataValues.put("optimizer", null);
        cases[0] = tcb.addCase("Promotion optimizer is required");
        return cases;
    }

    @Nested
    class OptimizerFailure {
        @ParameterizedTest
        @MethodSource("calculateDataProvider")
        @SuppressWarnings("unchecked")
        void calculateTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                           IllegalStateException expected) {
            IPricingEngine engine = (IPricingEngine) dataValues.get("engine");
            Basket basket = (Basket) dataValues.get("basket");
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            assertThatThrownBy(() -> engine.calculate(basket, promotions)).isSameAs(expected);
            assertThat(((int[]) dataValues.get("optimizerCalls"))[0]).isEqualTo(expectedCalls.get("optimizer"));
        }

        static Object[][] calculateDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            IllegalStateException failure = new IllegalStateException("Allocation failed");
            int[] optimizerCalls = {0};
            IPromotionOptimizer failing = (basket, rules) -> {
                optimizerCalls[0]++;
                throw failure;
            };
            dataValues.put("engine", new PricingEngine(failing));
            dataValues.put("basket", Basket.empty());
            dataValues.put("promotions", List.of());
            dataValues.put("optimizerCalls", optimizerCalls);
            expectedCalls.put("optimizer", 1);
            cases[0] = tcb.addCase(failure);
            return cases;
        }
    }

    private static IPromotion offer(Map<String, Integer> quantities, long price) {
        PricingRule rule = new PricingRule(quantities, Money.ofPence(price));
        return basket -> PromotionResult.eligible(rule, basket);
    }
}
