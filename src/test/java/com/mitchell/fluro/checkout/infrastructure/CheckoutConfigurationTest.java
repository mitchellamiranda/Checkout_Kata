package com.mitchell.fluro.checkout.infrastructure;

import com.mitchell.fluro.checkout.application.checkout.Checkout;
import com.mitchell.fluro.checkout.application.checkout.CheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutConfigurationTest {

    private final CheckoutConfiguration configuration = new CheckoutConfiguration();

    @Test
    void constructsRulesFromAlternativeDataWithoutEngineChanges() {
        PricingProperties properties = new PricingProperties(
                Map.of("X", 100L, "Y", 50L, "Z", 75L),
                List.of(new PricingProperties.MultiPrice("X", 3, 250)),
                List.of(new PricingProperties.BuyNGetOneFree("Y", 1)),
                List.of(new PricingProperties.MealDeal(Map.of("X", 1, "Z", 1), 125)));
        Checkout checkout = new Checkout(configuration.pricingRules(properties));
        for (String sku : List.of("X", "X", "X", "X", "Y", "Y", "Z")) {
            checkout.scan(sku);
        }
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(425));
    }

    @Test
    void supportsACatalogueWithoutPromotions() {
        PricingProperties properties = new PricingProperties(Map.of("A", 10L), List.of(), List.of(), List.of());
        Checkout checkout = new Checkout(configuration.pricingRules(properties));
        checkout.scan("A");
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(10));
    }

    @Test
    void rejectsUnknownPromotionSkusAtConfigurationTime() {
        PricingProperties properties = new PricingProperties(Map.of("A", 10L),
                List.of(new PricingProperties.MultiPrice("Z", 2, 10)), List.of(), List.of());
        assertThatThrownBy(() -> configuration.pricingRules(properties))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown SKU: Z");
    }

    @Test
    void configurationRecordsDefensivelyCopyCollections() {
        Map<String, Long> prices = new HashMap<>(Map.of("A", 10L));
        List<PricingProperties.MultiPrice> offers = new ArrayList<>();
        Map<String, Integer> quantities = new HashMap<>(Map.of("A", 1, "B", 1));
        PricingProperties.MealDeal meal = new PricingProperties.MealDeal(quantities, 10);
        PricingProperties properties = new PricingProperties(prices, offers, List.of(), List.of(meal));
        prices.clear();
        offers.add(new PricingProperties.MultiPrice("A", 2, 10));
        quantities.clear();
        assertThat(properties.unitPrices()).containsExactlyEntriesOf(Map.of("A", 10L));
        assertThat(properties.multiPrices()).isEmpty();
        assertThat(meal.quantities()).containsExactlyInAnyOrderEntriesOf(Map.of("A", 1, "B", 1));
    }

    @Test
    void factoryRejectsMissingDependencies() {
        assertThatThrownBy(() -> new CheckoutFactory(null, new PricingEngine()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CheckoutFactory(new PricingRules(List.of()), null))
                .isInstanceOf(NullPointerException.class);
    }
}
