package com.mitchell.fluro.checkout.domain.pricing;

import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PricingRules {

    private final Map<String, Item> items;
    private final List<IPromotion> promotions;

    public PricingRules(Collection<Item> items) {
        this(items, List.of());
    }

    public PricingRules(Collection<Item> items, Collection<IPromotion> promotions) {
        Map<String, Item> catalogue = new HashMap<>();
        for (Item item : items) {
            Objects.requireNonNull(item, "Catalogue items are required");
            if (catalogue.putIfAbsent(item.sku(), item) != null) {
                throw new IllegalArgumentException("Duplicate catalogue SKU: " + item.sku());
            }
        }
        this.items = Map.copyOf(catalogue);
        this.promotions = List.copyOf(promotions);
    }

    public List<IPromotion> promotions() {
        return promotions;
    }

    public Item item(String sku) {
        Item.requireValidSku(sku);
        Item item = items.get(sku);
        if (item == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        return item;
    }
}
