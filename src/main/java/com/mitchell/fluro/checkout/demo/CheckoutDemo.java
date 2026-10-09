package com.mitchell.fluro.checkout.demo;

import com.mitchell.fluro.checkout.application.checkout.CheckoutFactory;
import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import java.util.List;

import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;

public final class CheckoutDemo {

    private static final IPricingEngine engine = new PricingEngine(new PromotionOptimizer());
    private static final ICheckoutFactory factory = new CheckoutFactory(engine);

    public static void main(String[] args) {
        var rules = new PricingRules(
            List.of(
                new Item("A", Money.ofPence(50)),
                new Item("B", Money.ofPence(75))
            ),
            List.of(
                new MultiPricePromotion("B", 2, Money.ofPence(125))
            )
        );
        ICheckout checkout = factory.create(rules);

        System.out.print("\nStarting checkout demo...\n");
        System.out.print("Scanning items: \n");

        checkout.scan("B");
        System.out.print("B\n");

        checkout.scan("A");
        System.out.print("A\n");

        checkout.scan("B");
        System.out.print("B\n");

        Money total = checkout.getTotal(); // 175p

        System.out.print("Checkout total: \n");
        System.out.print(total);
        System.out.print("\n");


        var revisedRules = new PricingRules(
            List.of(
                new Item("A", Money.ofPence(50)),
                new Item("B", Money.ofPence(150)),
                new Item("C", Money.ofPence(50))
            ),
            List.of(
                new MultiPricePromotion("C", 2, Money.ofPence(75)),
                new BuyNGetOneFreePromotion("A", 2)
            )
        );
        ICheckout nextTransaction = factory.create(revisedRules);

        System.out.print("\nStarting checkout demo...\n");
        System.out.print("Scanning items: \n");

        nextTransaction.scan("A");
        System.out.print("A\n");

        nextTransaction.scan("B");
        System.out.print("B\n");

        nextTransaction.scan("A");
        System.out.print("A\n");

        nextTransaction.scan("C");
        System.out.print("C\n");

        nextTransaction.scan("C");
        System.out.print("C\n");

        nextTransaction.scan("A");
        System.out.print("A\n");

        Money nextTotal = nextTransaction.getTotal();

        System.out.print("Checkout total: \n");
        System.out.print(nextTotal);
        System.out.print("\n");
    }
}