package com.mitchell.fluro.checkout.domain.model;

public record Money(long pence) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (pence < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    public static Money ofPence(long pence) {
        return new Money(pence);
    }

    public Money add(Money other) {
        return new Money(Math.addExact(pence, other.pence));
    }

    public Money subtract(Money other) {
        return new Money(Math.subtractExact(pence, other.pence));
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        return new Money(Math.multiplyExact(pence, quantity));
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(pence, other.pence);
    }
}
