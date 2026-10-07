package com.mitchell.fluro.checkout.domain.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BasketTest {

    private static final Item A = new Item("A", Money.ofPence(50));
    private static final Item B = new Item("B", Money.ofPence(75));

    @Test
    void emptyBasketHasNoItemsAndZeroTotal() {
        Basket basket = Basket.empty();
        assertThat(basket.quantities()).isEmpty();
        assertThat(basket.quantityOf("A")).isZero();
        assertThat(basket.item("A")).isEmpty();
        assertThat(basket.unitTotal()).isEqualTo(Money.ZERO);
    }

    @Test
    void addingItemsCreatesIndependentSnapshots() {
        Basket first = Basket.empty().add(A);
        Basket second = first.add(B).add(A);
        assertThat(first.quantities()).containsExactlyEntriesOf(Map.of(A, 1));
        assertThat(second.quantities()).containsExactlyInAnyOrderEntriesOf(Map.of(A, 2, B, 1));
        assertThat(second.quantityOf("A")).isEqualTo(2);
        assertThat(second.unitTotal()).isEqualTo(Money.ofPence(175));
    }

    @Test
    void defensivelyCopiesAndExposesAnImmutableMap() {
        Map<Item, Integer> source = new HashMap<>(Map.of(A, 2));
        Basket basket = new Basket(source);
        source.clear();
        assertThat(basket.quantityOf("A")).isEqualTo(2);
        assertThatThrownBy(() -> basket.quantities().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveQuantities(int quantity) {
        assertThatThrownBy(() -> new Basket(Map.of(A, quantity)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsConflictingPricesForOneSku() {
        Item differentA = new Item("A", Money.ofPence(60));
        assertThatThrownBy(() -> new Basket(Map.of(A, 1, differentA, 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate SKU");
        assertThatThrownBy(() -> Basket.empty().add(A).add(differentA))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Conflicting price");
    }

    @Test
    void rejectsQuantityOverflow() {
        Basket basket = new Basket(Map.of(A, Integer.MAX_VALUE));
        assertThatThrownBy(() -> basket.add(A)).isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", " A", "A "})
    void rejectsInvalidSkus(String sku) {
        assertThatThrownBy(() -> new Item(sku, Money.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullSource
    void rejectsNullSku(String sku) {
        assertThatThrownBy(() -> new Item(sku, Money.ZERO)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> new Item("A", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Basket(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Basket.empty().add(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void supportsFreeItemsAndMultiCharacterSkus() {
        assertThat(Basket.empty().add(new Item("APPLE-01", Money.ZERO)).unitTotal()).isEqualTo(Money.ZERO);
    }
}
