package com.mitchell.fluro.checkout.application.checkout;

@FunctionalInterface
public interface ICheckoutFactory {

    /**
     * Creates a fresh, empty transaction with its own basket.
     */
    ICheckout create();
}
