package com.mitchell.fluro.checkout.domain.pricing;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.PromotionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingRuleTest {

    @Test
    void computesBundleLimitAndUndiscountedValue() {
        PricingRule rule = new PricingRule(Map.of("A", 2, "B", 1), Money.ofPence(100));
        Basket basket = new Basket(Map.of(new Item("A", Money.ofPence(50)), 7,
                new Item("B", Money.ofPence(75)), 2));
        assertThat(rule.maximumApplications(basket)).isEqualTo(2);
        assertThat(rule.unitTotal(basket)).isEqualTo(Money.ofPence(175));
        assertThat(PromotionResult.eligible(rule, Basket.empty())).isEqualTo(PromotionResult.NONE);
        assertThat(PromotionResult.eligible(rule, basket).rules()).containsExactly(rule);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonConsumingRules(int quantity) {
        assertThatThrownBy(() -> new PricingRule(Map.of("A", quantity), Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyInvalidAndNullRules() {
        assertThatThrownBy(() -> new PricingRule(Map.of(), Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PricingRule(Map.of(" ", 1), Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PricingRule(Map.of("A", 1), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PricingRule(null, Money.ZERO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PromotionResult(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void snapshotsBothRuleAndResultCollections() {
        Map<String, Integer> quantities = new HashMap<>(Map.of("A", 2));
        PricingRule rule = new PricingRule(quantities, Money.ofPence(75));
        List<PricingRule> rules = new ArrayList<>(List.of(rule));
        PromotionResult result = new PromotionResult(rules);
        quantities.clear();
        rules.clear();
        assertThat(rule.quantities()).containsExactlyEntriesOf(Map.of("A", 2));
        assertThat(result.rules()).containsExactly(rule);
        assertThatThrownBy(() -> rule.quantities().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.rules().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
