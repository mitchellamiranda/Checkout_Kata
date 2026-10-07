package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class PricingOptimalityTest {

    @ParameterizedTest(name = "exhaustive oracle, deterministic seed {0}")
    @MethodSource("seeds")
    void agreesWithIndependentEnumerationOfEveryOfferCount(int seed) {
        Random random = new Random(seed);
        String[] skus = {"A", "B", "C"};
        long[] prices = {1 + random.nextInt(200), 1 + random.nextInt(200), 1 + random.nextInt(200)};
        int[] counts = {random.nextInt(7), random.nextInt(7), random.nextInt(7)};
        Map<Item, Integer> items = new HashMap<>();
        for (int index = 0; index < skus.length; index++) {
            if (counts[index] > 0) {
                items.put(new Item(skus[index], Money.ofPence(prices[index])), counts[index]);
            }
        }
        int pairSize = 1 + random.nextInt(3);
        int largerSize = pairSize + 1;
        int paid = 1 + random.nextInt(3);
        long pairPrice = random.nextInt(500);
        long largerPrice = random.nextInt(500);
        long abPrice = random.nextInt(400);
        long bcPrice = random.nextInt(400);
        List<IPromotion> promotions = new ArrayList<>(List.of(
                new MultiPricePromotion("A", pairSize, Money.ofPence(pairPrice)),
                new MultiPricePromotion("A", largerSize, Money.ofPence(largerPrice)),
                new BuyNGetOneFreePromotion("B", paid),
                new MealDealPromotion(Map.of("A", 1, "B", 1), Money.ofPence(abPrice)),
                new MealDealPromotion(Map.of("B", 1, "C", 1), Money.ofPence(bcPrice))));
        List<ExpectedOffer> offers = List.of(
                new ExpectedOffer(new int[]{pairSize, 0, 0}, pairPrice),
                new ExpectedOffer(new int[]{largerSize, 0, 0}, largerPrice),
                new ExpectedOffer(new int[]{0, paid + 1, 0}, paid * prices[1]),
                new ExpectedOffer(new int[]{1, 1, 0}, abPrice),
                new ExpectedOffer(new int[]{0, 1, 1}, bcPrice));
        Money expected = Money.ofPence(enumerate(counts, prices, offers, 0));
        Basket basket = new Basket(items);
        PricingEngine engine = new PricingEngine();
        assertThat(engine.calculate(basket, promotions)).isEqualTo(expected);
        Collections.reverse(promotions);
        assertThat(engine.calculate(basket, promotions)).isEqualTo(expected);
    }

    static IntStream seeds() {
        return IntStream.range(0, 250);
    }

    // Deliberately independent of production eligibility, savings, grouping and state-search helpers.
    private static long enumerate(int[] remaining, long[] unitPrices, List<ExpectedOffer> offers, int index) {
        if (index == offers.size()) {
            long total = 0;
            for (int sku = 0; sku < remaining.length; sku++) {
                total += remaining[sku] * unitPrices[sku];
            }
            return total;
        }
        ExpectedOffer offer = offers.get(index);
        int limit = Integer.MAX_VALUE;
        for (int sku = 0; sku < remaining.length; sku++) {
            if (offer.quantities()[sku] > 0) {
                limit = Math.min(limit, remaining[sku] / offer.quantities()[sku]);
            }
        }
        long best = Long.MAX_VALUE;
        for (int count = 0; count <= limit; count++) {
            int[] next = remaining.clone();
            for (int sku = 0; sku < next.length; sku++) {
                next[sku] -= count * offer.quantities()[sku];
            }
            best = Math.min(best, count * offer.price() + enumerate(next, unitPrices, offers, index + 1));
        }
        return best;
    }

    private record ExpectedOffer(int[] quantities, long price) {
    }
}
