package com.mitchell.fluro.checkout.domain.service;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRule;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.promotion.PromotionResult;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingEngineTest {

    private static final Item A = new Item("A", Money.ofPence(100));
    private static final Item B = new Item("B", Money.ofPence(100));
    private final PricingEngine engine = new PricingEngine();

    @Test
    @DisplayName("Finds the global optimum rather than taking the biggest discount first")
    void avoidsGreedyAllocation() {
        Basket basket = new Basket(Map.of(A, 6));
        IPromotion fourFor250 = offer(Map.of("A", 4), 250);
        IPromotion threeFor200 = offer(Map.of("A", 3), 200);
        assertThat(engine.calculate(basket, List.of(fourFor250, threeFor200)))
                .isEqualTo(Money.ofPence(400));
        assertThat(engine.calculate(basket, List.of(threeFor200, fourFor250)))
                .isEqualTo(Money.ofPence(400));
    }

    @Test
    void doesNotSpendAnItemTwiceAcrossDifferentOffers() {
        Basket basket = new Basket(Map.of(A, 2, B, 1));
        assertThat(engine.calculate(basket, List.of(offer(Map.of("A", 2), 100),
                offer(Map.of("A", 1, "B", 1), 75)))).isEqualTo(Money.ofPence(175));
    }

    @Test
    void canCombineDifferentOffersForTheSameSku() {
        Basket basket = new Basket(Map.of(A, 5));
        assertThat(engine.calculate(basket, List.of(offer(Map.of("A", 2), 120),
                offer(Map.of("A", 3), 160)))).isEqualTo(Money.ofPence(280));
    }

    @Test
    void keepsUnitPricingWhenOffersAreMoreExpensiveOrEqual() {
        Basket basket = new Basket(Map.of(A, 2));
        assertThat(engine.calculate(basket, List.of(offer(Map.of("A", 2), 300),
                offer(Map.of("A", 2), 200)))).isEqualTo(Money.ofPence(200));
    }

    @Test
    void acceptsZeroPriceBundlesAndDeduplicatesIdenticalRules() {
        Basket basket = new Basket(Map.of(A, 5));
        IPromotion freePair = offer(Map.of("A", 2), 0);
        assertThat(engine.calculate(basket, List.of(freePair, freePair))).isEqualTo(Money.ofPence(100));
    }

    @Test
    void customStrategyCanReturnMultipleAlternativesWithoutEngineChanges() {
        Basket basket = new Basket(Map.of(A, 5));
        IPromotion alternativeBundles = ignored -> new PromotionResult(List.of(
                new PricingRule(Map.of("A", 2), Money.ofPence(120)),
                new PricingRule(Map.of("A", 3), Money.ofPence(160))));
        assertThat(engine.calculate(basket, List.of(alternativeBundles))).isEqualTo(Money.ofPence(280));
    }

    @Test
    void rejectsCandidatesReferencingItemsOutsideTheBasket() {
        IPromotion malformed = ignored -> new PromotionResult(List.of(
                new PricingRule(Map.of("Z", 1), Money.ZERO)));
        assertThatThrownBy(() -> engine.calculate(new Basket(Map.of(A, 1)), List.of(malformed)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("absent from basket");
    }

    @Test
    void ignoresAnInfeasibleCandidateForAnExistingSku() {
        IPromotion infeasible = ignored -> new PromotionResult(List.of(
                new PricingRule(Map.of("A", 2), Money.ZERO)));
        assertThat(engine.calculate(new Basket(Map.of(A, 1)), List.of(infeasible))).isEqualTo(A.unitPrice());
    }

    @Test
    void propagatesStrategyFailures() {
        IPromotion failing = ignored -> {
            throw new IllegalStateException("Invalid promotion configuration");
        };
        assertThatThrownBy(() -> engine.calculate(Basket.empty(), List.of(failing)))
                .isInstanceOf(IllegalStateException.class).hasMessage("Invalid promotion configuration");
    }

    @Test
    void mergesPreviouslySeparateGroupsWhenAnOfferBridgesThem() {
        Item c = new Item("C", Money.ofPence(100));
        Item d = new Item("D", Money.ofPence(100));
        Basket basket = new Basket(Map.of(A, 1, B, 1, c, 1, d, 1));
        IPromotion ab = offer(Map.of("A", 1, "B", 1), 150);
        IPromotion cd = offer(Map.of("C", 1, "D", 1), 150);
        IPromotion bc = offer(Map.of("B", 1, "C", 1), 1);
        assertThat(engine.calculate(basket, List.of(ab, cd, bc))).isEqualTo(Money.ofPence(201));
        assertThat(engine.calculate(basket, List.of(bc, cd, ab))).isEqualTo(Money.ofPence(201));
    }

    private static IPromotion offer(Map<String, Integer> quantities, long price) {
        PricingRule rule = new PricingRule(quantities, Money.ofPence(price));
        return basket -> PromotionResult.eligible(rule, basket);
    }
}
