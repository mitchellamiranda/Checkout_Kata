package com.mitchell.fluro.checkout.domain.model;

import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Nested
    @DisplayName("Exact arithmetic in pence")
    class ExactArithmetic {

        @ParameterizedTest
        @MethodSource("addDataProvider")
        @DisplayName("Addition preserves the exact sum")
        void addTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Money expected) {
            assertThat(((Money) dataValues.get("money")).add((Money) dataValues.get("other"))).isEqualTo(expected);
        }

        static Object[][] addDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("money", Money.ofPence(50));
            dataDefaults.put("other", Money.ofPence(75));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(Money.ofPence(125));
            return cases;
        }

        @ParameterizedTest
        @MethodSource("subtractDataProvider")
        @DisplayName("Subtraction preserves the exact difference")
        void subtractTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Money expected) {
            assertThat(((Money) dataValues.get("money")).subtract((Money) dataValues.get("other"))).isEqualTo(expected);
        }

        static Object[][] subtractDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("money", Money.ofPence(125));
            dataDefaults.put("other", Money.ofPence(75));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(Money.ofPence(50));
            return cases;
        }

        @ParameterizedTest
        @MethodSource("multiplyDataProvider")
        @DisplayName("Multiplication supports positive and zero quantities")
        void multiplyTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Money expected) {
            assertThat(((Money) dataValues.get("money")).multiply((Integer) dataValues.get("quantity")))
                    .isEqualTo(expected);
        }

        static Object[][] multiplyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("money", Money.ofPence(25));
            dataDefaults.put("quantity", 3);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = tcb.addCase(Money.ofPence(75));
            dataValues.put("quantity", 0);
            cases[1] = tcb.addCase(Money.ZERO);
            return cases;
        }
    }

    @ParameterizedTest
    @MethodSource("compareToDataProvider")
    @DisplayName("Amounts compare below, above and equal to each other")
    void compareToTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, Integer expected) {
        assertThat(((Money) dataValues.get("money")).compareTo((Money) dataValues.get("other"))).isEqualTo(expected);
    }

    static Object[][] compareToDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("money", Money.ofPence(25));
        dataDefaults.put("other", Money.ofPence(50));
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[3][];
        cases[0] = tcb.addCase(-1);
        dataValues.put("money", Money.ofPence(50));
        dataValues.put("other", Money.ZERO);
        cases[1] = tcb.addCase(1);
        dataValues.put("money", Money.ZERO);
        dataValues.put("other", Money.ofPence(0));
        cases[2] = tcb.addCase(0);
        return cases;
    }

    @ParameterizedTest
    @MethodSource("ofPenceDataProvider")
    @DisplayName("Negative amounts are rejected, including the minimum long")
    void ofPenceTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     Class<? extends Throwable> expected) {
        assertThatThrownBy(() -> Money.ofPence((Long) dataValues.get("amount"))).isInstanceOf(expected);
    }

    static Object[][] ofPenceDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("amount", -1L);
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] cases = new Object[2][];
        cases[0] = tcb.addCase(IllegalArgumentException.class);
        dataValues.put("amount", Long.MIN_VALUE);
        cases[1] = tcb.addCase(IllegalArgumentException.class);
        return cases;
    }

    @Nested
    @DisplayName("Invalid arithmetic and overflow")
    class InvalidArithmetic {

        @ParameterizedTest
        @MethodSource("addDataProvider")
        @DisplayName("Addition fails explicitly on overflow")
        void addTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> ((Money) dataValues.get("money")).add((Money) dataValues.get("other")))
                    .isInstanceOf(expected);
        }

        static Object[][] addDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("money", Money.ofPence(Long.MAX_VALUE));
            dataDefaults.put("other", Money.ofPence(1));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(ArithmeticException.class);
            return cases;
        }

        @ParameterizedTest
        @MethodSource("subtractDataProvider")
        @DisplayName("Subtraction cannot produce a negative amount")
        void subtractTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> ((Money) dataValues.get("money")).subtract((Money) dataValues.get("other")))
                    .isInstanceOf(expected);
        }

        static Object[][] subtractDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("money", Money.ZERO);
            dataDefaults.put("other", Money.ofPence(1));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            return cases;
        }

        @ParameterizedTest
        @MethodSource("multiplyDataProvider")
        @DisplayName("Multiplication rejects negative quantities and overflow")
        void multiplyTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                          Class<? extends Throwable> expected) {
            assertThatThrownBy(() -> ((Money) dataValues.get("money")).multiply((Integer) dataValues.get("quantity")))
                    .isInstanceOf(expected);
        }

        static Object[][] multiplyDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("money", Money.ZERO);
            dataDefaults.put("quantity", -1);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];
            cases[0] = tcb.addCase(IllegalArgumentException.class);
            dataValues.put("money", Money.ofPence(Long.MAX_VALUE));
            dataValues.put("quantity", 2);
            cases[1] = tcb.addCase(ArithmeticException.class);
            return cases;
        }
    }
}
