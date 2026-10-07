package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutTest {

    private final PricingRules rules = new PricingRules(List.of(
            new Item("A", Money.ofPence(50)),
            new Item("B", Money.ofPence(75)),
            new Item("C", Money.ofPence(25)),
            new Item("D", Money.ofPence(150)),
            new Item("E", Money.ofPence(200))));

    @Nested
    @DisplayName("Unit pricing")
    class UnitPricing {

        @ParameterizedTest(name = "{0} costs {1} pence")
        @CsvSource({"'', 0", "A, 50", "B, 75", "C, 25", "D, 150", "E, 200",
                "AAA, 150", "ABCDE, 500", "BAB, 200", "BBA, 200", "ABB, 200"})
        void pricesAnyScanOrder(String scanned, long expected) {
            Checkout checkout = new Checkout(rules);
            scanned.chars().mapToObj(character -> String.valueOf((char) character)).forEach(checkout::scan);
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(expected));
        }
    }

    @Nested
    @DisplayName("Transaction lifecycle")
    class TransactionLifecycle {

        @Test
        void totalsAreRepeatableAndScanningCanContinue() {
            Checkout checkout = new Checkout(rules);
            checkout.scan("A");
            Basket snapshot = checkout.getBasket();
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(50));
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(50));
            checkout.scan("B");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(125));
            assertThat(snapshot.quantityOf("B")).isZero();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", " ", "a", "Z", " A", "A "})
        void invalidScansDoNotMutateTheBasket(String sku) {
            Checkout checkout = new Checkout(rules);
            checkout.scan("A");
            Basket before = checkout.getBasket();
            assertThatThrownBy(() -> checkout.scan(sku))
                    .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
            assertThat(checkout.getBasket()).isSameAs(before);
        }

        @Test
        void transactionsUseTheirOwnPricesAndBaskets() {
            Checkout original = new Checkout(rules);
            Checkout revised = new Checkout(new PricingRules(List.of(new Item("A", Money.ofPence(10)))));
            original.scan("A");
            revised.scan("A");
            revised.scan("A");
            assertThat(original.getTotal()).isEqualTo(Money.ofPence(50));
            assertThat(revised.getTotal()).isEqualTo(Money.ofPence(20));
        }

        @Test
        void rejectsMissingDependencies() {
            assertThatThrownBy(() -> new Checkout(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Checkout(rules, null)).isInstanceOf(NullPointerException.class);
        }
    }
}
