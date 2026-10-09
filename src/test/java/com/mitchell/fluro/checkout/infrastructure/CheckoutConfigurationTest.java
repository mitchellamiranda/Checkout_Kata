package com.mitchell.fluro.checkout.infrastructure;

import com.mitchell.fluro.checkout.application.checkout.CheckoutFactory;
import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.IPromotionOptimizer;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutConfigurationTest {

    @Nested
    class SuppliedCatalogue {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Money expected) {
            new ApplicationContextRunner().withUserConfiguration(CheckoutConfiguration.class).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(ICheckoutFactory.class)).isExactlyInstanceOf(CheckoutFactory.class);
                assertThat(context.getBean(IPricingEngine.class)).isExactlyInstanceOf(PricingEngine.class);
                assertThat(context.getBean(IPromotionOptimizer.class)).isExactlyInstanceOf(PromotionOptimizer.class);
                PricingRules rules = context.getBean("pricingRules", PricingRules.class);
                Map<String, Money> prices = (Map<String, Money>) dataValues.get("prices");
                prices.forEach((sku, price) -> {
                    assertThat(rules.item(sku)).isSameAs(context.getBean("item" + sku, Item.class));
                    assertThat(rules.item(sku).unitPrice()).isEqualTo(price);
                });
                assertThat(rules.promotions()).containsExactlyInAnyOrderElementsOf(
                        context.getBeansOfType(IPromotion.class).values()).hasSize(3);
                ICheckout first = context.getBean(ICheckoutFactory.class).create(rules);
                ICheckout second = context.getBean(ICheckoutFactory.class).create(rules);
                ((List<String>) dataValues.get("scans")).forEach(first::scan);

                assertThat(first).isNotSameAs(second);
                assertThat(first.getTotal()).isEqualTo(expected);
                assertThat(first.getTotal()).isEqualTo(expected);
                assertThat(second.getTotal()).isEqualTo(Money.ZERO);
                assertThat(second.getBasket()).isEqualTo(Basket.empty());
                assertThatThrownBy(() -> rules.promotions().clear())
                        .isExactlyInstanceOf(UnsupportedOperationException.class);
            });
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("scans", null);
            dataDefaults.put("prices", Map.of("A", Money.ofPence(50), "B", Money.ofPence(75),
                    "C", Money.ofPence(25), "D", Money.ofPence(150), "E", Money.ofPence(200)));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[6][];

            dataValues.put("scans", new ArrayList<String>());
            cases[0] = tcb.addCase(Money.ZERO);
            dataValues.put("scans", new ArrayList<>(List.of("A", "B", "C", "D", "E")));
            cases[1] = tcb.addCase(Money.ofPence(450));
            dataValues.put("scans", new ArrayList<>(List.of("B", "B")));
            cases[2] = tcb.addCase(Money.ofPence(125));
            dataValues.put("scans", new ArrayList<>(List.of("C", "C", "C", "C")));
            cases[3] = tcb.addCase(Money.ofPence(75));
            dataValues.put("scans", new ArrayList<>(List.of("D", "E")));
            cases[4] = tcb.addCase(Money.ofPence(300));
            dataValues.put("scans", new ArrayList<>(List.of("A", "B", "B", "C", "C", "C", "C", "D", "E")));
            cases[5] = tcb.addCase(Money.ofPence(550));
            return cases;
        }
    }

    @Nested
    class AlternateXmlCatalogue {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Money expected) {
            xmlRunner((String) dataValues.get("fixture")).run(context -> {
                assertThat(context).hasNotFailed();
                PricingRules rules = context.getBean("pricingRules", PricingRules.class);
                assertThat(rules.promotions()).hasSize((Integer) dataValues.get("promotionCount"))
                        .containsExactlyInAnyOrderElementsOf(context.getBeansOfType(IPromotion.class).values());
                assertThat(rules.item("X").unitPrice()).isEqualTo(dataValues.get("unitPrice"));
                assertThatThrownBy(() -> rules.item("A")).isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Unknown SKU: A");
                ICheckout checkout = context.getBean(ICheckoutFactory.class).create(rules);
                ((List<String>) dataValues.get("scans")).forEach(checkout::scan);
                assertThat(checkout.getTotal()).isEqualTo(expected);
            });
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("fixture", "xml/AlternativeCatalogue.xml");
            dataDefaults.put("scans", null);
            dataDefaults.put("promotionCount", 3);
            dataDefaults.put("unitPrice", Money.ofPence(100));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];

            dataValues.put("scans", new ArrayList<>(List.of("X", "X", "X", "X", "Y", "Y", "Z")));
            cases[0] = tcb.addCase(Money.ofPence(425));
            dataValues.put("fixture", "xml/UnitOnlyContext.xml");
            dataValues.put("scans", new ArrayList<>(List.of("X", "X", "X")));
            dataValues.put("promotionCount", 0);
            dataValues.put("unitPrice", Money.ofPence(42));
            cases[1] = tcb.addCase(Money.ofPence(126));
            return cases;
        }
    }

    @Nested
    class AdditionalXmlPromotions {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Money expected) {
            xmlRunner((String) dataValues.get("fixture")).run(context -> {
                assertThat(context).hasNotFailed();
                PricingRules rules = context.getBean("pricingRules", PricingRules.class);
                assertThat(rules.promotions()).containsExactlyInAnyOrderElementsOf(
                        context.getBeansOfType(IPromotion.class).values())
                        .hasSize((Integer) dataValues.get("promotionCount"));
                ICheckout checkout = context.getBean(ICheckoutFactory.class).create(rules);
                ((List<String>) dataValues.get("scans")).forEach(checkout::scan);
                assertThat(checkout.getTotal()).isEqualTo(expected);
                assertThatThrownBy(() -> rules.promotions().clear())
                        .isExactlyInstanceOf(UnsupportedOperationException.class);
            });
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("fixture", "xml/AdditionalPromotion.xml");
            dataDefaults.put("promotionCount", 4);
            dataDefaults.put("scans", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[3][];

            dataValues.put("scans", new ArrayList<>(List.of("B", "B")));
            cases[0] = tcb.addCase(Money.ofPence(100));
            dataValues.put("fixture", "xml/OverlappingPromotions.xml");
            dataValues.put("promotionCount", 5);
            dataValues.put("scans", new ArrayList<>(List.of("B", "B", "B")));
            cases[1] = tcb.addCase(Money.ofPence(140));
            dataValues.put("fixture", "xml/OverlappingPromotions.xml");
            dataValues.put("promotionCount", 5);
            dataValues.put("scans", new ArrayList<>(List.of("B", "B", "B", "B")));
            cases[2] = tcb.addCase(Money.ofPence(200));
            return cases;
        }
    }

    @Nested
    class ExplicitEngineConstructorReference {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Money expected) {
            xmlRunner((String) dataValues.get("fixture")).run(context -> {
                assertThat(context).hasNotFailed();
                RecordingEngine engine = context.getBean("pricingEngine", RecordingEngine.class);
                ICheckout checkout = context.getBean(ICheckoutFactory.class)
                        .create(context.getBean("pricingRules", PricingRules.class));
                ((List<String>) dataValues.get("scans")).forEach(checkout::scan);
                assertThat(checkout.getTotal()).isEqualTo(expected);
                assertThat(engine.baskets()).containsExactly((Basket) dataValues.get("basket"))
                        .hasSize(expectedCalls.get("calculate"));
                assertThat(engine.promotions()).containsExactly(
                        context.getBean("pricingRules", PricingRules.class).promotions())
                        .hasSize(expectedCalls.get("calculate"));
            });
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("fixture", "xml/RecordingEngineContext.xml");
            dataDefaults.put("scans", null);
            dataDefaults.put("basket", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("calculate", 1);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("scans", new ArrayList<>(List.of("X", "X")));
            dataValues.put("basket", new Basket(Map.of(new Item("X", Money.ofPence(42)), 2)));
            cases[0] = tcb.addCase(Money.ofPence(17));
            return cases;
        }
    }

    @Nested
    class ExplicitOptimizerConstructorReference {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             Money expected) {
            xmlRunner((String) dataValues.get("fixture")).run(context -> {
                assertThat(context).hasNotFailed();
                RecordingOptimizer optimizer = context.getBean("promotionOptimizer", RecordingOptimizer.class);
                assertThat(context.getBean("pricingEngine")).isExactlyInstanceOf(PricingEngine.class);
                ICheckout checkout = context.getBean(ICheckoutFactory.class)
                        .create(context.getBean("pricingRules", PricingRules.class));
                ((List<String>) dataValues.get("scans")).forEach(checkout::scan);
                assertThat(checkout.getTotal()).isEqualTo(expected);
                assertThat(optimizer.baskets()).containsExactly((Basket) dataValues.get("basket"))
                        .hasSize(expectedCalls.get("maximumSavings"));
                assertThat(optimizer.rules()).containsExactly((List<PricingRule>) dataValues.get("rules"))
                        .hasSize(expectedCalls.get("maximumSavings"));
            });
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("fixture", "xml/RecordingOptimizerContext.xml");
            dataDefaults.put("scans", null);
            dataDefaults.put("basket", null);
            dataDefaults.put("rules", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("maximumSavings", 1);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("scans", new ArrayList<>(List.of("X", "X")));
            dataValues.put("basket", new Basket(Map.of(new Item("X", Money.ofPence(42)), 2)));
            dataValues.put("rules", new ArrayList<>(List.of(new PricingRule(Map.of("X", 2), Money.ofPence(50)))));
            cases[0] = tcb.addCase(Money.ofPence(77));
            return cases;
        }
    }

    @Nested
    class CallerSuppliedRules {

        @ParameterizedTest
        @MethodSource("createDataProvider")
        void createTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                        Money expected) {
            xmlRunner((String) dataValues.get("fixture")).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBeansOfType(PricingRules.class))
                        .hasSize((Integer) dataValues.get("configuredRules"));
                ICheckoutFactory factory = context.getBean(ICheckoutFactory.class);
                ICheckout first = factory.create((PricingRules) dataValues.get("originalRules"));
                first.scan("A");
                assertThat(first.getTotal()).isEqualTo(dataValues.get("originalTotal"));

                ICheckout second = factory.create((PricingRules) dataValues.get("revisedRules"));
                second.scan("A");
                assertThat(second.getTotal()).isEqualTo(expected);
                assertThat(first.getTotal()).isEqualTo(dataValues.get("originalTotal"));
            });
        }

        static Object[][] createDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("fixture", "ApplicationContext.xml");
            dataDefaults.put("configuredRules", 2);
            dataDefaults.put("originalRules", new PricingRules(List.of(new Item("A", Money.ofPence(10)))));
            dataDefaults.put("revisedRules", new PricingRules(List.of(new Item("A", Money.ofPence(60)))));
            dataDefaults.put("originalTotal", Money.ofPence(10));
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];

            cases[0] = tcb.addCase(Money.ofPence(60));
            dataValues.put("fixture", "CheckoutServicesContext.xml");
            dataValues.put("configuredRules", 0);
            cases[1] = tcb.addCase(Money.ofPence(60));
            return cases;
        }
    }

    @Nested
    class InvalidXmlContexts {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             String expected) {
            xmlRunner((String) dataValues.get("fixture")).run(context -> {
                assertThat(context).hasFailed();
                Throwable root = NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure());
                assertThat(root).isExactlyInstanceOf((Class<? extends Throwable>) dataValues.get("rootType"))
                        .hasMessage(expected);
            });
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("fixture", null);
            dataDefaults.put("rootType", IllegalArgumentException.class);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[12][];

            dataValues.put("fixture", "xml/MissingReference.xml");
            dataValues.put("rootType", NoSuchBeanDefinitionException.class);
            cases[0] = tcb.addCase("No bean named 'pricingEngine' available");
            dataValues.put("fixture", "xml/MissingUnitPrice.xml");
            dataValues.put("rootType", NoSuchBeanDefinitionException.class);
            cases[1] = tcb.addCase("No qualifying bean of type 'com.mitchell.fluro.checkout.domain.model.Money' "
                    + "available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}");
            dataValues.put("fixture", "xml/NegativePrice.xml");
            cases[2] = tcb.addCase("Money cannot be negative");
            dataValues.put("fixture", "xml/ZeroQuantity.xml");
            cases[3] = tcb.addCase("Rule quantities must be positive");
            dataValues.put("fixture", "xml/DuplicateSku.xml");
            cases[4] = tcb.addCase("Duplicate catalogue SKU: X");
            dataValues.put("fixture", "xml/UnknownPromotionSku.xml");
            cases[5] = tcb.addCase("Unknown SKU: Z");
            dataValues.put("fixture", "xml/MissingMultiPrice.xml");
            dataValues.put("rootType", NoSuchBeanDefinitionException.class);
            cases[6] = tcb.addCase("No qualifying bean of type 'com.mitchell.fluro.checkout.domain.model.Money' "
                    + "available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}");
            dataValues.put("fixture", "xml/MissingMealPrice.xml");
            dataValues.put("rootType", NoSuchBeanDefinitionException.class);
            cases[7] = tcb.addCase("No qualifying bean of type 'com.mitchell.fluro.checkout.domain.model.Money' "
                    + "available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}");
            dataValues.put("fixture", "xml/UnknownMealSku.xml");
            cases[8] = tcb.addCase("Unknown SKU: Z");
            dataValues.put("fixture", "xml/DoesNotExist.xml");
            dataValues.put("rootType", FileNotFoundException.class);
            cases[9] = tcb.addCase("class path resource [xml/DoesNotExist.xml] cannot be opened because it does not exist");
            dataValues.put("fixture", "xml/UnknownFreeOfferSku.xml");
            cases[10] = tcb.addCase("Unknown SKU: Z");
            dataValues.put("fixture", "xml/ZeroPaidQuantity.xml");
            cases[11] = tcb.addCase("Paid quantity must be positive");
            return cases;
        }
    }

    private static ApplicationContextRunner xmlRunner(String fixture) {
        return new ApplicationContextRunner().withUserConfiguration(CheckoutConfiguration.class)
                .withPropertyValues("checkout.context=classpath:" + fixture);
    }

    public record RecordingEngine(List<Basket> baskets, List<List<IPromotion>> promotions,
                                  Money result) implements IPricingEngine {
        @Override
        public Money calculate(Basket basket, List<IPromotion> registeredPromotions) {
            baskets.add(basket);
            promotions.add(registeredPromotions);
            return result;
        }
    }

    public record RecordingOptimizer(List<Basket> baskets, List<List<PricingRule>> rules,
                                     Money savings) implements IPromotionOptimizer {
        @Override
        public Money maximumSavings(Basket basket, List<PricingRule> eligibleRules) {
            baskets.add(basket);
            rules.add(eligibleRules);
            return savings;
        }
    }
}
