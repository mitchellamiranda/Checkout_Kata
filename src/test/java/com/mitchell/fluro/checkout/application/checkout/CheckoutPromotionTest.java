package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutPromotionTest {

    private static final List<Item> ITEMS = List.of(
            new Item("A", Money.ofPence(50)), new Item("B", Money.ofPence(75)),
            new Item("C", Money.ofPence(25)), new Item("D", Money.ofPence(150)),
            new Item("E", Money.ofPence(200)));
    private final PricingRules exerciseRules = new PricingRules(ITEMS, List.of(
            new MultiPricePromotion("B", 2, Money.ofPence(125)),
            new BuyNGetOneFreePromotion("C", 3),
            new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300))));

    @Nested
    @DisplayName("The exercise price list")
    class ExercisePriceList {

        @ParameterizedTest(name = "{0} costs {1} pence")
        @CsvSource({"'', 0", "A, 50", "B, 75", "C, 25", "D, 150", "E, 200",
                "BB, 125", "CCC, 75", "CCCC, 75", "DE, 300", "BAB, 175",
                "ABCDE, 450", "ABBCCCCDE, 550", "AABBBBCCCCCCCCDDEE, 1100", "BBBCCCCCDDE, 750"})
        void combinesAllPromotionTypes(String scanned, long expected) {
            Checkout checkout = new Checkout(exerciseRules);
            scan(checkout, scanned);
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(expected));
        }

        @Test
        void repricesTheWholeBasketAfterEveryScanWithoutConsumingOffers() {
            Checkout checkout = new Checkout(exerciseRules);
            checkout.scan("B");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(75));
            checkout.scan("A");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(125));
            checkout.scan("B");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(175));
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(175));
            checkout.scan("B");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(250));
            assertThat(checkout.getBasket().quantityOf("B")).isEqualTo(3);
        }
    }

    @ParameterizedTest(name = "scan-order shuffle {0}")
    @MethodSource("shuffleSeeds")
    void isIndependentOfScanOrder(int seed) {
        List<String> scanned = new ArrayList<>("AABBBBBCCCCCCCCCDDDEE".chars()
                .mapToObj(character -> String.valueOf((char) character)).toList());
        Collections.shuffle(scanned, new Random(seed));
        Checkout checkout = new Checkout(exerciseRules);
        scanned.forEach(checkout::scan);
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(1350));
    }

    @Nested
    @DisplayName("Offers competing for the same items")
    class OverlappingOffers {

        @Test
        void selectsMultipriceInsteadOfBuyThreeGetOneFreeWhenCheaper() {
            PricingRules rules = new PricingRules(ITEMS, List.of(
                    new BuyNGetOneFreePromotion("C", 3),
                    new MultiPricePromotion("C", 2, Money.ofPence(30))));
            Checkout checkout = new Checkout(rules);
            scan(checkout, "CCCC");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(60));
        }

        @ParameterizedTest
        @CsvSource({"155, 225", "100, 175"})
        void choosesBetweenAFreeItemAndAMealDeal(long mealPrice, long expected) {
            PricingRules rules = new PricingRules(ITEMS, List.of(
                    new BuyNGetOneFreePromotion("C", 3),
                    new MealDealPromotion(Map.of("C", 1, "D", 1), Money.ofPence(mealPrice))));
            Checkout checkout = new Checkout(rules);
            scan(checkout, "CCCCD");
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(expected));
        }

        @ParameterizedTest
        @CsvSource({"DDE, 400", "DDEE, 600"})
        void allocatesMealItemsAgainstCompetingMultipriceBundles(String scanned, long expected) {
            PricingRules rules = new PricingRules(ITEMS, List.of(
                    new MultiPricePromotion("D", 2, Money.ofPence(200)),
                    new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300))));
            Checkout checkout = new Checkout(rules);
            scan(checkout, scanned);
            assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(expected));
        }
    }

    static IntStream shuffleSeeds() {
        return IntStream.range(0, 50);
    }

    private static void scan(Checkout checkout, String scanned) {
        scanned.chars().mapToObj(character -> String.valueOf((char) character)).forEach(checkout::scan);
    }
}
