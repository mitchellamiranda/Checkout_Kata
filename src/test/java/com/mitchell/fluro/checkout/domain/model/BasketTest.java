package com.mitchell.fluro.checkout.domain.model;

import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BasketTest {

    @ParameterizedTest
    @MethodSource("emptyDataProvider")
    @DisplayName("An empty basket has no quantities, no matching item and a zero total")
    void emptyTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Money expected) {
        Basket basket = Basket.empty();
        assertThat(basket.quantities()).isEmpty();
        assertThat(basket.quantityOf((String) dataValues.get("sku"))).isZero();
        assertThat(basket.item((String) dataValues.get("sku"))).isEmpty();
        assertThat(basket.unitTotal()).isEqualTo(expected);
    }

    static Object[][] emptyDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("sku", "A");
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        cases[0] = tcb.addCase(Money.ZERO);
        return cases;
    }

    @Nested
    @DisplayName("Adding items preserves independent snapshots")
    class AddingItems {

        @ParameterizedTest
        @MethodSource("addDataProvider")
        @DisplayName("Adding another SKU and repeating the first leaves the original unchanged")
        void addTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     Map<Item, Integer> expected) {
            Basket first = (Basket) dataValues.get("basket");
            Item a = (Item) dataValues.get("firstItem");
            Basket second = first.add((Item) dataValues.get("nextItem")).add(a);
            assertThat(first.quantities()).containsExactlyEntriesOf(Map.of(a, 1));
            assertThat(second.quantities()).containsExactlyInAnyOrderEntriesOf(expected);
            assertThat(second.quantityOf(a.sku())).isEqualTo(2);
            assertThat(second.unitTotal()).isEqualTo(Money.ofPence(175));
        }

        static Object[][] addDataProvider() {
            Item a = new Item("A", Money.ofPence(50));
            Item b = new Item("B", Money.ofPence(75));
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", Basket.empty().add(a));
            dataDefaults.put("firstItem", a);
            dataDefaults.put("nextItem", b);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(Map.of(a, 2, b, 1));
            return cases;
        }
    }

    @Nested
    @DisplayName("Basket constructor validation")
    class InvalidBaskets {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Nonpositive quantities and a null quantity map are rejected")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            Map<Item, Integer> quantities = (Map<Item, Integer>) dataValues.get("quantities");
            assertThatThrownBy(() -> new Basket(quantities)).isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            Item a = new Item("A", Money.ofPence(50));
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", Map.of(a, 0));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[4][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", Map.of(a, -1));
            cases[1] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", Map.of(a, Integer.MIN_VALUE));
            cases[2] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("quantities", null);
            cases[3] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Conflicting prices for one SKU")
    class ConflictingPrices {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("A basket cannot contain duplicate SKUs with different prices")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            @SuppressWarnings("unchecked")
            Map<Item, Integer> quantities = (Map<Item, Integer>) dataValues.get("quantities");
            assertThatThrownBy(() -> new Basket(quantities))
                    .isInstanceOf(expected).hasMessageContaining("Duplicate SKU");
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", Map.of(new Item("A", Money.ofPence(50)), 1,
                    new Item("A", Money.ofPence(60)), 1));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            return cases;
        }

        @ParameterizedTest
        @MethodSource("addDataProvider")
        @DisplayName("Adding an existing SKU with a different price reports the conflict")
        void addTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> ((Basket) dataValues.get("basket")).add((Item) dataValues.get("item")))
                    .isInstanceOf(expected).hasMessageContaining("Conflicting price");
        }

        static Object[][] addDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", Basket.empty().add(new Item("A", Money.ofPence(50))));
            dataDefaults.put("item", new Item("A", Money.ofPence(60)));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Invalid additions")
    class InvalidAdditions {

        @ParameterizedTest
        @MethodSource("addDataProvider")
        @DisplayName("Quantity overflow and null items are rejected")
        void addTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> ((Basket) dataValues.get("basket")).add((Item) dataValues.get("item")))
                    .isInstanceOf(expected);
        }

        static Object[][] addDataProvider() {
            Item a = new Item("A", Money.ofPence(50));
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", new Basket(Map.of(a, Integer.MAX_VALUE)));
            dataDefaults.put("item", a);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = tcb.addCase(ArithmeticException.class);
            dataValues.put("basket", Basket.empty());
            dataValues.put("item", null);
            cases[1] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @Nested
    @DisplayName("Basket defensive copying")
    class DefensiveCopies {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Clearing the source does not change the basket's quantity")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Integer expected) {
            assertThat(((Basket) dataValues.get("basket")).quantityOf((String) dataValues.get("sku")))
                    .isEqualTo(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("basket", null);
            dataDefaults.put("sku", "A");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            Map<Item, Integer> source = new HashMap<>(Map.of(new Item("A", Money.ofPence(50)), 2));
            Basket basket = new Basket(source);
            source.clear();
            dataValues.put("basket", basket);
            cases[0] = tcb.addCase(2);
            return cases;
        }
    }

    @ParameterizedTest
    @MethodSource("quantitiesDataProvider")
    @DisplayName("The exposed quantity map is immutable")
    void quantitiesTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                        Class<? extends Throwable> expected) {
        assertThatThrownBy(() -> ((Basket) dataValues.get("basket")).quantities().clear()).isInstanceOf(expected);
    }

    static Object[][] quantitiesDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("basket", new Basket(Map.of(new Item("A", Money.ofPence(50)), 2)));
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        cases[0] = tcb.addCase(UnsupportedOperationException.class);
        return cases;
    }

    @Nested
    @DisplayName("Item constructor validation")
    class InvalidItems {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @DisplayName("Blank, padded and null SKUs and null prices are rejected")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> new Item((String) dataValues.get("sku"), (Money) dataValues.get("price")))
                    .isInstanceOf(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("sku", "");
            dataDefaults.put("price", Money.ZERO);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[7][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("sku", " ");
            cases[1] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("sku", "\t");
            cases[2] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("sku", " A");
            cases[3] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("sku", "A ");
            cases[4] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("sku", null);
            cases[5] = tcb.addCase(NullPointerException.class);
            dataValues.put("sku", "A");
            dataValues.put("price", null);
            cases[6] = tcb.addCase(NullPointerException.class);
            return cases;
        }
    }

    @ParameterizedTest
    @MethodSource("unitTotalDataProvider")
    @DisplayName("Free items with multi-character SKUs contribute zero to the total")
    void unitTotalTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Money expected) {
        assertThat(((Basket) dataValues.get("basket")).unitTotal()).isEqualTo(expected);
    }

    static Object[][] unitTotalDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("basket", Basket.empty().add(new Item("APPLE-01", Money.ZERO)));
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[1][];
        cases[0] = tcb.addCase(Money.ZERO);
        return cases;
    }
}
