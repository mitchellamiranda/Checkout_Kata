package com.mitchell.fluro.checkout.demo;

import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.CommandLineRunner;

public final class CheckoutDemoRunner implements CommandLineRunner {

    private final ICheckoutFactory checkoutFactory;
    private final PricingRules pricingRules;
    private final PrintStream output;

    public CheckoutDemoRunner(ICheckoutFactory checkoutFactory, PricingRules pricingRules, PrintStream output) {
        this.checkoutFactory = Objects.requireNonNull(checkoutFactory, "Checkout factory is required");
        this.pricingRules = Objects.requireNonNull(pricingRules, "Pricing rules are required");
        this.output = Objects.requireNonNull(output, "Output is required");
    }

    @Override
    public void run(String... args) {
        List<List<String>> baskets = List.of(
                List.of("B", "A", "B"),
                List.of("B", "B", "A"),
                List.of("A", "B", "B", "C", "C", "C", "C", "D", "E"),
                List.of("A", "A", "B", "B", "B", "B", "C", "C", "C", "C",
                        "C", "C", "C", "C", "D", "D", "E", "E"));
        for (List<String> skus : baskets) {
            ICheckout checkout = checkoutFactory.create(pricingRules);
            skus.forEach(checkout::scan);
            output.println(String.join(" ", skus) + " -> " + checkout.getTotal().pence() + "p");
        }
    }
}
