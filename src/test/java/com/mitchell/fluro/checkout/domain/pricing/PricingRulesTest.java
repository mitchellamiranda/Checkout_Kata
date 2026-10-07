package com.mitchell.fluro.checkout.domain.pricing;

import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.promotion.IPromotion;
import com.mitchell.fluro.checkout.domain.promotion.PromotionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingRulesTest {

    private static final Item A = new Item("A", Money.ofPence(50));

    @Test
    void snapshotsTheSuppliedCatalogue() {
        List<Item> source = new ArrayList<>(List.of(A));
        PricingRules rules = new PricingRules(source);
        source.clear();
        assertThat(rules.item("A")).isEqualTo(A);
    }

    @Test
    void snapshotsPromotionRegistrations() {
        IPromotion promotion = basket -> PromotionResult.NONE;
        List<IPromotion> source = new ArrayList<>(List.of(promotion));
        PricingRules rules = new PricingRules(List.of(A), source);
        source.clear();
        assertThat(rules.promotions()).containsExactly(promotion);
        assertThatThrownBy(() -> rules.promotions().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new PricingRules(List.of(A), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PricingRules(List.of(A), Arrays.asList(promotion, null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsDuplicateSkusEvenWhenPricesAgree() {
        assertThatThrownBy(() -> new PricingRules(List.of(A, A)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate catalogue SKU");
        assertThatThrownBy(() -> new PricingRules(List.of(A, new Item("A", Money.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingCatalogueOrItems() {
        assertThatThrownBy(() -> new PricingRules(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PricingRules(Arrays.asList(A, null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reportsUnknownSkuExplicitly() {
        PricingRules rules = new PricingRules(List.of());
        assertThatThrownBy(() -> rules.item("A")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown SKU: A");
    }
}
