package com.mitchell.fluro.checkout.infrastructure;

import com.mitchell.fluro.checkout.application.checkout.Checkout;
import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.IPromotionOptimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutConfigurationTest {

    private final CheckoutConfiguration configuration = new CheckoutConfiguration();
    private final IPricingEngine pricingEngine = configuration.pricingEngine(configuration.promotionOptimizer());
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CheckoutConfiguration.class)
            .withPropertyValues("checkout.unit-prices.X=42", "checkout.multi-prices=",
                    "checkout.buy-n-get-one-free=", "checkout.meal-deals=");

    @Test
    void constructsRulesFromAlternativeDataWithoutEngineChanges() {
        PricingProperties properties = new PricingProperties(
                Map.of("X", 100L, "Y", 50L, "Z", 75L),
                List.of(new PricingProperties.MultiPrice("X", 3, 250L)),
                List.of(new PricingProperties.BuyNGetOneFree("Y", 1)),
                List.of(new PricingProperties.MealDeal(Map.of("X", 1, "Z", 1), 125L)));
        ICheckout checkout = new Checkout(configuration.pricingRules(properties), pricingEngine);
        for (String sku : List.of("X", "X", "X", "X", "Y", "Y", "Z")) {
            checkout.scan(sku);
        }
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(425));
    }

    @Test
    void supportsACatalogueWithoutPromotions() {
        PricingProperties properties = new PricingProperties(Map.of("A", 10L), List.of(), List.of(), List.of());
        ICheckout checkout = new Checkout(configuration.pricingRules(properties), pricingEngine);
        checkout.scan("A");
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(10));
    }

    @Test
    void rejectsUnknownPromotionSkusAtConfigurationTime() {
        PricingProperties properties = new PricingProperties(Map.of("A", 10L),
                List.of(new PricingProperties.MultiPrice("Z", 2, 10L)), List.of(), List.of());
        assertThatThrownBy(() -> configuration.pricingRules(properties))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown SKU: Z");
    }

    @Test
    void configurationRecordsDefensivelyCopyCollections() {
        Map<String, Long> prices = new HashMap<>(Map.of("A", 10L));
        List<PricingProperties.MultiPrice> offers = new ArrayList<>();
        Map<String, Integer> quantities = new HashMap<>(Map.of("A", 1, "B", 1));
        PricingProperties.MealDeal meal = new PricingProperties.MealDeal(quantities, 10L);
        PricingProperties properties = new PricingProperties(prices, offers, List.of(), List.of(meal));
        prices.clear();
        offers.add(new PricingProperties.MultiPrice("A", 2, 10L));
        quantities.clear();
        assertThat(properties.unitPrices()).containsExactlyEntriesOf(Map.of("A", 10L));
        assertThat(properties.multiPrices()).isEmpty();
        assertThat(meal.quantities()).containsExactlyInAnyOrderEntriesOf(Map.of("A", 1, "B", 1));
    }

    @Test
    void bindsAnExternalCatalogueWithExplicitlyEmptyPromotionLists() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ICheckout checkout = context.getBean(ICheckoutFactory.class).create();
                    checkout.scan("X");
                    assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(42));
                });
    }

    @Test
    void injectsAnAlternativePricingEngineIntoTheFactory() {
        List<Integer> pricedQuantities = new ArrayList<>();
        IPricingEngine alternative = (basket, promotions) -> {
            pricedQuantities.add(basket.quantityOf("X"));
            return pricingEngine.calculate(basket, promotions);
        };
        contextRunner.withBean("alternativeEngine", IPricingEngine.class, () -> alternative,
                        definition -> definition.setPrimary(true))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ICheckout checkout = context.getBean(ICheckoutFactory.class).create();
                    checkout.scan("X");
                    assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(42));
                    assertThat(pricedQuantities).containsExactly(1);
                });
    }

    @Test
    void injectsAnAlternativeOptimizerIntoThePricingEngine() {
        List<Integer> optimizedQuantities = new ArrayList<>();
        IPromotionOptimizer alternative = (basket, rules) -> {
            optimizedQuantities.add(basket.quantityOf("X"));
            assertThat(rules).isEmpty();
            return Money.ZERO;
        };
        contextRunner.withBean("alternativeOptimizer", IPromotionOptimizer.class, () -> alternative,
                        definition -> definition.setPrimary(true))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ICheckout checkout = context.getBean(ICheckoutFactory.class).create();
                    checkout.scan("X");
                    assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(42));
                    assertThat(optimizedQuantities).containsExactly(1);
                });
    }

    @Test
    void missingPricesCannotSilentlyBecomeFreeOffers() {
        assertThatThrownBy(() -> new PricingProperties.MultiPrice("A", 2, null))
                .isInstanceOf(NullPointerException.class).hasMessage("Multiprice price is required");
        assertThatThrownBy(() -> new PricingProperties.MealDeal(Map.of("A", 1, "B", 1), null))
                .isInstanceOf(NullPointerException.class).hasMessage("Meal deal price is required");
    }
}
