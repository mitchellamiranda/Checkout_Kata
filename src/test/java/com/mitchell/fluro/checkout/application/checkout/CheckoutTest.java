package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;

import java.util.ArrayList;
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

    private final IPricingEngine pricingEngine = new PricingEngine(new PromotionOptimizer());
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
            ICheckout checkout = new Checkout(rules, pricingEngine);
            scanned.chars().mapToObj(character -> String.valueOf((char) character)).forEach(checkout::scan);
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(expected));
        }
    }

    @Nested
    @DisplayName("Transaction lifecycle")
    class TransactionLifecycle {

        @Test
        void totalsAreRepeatableAndScanningCanContinue() {
            ICheckout checkout = new Checkout(rules, pricingEngine);
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
            ICheckout checkout = new Checkout(rules, pricingEngine);
            checkout.scan("A");
            Basket before = checkout.getBasket();
            assertThatThrownBy(() -> checkout.scan(sku))
                    .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
            assertThat(checkout.getBasket()).isSameAs(before);
        }

        @Test
        void transactionsUseTheirOwnPricesAndBaskets() {
            ICheckout original = new Checkout(rules, pricingEngine);
            ICheckout revised = new Checkout(new PricingRules(List.of(new Item("A", Money.ofPence(10)))),
                    pricingEngine);
            original.scan("A");
            revised.scan("A");
            revised.scan("A");
            assertThat(original.getTotal()).isEqualTo(Money.ofPence(50));
            assertThat(revised.getTotal()).isEqualTo(Money.ofPence(20));
        }

        @Test
        void rejectsMissingDependencies() {
            assertThatThrownBy(() -> new Checkout(null, pricingEngine)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Checkout(rules, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Constructor-injected pricing")
    class InjectedPricing {

        @Test
        void passesEachCurrentSnapshotAndItsRulesToTheInjectedEngine() {
            List<Basket> pricedBaskets = new ArrayList<>();
            IPricingEngine recordingEngine = (basket, promotions) -> {
                pricedBaskets.add(basket);
                assertThat(promotions).isSameAs(rules.promotions());
                return pricingEngine.calculate(basket, promotions);
            };
            ICheckout checkout = new Checkout(rules, recordingEngine);
            checkout.scan("A");
            Basket first = checkout.getBasket();
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(50));
            checkout.scan("B");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(125));
            assertThat(pricedBaskets).containsExactly(first, checkout.getBasket());
        }

        @Test
        void propagatesInjectedEngineFailuresWithoutChangingTheBasket() {
            IllegalStateException failure = new IllegalStateException("Pricing unavailable");
            IPricingEngine failingEngine = (basket, promotions) -> {
                throw failure;
            };
            ICheckout checkout = new Checkout(rules, failingEngine);
            checkout.scan("A");
            Basket before = checkout.getBasket();
            assertThatThrownBy(checkout::getTotal).isSameAs(failure);
            assertThat(checkout.getBasket()).isSameAs(before);
        }
    }
}
