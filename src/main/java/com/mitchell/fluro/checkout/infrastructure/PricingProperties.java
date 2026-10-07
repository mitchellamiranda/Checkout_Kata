package com.mitchell.fluro.checkout.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("checkout")
public record PricingProperties(
        Map<String, Long> unitPrices,
        List<MultiPrice> multiPrices,
        List<BuyNGetOneFree> buyNGetOneFree,
        List<MealDeal> mealDeals) {

    public PricingProperties {
        unitPrices = Map.copyOf(unitPrices);
        multiPrices = List.copyOf(multiPrices);
        buyNGetOneFree = List.copyOf(buyNGetOneFree);
        mealDeals = List.copyOf(mealDeals);
    }

    public record MultiPrice(String sku, int quantity, Long price) {
        public MultiPrice {
            Objects.requireNonNull(price, "Multiprice price is required");
        }
    }

    public record BuyNGetOneFree(String sku, int paidQuantity) {
    }

    public record MealDeal(Map<String, Integer> quantities, Long price) {
        public MealDeal {
            quantities = Map.copyOf(quantities);
            Objects.requireNonNull(price, "Meal deal price is required");
        }
    }
}
