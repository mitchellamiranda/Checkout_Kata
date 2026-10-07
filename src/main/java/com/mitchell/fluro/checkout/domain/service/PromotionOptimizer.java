package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PromotionOptimizer {

    Money maximumSavings(Basket basket, List<PricingRule> rules) {
        List<String> skus = basket.quantities().keySet().stream().map(item -> item.sku()).sorted().toList();
        List<Offer> offers = new ArrayList<>();
        for (PricingRule rule : rules) {
            Money regularPrice = rule.unitTotal(basket);
            if (rule.maximumApplications(basket) > 0 && rule.price().compareTo(regularPrice) < 0) {
                List<Integer> quantities = skus.stream().map(sku -> rule.quantities().getOrDefault(sku, 0)).toList();
                offers.add(new Offer(quantities, regularPrice.subtract(rule.price())));
            }
        }
        List<Integer> initial = skus.stream().map(basket::quantityOf).toList();
        return search(initial, offers);
    }

    private Money search(List<Integer> initial, List<Offer> offers) {
        Map<List<Integer>, Money> savings = new HashMap<>();
        Deque<SearchFrame> pending = new ArrayDeque<>();
        pending.push(new SearchFrame(initial, false));
        while (!pending.isEmpty()) {
            SearchFrame frame = pending.pop();
            if (savings.containsKey(frame.remaining())) {
                continue;
            }
            if (!frame.expanded()) {
                pending.push(new SearchFrame(frame.remaining(), true));
                for (Offer offer : offers) {
                    offer.consume(frame.remaining()).ifPresent(remaining ->
                            pending.push(new SearchFrame(remaining, false)));
                }
            } else {
                Money best = Money.ZERO;
                for (Offer offer : offers) {
                    Optional<List<Integer>> remaining = offer.consume(frame.remaining());
                    if (remaining.isPresent()) {
                        Money candidate = offer.saving().add(savings.get(remaining.orElseThrow()));
                        if (candidate.compareTo(best) > 0) {
                            best = candidate;
                        }
                    }
                }
                savings.put(frame.remaining(), best);
            }
        }
        return savings.get(initial);
    }

    private record SearchFrame(List<Integer> remaining, boolean expanded) {
    }

    private record Offer(List<Integer> quantities, Money saving) {

        Optional<List<Integer>> consume(List<Integer> available) {
            List<Integer> remaining = new ArrayList<>(available.size());
            for (int index = 0; index < available.size(); index++) {
                int quantity = available.get(index) - quantities.get(index);
                if (quantity < 0) {
                    return Optional.empty();
                }
                remaining.add(quantity);
            }
            return Optional.of(List.copyOf(remaining));
        }
    }
}
