package com.mitchell.fluro.checkout.domain.pricing;

import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.promotion.PromotionResult;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingRulesTest {

    @Nested
    @DisplayName("Catalogue defensive copying")
    class CatalogueSnapshots {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Catalogue lookup survives clearing the supplied item list")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Item expected) {
            assertThat(((PricingRules) dataValues.get("rules")).item((String) dataValues.get("sku")))
                    .isEqualTo(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rules", null);
            dataDefaults.put("sku", "A");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            Item a = new Item("A", Money.ofPence(50));
            List<Item> source = new ArrayList<>(List.of(a));
            PricingRules rules = new PricingRules(source);
            source.clear();
            dataValues.put("rules", rules);
            cases[0] = tcb.addCase(a);
            return cases;
        }
    }

    @Nested
    @DisplayName("Promotion registration defensive copying")
    class PromotionSnapshots {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Promotions survive clearing the supplied list and cannot be cleared")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             List<IPromotion> expected) {
            PricingRules rules = (PricingRules) dataValues.get("rules");
            assertThat(rules.promotions()).containsExactlyElementsOf(expected);
            assertThatThrownBy(() -> rules.promotions().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rules", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            IPromotion promotion = basket -> PromotionResult.NONE;
            List<IPromotion> source = new ArrayList<>(List.of(promotion));
            PricingRules rules = new PricingRules(List.of(new Item("A", Money.ofPence(50))), source);
            source.clear();
            dataValues.put("rules", rules);
            cases[0] = tcb.addCase(List.of(promotion));
            return cases;
        }
    }

    @Nested
    @DisplayName("Null promotion registration validation")
    class InvalidPromotions {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Null promotion lists and entries are rejected")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            List<Item> items = (List<Item>) dataValues.get("items");
            @SuppressWarnings("unchecked")
            List<IPromotion> promotions = (List<IPromotion>) dataValues.get("promotions");
            assertThatThrownBy(() -> new PricingRules(items, promotions)).isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("items", List.of(new Item("A", Money.ofPence(50))));
            dataDefaults.put("promotions", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = tcb.addCase(NullPointerException.class);
            IPromotion promotion = basket -> PromotionResult.NONE;
            dataValues.put("promotions", Arrays.asList(promotion, null));
            cases[1] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Duplicate catalogue SKU validation")
    class DuplicateSkus {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Duplicate SKUs are rejected whether their prices agree or differ")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            List<Item> items = (List<Item>) dataValues.get("items");
            assertThatThrownBy(() -> new PricingRules(items))
                    .isInstanceOf(expected).hasMessageContaining("Duplicate catalogue SKU");
        }

        static Object[][] constructorDataProvider() {
            Item a = new Item("A", Money.ofPence(50));
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("items", List.of(a, a));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("items", List.of(a, new Item("A", Money.ZERO)));
            cases[1] = tcb.addCase(IllegalArgumentException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Null catalogue validation")
    class InvalidCatalogues {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Null catalogues and catalogue entries are rejected")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            List<Item> items = (List<Item>) dataValues.get("items");
            assertThatThrownBy(() -> new PricingRules(items)).isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("items", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = tcb.addCase(NullPointerException.class);
            dataValues.put("items", Arrays.asList(new Item("A", Money.ofPence(50)), null));
            cases[1] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @ParameterizedTest
    @MethodSource("itemDataProvider")
    @DisplayName("Looking up an unknown SKU reports its name explicitly")
    void itemTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                  Class<? extends Throwable> expected) {
        assertThatThrownBy(() -> ((PricingRules) dataValues.get("rules")).item((String) dataValues.get("sku")))
                .isInstanceOf(expected).hasMessage("Unknown SKU: A");
    }

    static Object[][] itemDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("rules", new PricingRules(List.of()));
        dataDefaults.put("sku", "A");
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        cases[0] = tcb.addCase(IllegalArgumentException.class);
        return cases;
    }
}
