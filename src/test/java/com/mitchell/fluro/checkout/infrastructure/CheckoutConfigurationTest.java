package com.mitchell.fluro.checkout.infrastructure;

import com.mitchell.fluro.checkout.application.checkout.Checkout;
import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.promotion.PromotionResult;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.IPromotionOptimizer;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutConfigurationTest {

    @Nested
    class AlternativeCatalogue {

        @ParameterizedTest
        @MethodSource("pricingRulesDataProvider")
        void pricingRulesTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                              Money expected) {
            CheckoutConfiguration configuration = new CheckoutConfiguration();
            PricingRules rules = configuration.pricingRules(
                    (PricingProperties) dataValues.get("properties"), List.of());
            ICheckout checkout = new Checkout(rules,
                    configuration.pricingEngine(configuration.promotionOptimizer()));
            checkout.scan((String) dataValues.get("multiPriceSku"));
            checkout.scan((String) dataValues.get("multiPriceSku"));
            checkout.scan((String) dataValues.get("multiPriceSku"));
            checkout.scan((String) dataValues.get("multiPriceSku"));
            checkout.scan((String) dataValues.get("freeOfferSku"));
            checkout.scan((String) dataValues.get("freeOfferSku"));
            checkout.scan((String) dataValues.get("mealDealSku"));

            assertThat(checkout.getTotal()).isEqualTo(expected);
        }

        static Object[][] pricingRulesDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("properties", null);
            dataDefaults.put("multiPriceSku", "X");
            dataDefaults.put("freeOfferSku", "Y");
            dataDefaults.put("mealDealSku", "Z");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("properties", new PricingProperties(
                    Map.of("X", 100L, "Y", 50L, "Z", 75L),
                    List.of(new PricingProperties.MultiPrice("X", 3, 250L)),
                    List.of(new PricingProperties.BuyNGetOneFree("Y", 1)),
                    List.of(new PricingProperties.MealDeal(Map.of("X", 1, "Z", 1), 125L))));
            cases[0] = builder.addCase(Money.ofPence(425));
            return cases;
        }
    }

    @Nested
    class CatalogueWithoutPromotions {

        @ParameterizedTest
        @MethodSource("pricingRulesDataProvider")
        void pricingRulesTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                              Money expected) {
            CheckoutConfiguration configuration = new CheckoutConfiguration();
            PricingRules rules = configuration.pricingRules(
                    (PricingProperties) dataValues.get("properties"), List.of());
            ICheckout checkout = new Checkout(rules,
                    configuration.pricingEngine(configuration.promotionOptimizer()));
            checkout.scan((String) dataValues.get("sku"));

            assertThat(checkout.getTotal()).isEqualTo(expected);
        }

        static Object[][] pricingRulesDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("properties", null);
            dataDefaults.put("sku", "A");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("properties",
                    new PricingProperties(Map.of("A", 10L), List.of(), List.of(), List.of()));
            cases[0] = builder.addCase(Money.ofPence(10));
            return cases;
        }
    }

    @Nested
    class UnknownPromotionSku {

        @ParameterizedTest
        @MethodSource("pricingRulesDataProvider")
        void pricingRulesTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                              String expected) {
            CheckoutConfiguration configuration = new CheckoutConfiguration();

            assertThatThrownBy(() -> configuration.pricingRules(
                    (PricingProperties) dataValues.get("properties"), List.of()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(expected);
        }

        static Object[][] pricingRulesDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("properties", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("properties", new PricingProperties(Map.of("A", 10L),
                    List.of(new PricingProperties.MultiPrice("Z", 2, 10L)), List.of(), List.of()));
            cases[0] = builder.addCase("Unknown SKU: Z");
            return cases;
        }
    }

    @Nested
    class DefensivePropertyRecords {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             PropertiesExpected expected) {
            Map<String, Long> prices = (Map<String, Long>) dataValues.get("prices");
            List<PricingProperties.MultiPrice> offers =
                    (List<PricingProperties.MultiPrice>) dataValues.get("offers");
            Map<String, Integer> quantities = (Map<String, Integer>) dataValues.get("quantities");
            PricingProperties.MealDeal meal = new PricingProperties.MealDeal(
                    quantities, (Long) dataValues.get("mealPrice"));
            PricingProperties properties = new PricingProperties(prices, offers, List.of(), List.of(meal));
            prices.clear();
            offers.add((PricingProperties.MultiPrice) dataValues.get("addedOffer"));
            quantities.clear();

            assertThat(properties.unitPrices()).containsExactlyEntriesOf(expected.prices());
            assertThat(properties.multiPrices()).isEqualTo(expected.offers()).isEmpty();
            assertThat(meal.quantities()).containsExactlyInAnyOrderEntriesOf(expected.quantities());
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("prices", null);
            dataDefaults.put("offers", null);
            dataDefaults.put("quantities", null);
            dataDefaults.put("mealPrice", 10L);
            dataDefaults.put("addedOffer", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("prices", new HashMap<>(Map.of("A", 10L)));
            dataValues.put("offers", new ArrayList<PricingProperties.MultiPrice>());
            dataValues.put("quantities", new HashMap<>(Map.of("A", 1, "B", 1)));
            dataValues.put("addedOffer", new PricingProperties.MultiPrice("A", 2, 10L));
            cases[0] = builder.addCase(new PropertiesExpected(
                    Map.of("A", 10L), List.of(), Map.of("A", 1, "B", 1)));
            return cases;
        }
    }

    @Nested
    class ExternalCatalogueAndEngineReplacement {

        @ParameterizedTest
        @MethodSource("checkoutFactoryDataProvider")
        void checkoutFactoryTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                                 EngineExpected expected) {
            ApplicationContextRunner runner = (ApplicationContextRunner) dataValues.get("runner");
            List<?> pricedQuantities = (List<?>) dataValues.get("pricedQuantities");
            List<?> pricedPromotions = (List<?>) dataValues.get("pricedPromotions");

            runner.run(context -> {
                assertThat(context).hasNotFailed();
                ICheckout checkout = context.getBean(ICheckoutFactory.class).create();
                checkout.scan((String) dataValues.get("sku"));
                assertThat(checkout.getTotal()).isEqualTo(expected.total());
                assertThat(pricedQuantities).isEqualTo(expected.quantities())
                        .hasSize(expectedCalls.get("calculate"));
                assertThat(pricedPromotions).isEqualTo(expected.promotions())
                        .hasSize(expectedCalls.get("calculate"));
            });
        }

        static Object[][] checkoutFactoryDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("runner", null);
            dataDefaults.put("sku", "X");
            dataDefaults.put("pricedQuantities", null);
            dataDefaults.put("pricedPromotions", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("calculate", 0);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[2][];

            dataValues.put("runner", externalCatalogueRunner());
            dataValues.put("pricedQuantities", new ArrayList<Integer>());
            dataValues.put("pricedPromotions", new ArrayList<List<IPromotion>>());
            cases[0] = builder.addCase(new EngineExpected(Money.ofPence(42), List.of(), List.of()));

            List<Integer> pricedQuantities = new ArrayList<>();
            List<List<IPromotion>> pricedPromotions = new ArrayList<>();
            CheckoutConfiguration configuration = new CheckoutConfiguration();
            IPricingEngine delegate = configuration.pricingEngine(configuration.promotionOptimizer());
            IPricingEngine alternative = (basket, promotions) -> {
                pricedQuantities.add(basket.quantityOf("X"));
                pricedPromotions.add(promotions);
                return delegate.calculate(basket, promotions);
            };
            dataValues.put("runner", externalCatalogueRunner()
                    .withBean("alternativeEngine", IPricingEngine.class, () -> alternative,
                            definition -> definition.setPrimary(true)));
            dataValues.put("pricedQuantities", pricedQuantities);
            dataValues.put("pricedPromotions", pricedPromotions);
            expectedCalls.put("calculate", 1);
            cases[1] = builder.addCase(new EngineExpected(Money.ofPence(42), List.of(1), List.of(List.of())));
            return cases;
        }
    }

    @Nested
    class OptimizerReplacement {

        @ParameterizedTest
        @MethodSource("pricingEngineDataProvider")
        void pricingEngineTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                               OptimizerExpected expected) {
            ApplicationContextRunner runner = (ApplicationContextRunner) dataValues.get("runner");
            List<?> optimizedQuantities = (List<?>) dataValues.get("optimizedQuantities");
            List<?> optimizedRules = (List<?>) dataValues.get("optimizedRules");

            runner.run(context -> {
                assertThat(context).hasNotFailed();
                ICheckout checkout = context.getBean(ICheckoutFactory.class).create();
                checkout.scan((String) dataValues.get("sku"));
                assertThat(checkout.getTotal()).isEqualTo(expected.total());
                assertThat(optimizedQuantities).isEqualTo(expected.quantities())
                        .hasSize(expectedCalls.get("maximumSavings"));
                assertThat(optimizedRules).isEqualTo(expected.rules())
                        .hasSize(expectedCalls.get("maximumSavings"));
            });
        }

        static Object[][] pricingEngineDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("runner", null);
            dataDefaults.put("sku", "X");
            dataDefaults.put("optimizedQuantities", null);
            dataDefaults.put("optimizedRules", null);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("maximumSavings", 1);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            List<Integer> optimizedQuantities = new ArrayList<>();
            List<List<PricingRule>> optimizedRules = new ArrayList<>();
            IPromotionOptimizer alternative = (basket, rules) -> {
                optimizedQuantities.add(basket.quantityOf("X"));
                optimizedRules.add(rules);
                return Money.ZERO;
            };
            dataValues.put("runner", externalCatalogueRunner()
                    .withBean("alternativeOptimizer", IPromotionOptimizer.class, () -> alternative,
                            definition -> definition.setPrimary(true)));
            dataValues.put("optimizedQuantities", optimizedQuantities);
            dataValues.put("optimizedRules", optimizedRules);
            cases[0] = builder.addCase(new OptimizerExpected(Money.ofPence(42), List.of(1), List.of(List.of())));
            return cases;
        }
    }

    @Nested
    class AdditionalPromotionBeans {

        @ParameterizedTest
        @MethodSource("pricingRulesDataProvider")
        void pricingRulesTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                              PromotionTotalsExpected expected) {
            ApplicationContextRunner runner = (ApplicationContextRunner) dataValues.get("runner");
            IPromotion single = (IPromotion) dataValues.get("single");
            IPromotion pair = (IPromotion) dataValues.get("pair");

            runner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(PricingRules.class).promotions())
                        .containsExactlyInAnyOrder(single, pair);
                ICheckout checkout = context.getBean(ICheckoutFactory.class).create();
                checkout.scan((String) dataValues.get("sku"));
                assertThat(checkout.getTotal()).isEqualTo(expected.singleTotal());
                checkout.scan((String) dataValues.get("sku"));
                assertThat(checkout.getTotal()).isEqualTo(expected.pairTotal());
            });
        }

        static Object[][] pricingRulesDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("runner", null);
            dataDefaults.put("single", null);
            dataDefaults.put("pair", null);
            dataDefaults.put("sku", "X");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            PricingRule singleRule = new PricingRule(Map.of("X", 1), Money.ofPence(30));
            IPromotion single = basket -> PromotionResult.eligible(singleRule, basket);
            IPromotion pair = new MultiPricePromotion("X", 2, Money.ofPence(50));
            dataValues.put("runner", externalCatalogueRunner()
                    .withBean("singleOffer", IPromotion.class, () -> single)
                    .withBean("pairOffer", IPromotion.class, () -> pair));
            dataValues.put("single", single);
            dataValues.put("pair", pair);
            cases[0] = builder.addCase(new PromotionTotalsExpected(Money.ofPence(30), Money.ofPence(50)));
            return cases;
        }
    }

    @Nested
    class CombinedImmutablePromotionRegistrations {

        @ParameterizedTest
        @MethodSource("pricingRulesDataProvider")
        @SuppressWarnings("unchecked")
        void pricingRulesTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                              CombinedPromotionsExpected expected) {
            CheckoutConfiguration configuration = new CheckoutConfiguration();
            List<IPromotion> registrations = (List<IPromotion>) dataValues.get("registrations");
            PricingRules rules = configuration.pricingRules(
                    (PricingProperties) dataValues.get("properties"), registrations);
            registrations.clear();

            assertThat(rules.promotions()).hasSize(expected.promotionCount())
                    .contains((IPromotion) dataValues.get("cheaperPair"));
            assertThatThrownBy(() -> rules.promotions().clear()).isInstanceOf(UnsupportedOperationException.class);
            ICheckout checkout = new Checkout(rules,
                    configuration.pricingEngine(configuration.promotionOptimizer()));
            checkout.scan((String) dataValues.get("sku"));
            checkout.scan((String) dataValues.get("sku"));
            assertThat(checkout.getTotal()).isEqualTo(expected.total());
        }

        static Object[][] pricingRulesDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("properties", null);
            dataDefaults.put("registrations", null);
            dataDefaults.put("cheaperPair", null);
            dataDefaults.put("sku", "X");
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            IPromotion cheaperPair = new MultiPricePromotion("X", 2, Money.ofPence(50));
            dataValues.put("properties", new PricingProperties(Map.of("X", 42L),
                    List.of(new PricingProperties.MultiPrice("X", 2, 70L)), List.of(), List.of()));
            dataValues.put("registrations", new ArrayList<>(List.of(cheaperPair)));
            dataValues.put("cheaperPair", cheaperPair);
            cases[0] = builder.addCase(new CombinedPromotionsExpected(2, Money.ofPence(50)));
            return cases;
        }
    }

    @Nested
    class RequiredMultiPricePrice {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             String expected) {
            assertThatThrownBy(() -> new PricingProperties.MultiPrice(
                    (String) dataValues.get("sku"), (Integer) dataValues.get("quantity"),
                    (Long) dataValues.get("price")))
                    .isInstanceOf(NullPointerException.class).hasMessage(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("sku", "A");
            dataDefaults.put("quantity", 2);
            dataDefaults.put("price", 10L);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("price", null);
            cases[0] = builder.addCase("Multiprice price is required");
            return cases;
        }
    }

    @Nested
    class RequiredMealDealPrice {

        @ParameterizedTest
        @MethodSource("constructorDataProvider")
        @SuppressWarnings("unchecked")
        void constructorTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                             String expected) {
            assertThatThrownBy(() -> new PricingProperties.MealDeal(
                    (Map<String, Integer>) dataValues.get("quantities"), (Long) dataValues.get("price")))
                    .isInstanceOf(NullPointerException.class).hasMessage(expected);
        }

        static Object[][] constructorDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("quantities", null);
            dataDefaults.put("price", 10L);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder builder = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] cases = new Object[1][];

            dataValues.put("quantities", new HashMap<>(Map.of("A", 1, "B", 1)));
            dataValues.put("price", null);
            cases[0] = builder.addCase("Meal deal price is required");
            return cases;
        }
    }

    private static ApplicationContextRunner externalCatalogueRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(CheckoutConfiguration.class)
                .withPropertyValues("checkout.unit-prices.X=42", "checkout.multi-prices=",
                        "checkout.buy-n-get-one-free=", "checkout.meal-deals=");
    }

    record PropertiesExpected(Map<String, Long> prices, List<PricingProperties.MultiPrice> offers,
                              Map<String, Integer> quantities) {
    }

    record EngineExpected(Money total, List<Integer> quantities, List<List<IPromotion>> promotions) {
    }

    record OptimizerExpected(Money total, List<Integer> quantities, List<List<PricingRule>> rules) {
    }

    record PromotionTotalsExpected(Money singleTotal, Money pairTotal) {
    }

    record CombinedPromotionsExpected(int promotionCount, Money total) {
    }
}
