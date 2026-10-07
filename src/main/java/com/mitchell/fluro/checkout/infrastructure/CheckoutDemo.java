package com.mitchell.fluro.checkout.infrastructure;

import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.CommandLineRunner;

public final class CheckoutDemo implements CommandLineRunner {

    private final ICheckoutFactory checkoutFactory;
    private final PrintStream output;

    public CheckoutDemo(ICheckoutFactory checkoutFactory, PrintStream output) {
        this.checkoutFactory = Objects.requireNonNull(checkoutFactory, "Checkout factory is required");
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
            ICheckout checkout = checkoutFactory.create();
            skus.forEach(checkout::scan);
            output.println(String.join(" ", skus) + " -> " + checkout.getTotal().pence() + "p");
        }
    }
}
