package com.mitchell.fluro.checkout.infrastructure;

import com.mitchell.fluro.checkout.application.checkout.CheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PricingProperties.class)
public class CheckoutConfiguration {

    @Bean
    public PricingRules pricingRules(PricingProperties properties) {
        List<Item> items = properties.unitPrices().entrySet().stream()
                .map(entry -> new Item(entry.getKey(), Money.ofPence(entry.getValue())))
                .toList();
        PricingRules catalogue = new PricingRules(items);
        List<IPromotion> promotions = new ArrayList<>();
        properties.multiPrices().forEach(offer -> {
            catalogue.item(offer.sku());
            promotions.add(new MultiPricePromotion(offer.sku(), offer.quantity(), Money.ofPence(offer.price())));
        });
        properties.buyNGetOneFree().forEach(offer -> {
            catalogue.item(offer.sku());
            promotions.add(new BuyNGetOneFreePromotion(offer.sku(), offer.paidQuantity()));
        });
        properties.mealDeals().forEach(offer -> {
            offer.quantities().keySet().forEach(catalogue::item);
            promotions.add(new MealDealPromotion(offer.quantities(), Money.ofPence(offer.price())));
        });
        return new PricingRules(items, promotions);
    }

    @Bean
    public PricingEngine pricingEngine() {
        return new PricingEngine();
    }

    @Bean
    public CheckoutFactory checkoutFactory(PricingRules pricingRules, PricingEngine pricingEngine) {
        return new CheckoutFactory(pricingRules, pricingEngine);
    }
}
