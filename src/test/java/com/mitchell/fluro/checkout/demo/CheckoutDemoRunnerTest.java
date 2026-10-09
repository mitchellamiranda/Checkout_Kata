package com.mitchell.fluro.checkout.demo;

import com.mitchell.fluro.checkout.application.checkout.CheckoutFactory;
import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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

class CheckoutDemoRunnerTest {

    @Nested
    @DisplayName("Four independent baskets use the supplied pricing snapshot and output")
    class BasketExecution {

        @ParameterizedTest
        @MethodSource("runDataProvider")
        void runTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     String expected) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                CheckoutDemoRunner demo = new CheckoutDemoRunner(
                        (ICheckoutFactory) dataValues.get("factory"),
                        (PricingRules) dataValues.get("rules"), output);

                demo.run((String[]) dataValues.get("args"));

                assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualTo(expected);
                assertThat(dataValues.get("scannedBaskets")).isEqualTo(dataValues.get("expectedBaskets"));
                assertThat((List<?>) dataValues.get("createdCheckouts"))
                        .hasSize(expectedCalls.get("create")).doesNotHaveDuplicates();
                assertThat((List<?>) dataValues.get("suppliedRules"))
                        .hasSize(expectedCalls.get("create"))
                        .allSatisfy(rules -> assertThat(rules).isSameAs(dataValues.get("rules")));
                assertThat(((AtomicInteger) dataValues.get("getTotalCalls")).get())
                        .isEqualTo(expectedCalls.get("getTotal"));
            }
        }

        static Object[][] runDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("args", new String[0]);
            dataDefaults.put("rules", unitRules(1, 1, 1, 1, 1));
            dataDefaults.put("expectedBaskets", List.of(
                    List.of("B", "A", "B"),
                    List.of("B", "B", "A"),
                    List.of("A", "B", "B", "C", "C", "C", "C", "D", "E"),
                    List.of("A", "A", "B", "B", "B", "B", "C", "C", "C", "C",
                            "C", "C", "C", "C", "D", "D", "E", "E")));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("create", 4);
            expectedCallsDefaults.put("getTotal", 4);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[3][];

            dataValues.put("factory", recordingFactory(dataValues, factory()));
            dataProvider[0] = tcb.addCase(outputLines(
                    "B A B -> 3p",
                    "B B A -> 3p",
                    "A B B C C C C D E -> 9p",
                    "A A B B B B C C C C C C C C D D E E -> 18p"));

            dataValues.put("args", new String[]{"ignored", "--another=argument"});
            dataValues.put("rules", unitRules(10, 20, 30, 40, 50));
            dataValues.put("factory", recordingFactory(dataValues, factory()));
            dataProvider[1] = tcb.addCase(outputLines(
                    "B A B -> 50p",
                    "B B A -> 50p",
                    "A B B C C C C D E -> 260p",
                    "A A B B B B C C C C C C C C D D E E -> 520p"));

            dataValues.put("args", null);
            dataValues.put("rules", unitRules(0, 0, 0, 0, 0));
            dataValues.put("factory", recordingFactory(dataValues, factory()));
            dataProvider[2] = tcb.addCase(outputLines(
                    "B A B -> 0p",
                    "B B A -> 0p",
                    "A B B C C C C D E -> 0p",
                    "A A B B B B C C C C C C C C D D E E -> 0p"));
            return dataProvider;
        }
    }

    @Nested
    @DisplayName("Required constructor dependencies")
    class MissingDependencies {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        void constructorTest(HashMap<String, Object> dataValues,
                             HashMap<String, Integer> expectedCalls, String expected) {
            try (PrintStream output = (PrintStream) dataValues.get("output")) {
                assertThatThrownBy(() -> new CheckoutDemoRunner(
                        (ICheckoutFactory) dataValues.get("factory"),
                        (PricingRules) dataValues.get("rules"), output))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage(expected);
            }
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("factory", factory());
            dataDefaults.put("rules", unitRules(1, 1, 1, 1, 1));
            dataDefaults.put("output", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[4][];

            dataValues.put("factory", null);
            dataValues.put("output", new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            dataProvider[0] = tcb.addCase("Checkout factory is required");

            dataValues.put("rules", null);
            dataValues.put("output", new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            dataProvider[1] = tcb.addCase("Pricing rules are required");

            dataProvider[2] = tcb.addCase("Output is required");

            dataValues.put("factory", null);
            dataValues.put("rules", null);
            dataProvider[3] = tcb.addCase("Checkout factory is required");
            return dataProvider;
        }
    }

    @Nested
    @DisplayName("Failures escape without printing success or starting another basket")
    class FailedExecution {

        @ParameterizedTest
        @MethodSource("runDataProvider")
        void runTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     String expected) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                CheckoutDemoRunner demo = new CheckoutDemoRunner(
                        (ICheckoutFactory) dataValues.get("factory"),
                        (PricingRules) dataValues.get("rules"), output);

                assertThatThrownBy(demo::run).isSameAs(dataValues.get("failure"));

                assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualTo(expected);
                assertThat(dataValues.get("scannedBaskets")).isEqualTo(dataValues.get("expectedBaskets"));
                assertThat((List<?>) dataValues.get("suppliedRules"))
                        .hasSize(expectedCalls.get("create"))
                        .allSatisfy(rules -> assertThat(rules).isSameAs(dataValues.get("rules")));
                assertThat(((AtomicInteger) dataValues.get("getTotalCalls")).get())
                        .isEqualTo(expectedCalls.get("getTotal"));
            }
        }

        static Object[][] runDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("rules", unitRules(1, 1, 1, 1, 1));
            dataDefaults.put("expectedBaskets", List.of());
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("create", 1);
            expectedCallsDefaults.put("getTotal", 0);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[2][];

            IllegalStateException factoryFailure = new IllegalStateException("Factory unavailable");
            dataValues.put("failure", factoryFailure);
            dataValues.put("factory", recordingFactory(dataValues, rules -> {
                throw factoryFailure;
            }));
            dataProvider[0] = tcb.addCase("");

            ArithmeticException pricingFailure = new ArithmeticException("Pricing overflow");
            dataValues.put("failure", pricingFailure);
            dataValues.put("expectedBaskets", List.of(List.of("B", "A", "B")));
            dataValues.put("factory", recordingFactory(dataValues,
                    new CheckoutFactory((basket, promotions) -> {
                        throw pricingFailure;
                    })));
            expectedCalls.put("getTotal", 1);
            dataProvider[1] = tcb.addCase("");
            return dataProvider;
        }
    }

    private static PricingRules unitRules(long a, long b, long c, long d, long e) {
        return new PricingRules(List.of(
                new Item("A", Money.ofPence(a)),
                new Item("B", Money.ofPence(b)),
                new Item("C", Money.ofPence(c)),
                new Item("D", Money.ofPence(d)),
                new Item("E", Money.ofPence(e))));
    }

    private static ICheckoutFactory factory() {
        return new CheckoutFactory(new PricingEngine(new PromotionOptimizer()));
    }

    private static ICheckoutFactory recordingFactory(HashMap<String, Object> dataValues,
                                                      ICheckoutFactory delegate) {
        List<ICheckout> createdCheckouts = new ArrayList<>();
        List<List<String>> scannedBaskets = new ArrayList<>();
        List<PricingRules> suppliedRules = new ArrayList<>();
        AtomicInteger getTotalCalls = new AtomicInteger();
        dataValues.put("createdCheckouts", createdCheckouts);
        dataValues.put("scannedBaskets", scannedBaskets);
        dataValues.put("suppliedRules", suppliedRules);
        dataValues.put("getTotalCalls", getTotalCalls);
        return rules -> {
            suppliedRules.add(rules);
            ICheckout checkout = delegate.create(rules);
            createdCheckouts.add(checkout);
            List<String> scans = new ArrayList<>();
            scannedBaskets.add(scans);
            return new ICheckout() {
                @Override
                public void scan(String sku) {
                    scans.add(sku);
                    checkout.scan(sku);
                }

                @Override
                public Money getTotal() {
                    getTotalCalls.incrementAndGet();
                    return checkout.getTotal();
                }

                @Override
                public Basket getBasket() {
                    return checkout.getBasket();
                }
            };
        };
    }

    private static String outputLines(String... lines) {
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }
}
