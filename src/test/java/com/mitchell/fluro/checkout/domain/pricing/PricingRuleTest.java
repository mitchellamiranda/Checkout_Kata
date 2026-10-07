package com.mitchell.fluro.checkout.domain.pricing;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
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

class PricingRuleTest {

    @ParameterizedTest
    @MethodSource("maximumApplicationsDataProvider")
    @DisplayName("The scarcest required item limits the number of bundles")
    void maximumApplicationsTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                                 Integer expected) {
        assertThat(((PricingRule) dataValues.get("rule")).maximumApplications((Basket) dataValues.get("basket")))
                .isEqualTo(expected);
    }

    static Object[][] maximumApplicationsDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("rule", new PricingRule(Map.of("A", 2, "B", 1), Money.ofPence(100)));
        dataDefaults.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(50)), 7,
                new Item("B", Money.ofPence(75)), 2)));
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        cases[0] = tcb.addCase(2);
        return cases;
    }

    @ParameterizedTest
    @MethodSource("unitTotalDataProvider")
    @DisplayName("A bundle's unit total uses undiscounted catalogue prices")
    void unitTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Money expected) {
        assertThat(((PricingRule) dataValues.get("rule")).unitTotal((Basket) dataValues.get("basket")))
                .isEqualTo(expected);
    }

    static Object[][] unitTotalDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("rule", new PricingRule(Map.of("A", 2, "B", 1), Money.ofPence(100)));
        dataDefaults.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(50)), 7,
                new Item("B", Money.ofPence(75)), 2)));
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        cases[0] = tcb.addCase(Money.ofPence(175));
        return cases;
    }

    @ParameterizedTest
    @MethodSource("eligibleDataProvider")
    @DisplayName("Only baskets containing a complete bundle produce an eligible rule")
    void eligibleTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                      PromotionResult expected) {
        PromotionResult result = PromotionResult.eligible((PricingRule) dataValues.get("rule"),
                (Basket) dataValues.get("basket"));
        assertThat(result).isEqualTo(expected);
        assertThat(result.rules()).containsExactlyElementsOf(expected.rules());
    }

    static Object[][] eligibleDataProvider() {
        PricingRule rule = new PricingRule(Map.of("A", 2, "B", 1), Money.ofPence(100));
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("rule", rule);
        dataDefaults.put("basket", Basket.empty());
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[2][];
        cases[0] = tcb.addCase(PromotionResult.NONE);
        dataValues.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(50)), 7,
                new Item("B", Money.ofPence(75)), 2)));
        cases[1] = tcb.addCase(new PromotionResult(List.of(rule)));
        return cases;
    }

    @Nested
    @DisplayName("Pricing rule constructor validation")
    class InvalidRules {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Rules require consuming quantities, valid SKUs and non-null inputs")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> quantities = (Map<String, Integer>) dataValues.get("quantities");
            assertThatThrownBy(() -> new PricingRule(quantities, (Money) dataValues.get("price")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", Map.of("A", 0));
            dataDefaults.put("price", Money.ZERO);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[6][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", Map.of("A", -1));
            cases[1] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", Map.of());
            cases[2] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", Map.of(" ", 1));
            cases[3] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", Map.of("A", 1));
            dataValues.put("price", null);
            cases[4] = tcb.addCase(NullPointerException.class);
            dataValues.put("quantities", null);
            cases[5] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Promotion result constructor validation")
    class InvalidResults {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("A null result rule list is rejected")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            List<PricingRule> rules = (List<PricingRule>) dataValues.get("rules");
            assertThatThrownBy(() -> new PromotionResult(rules)).isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rules", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Pricing rule defensive copying")
    class RuleSnapshots {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Rule quantities survive source mutation and cannot be cleared")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Map<String, Integer> expected) {
            PricingRule rule = (PricingRule) dataValues.get("rule");
            assertThat(rule.quantities()).containsExactlyEntriesOf(expected);
            assertThatThrownBy(() -> rule.quantities().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rule", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            Map<String, Integer> quantities = new HashMap<>(Map.of("A", 2));
            PricingRule rule = new PricingRule(quantities, Money.ofPence(75));
            quantities.clear();
            dataValues.put("rule", rule);
            cases[0] = tcb.addCase(Map.of("A", 2));
            return cases;
        }
    }

    @Nested
    @DisplayName("Promotion result defensive copying")
    class ResultSnapshots {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Result rules survive source mutation and cannot be cleared")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             List<PricingRule> expected) {
            PromotionResult result = (PromotionResult) dataValues.get("result");
            assertThat(result.rules()).containsExactlyElementsOf(expected);
            assertThatThrownBy(() -> result.rules().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("result", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            PricingRule rule = new PricingRule(Map.of("A", 2), Money.ofPence(75));
            List<PricingRule> rules = new ArrayList<>(List.of(rule));
            PromotionResult result = new PromotionResult(rules);
            rules.clear();
            dataValues.put("result", result);
            cases[0] = tcb.addCase(List.of(rule));
            return cases;
        }
    }
}
