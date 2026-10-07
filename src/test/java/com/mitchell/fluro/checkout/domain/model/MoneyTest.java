package com.mitchell.fluro.checkout.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void calculatesExactlyInPence() {
        assertThat(Money.ofPence(50).add(Money.ofPence(75))).isEqualTo(Money.ofPence(125));
        assertThat(Money.ofPence(125).subtract(Money.ofPence(75))).isEqualTo(Money.ofPence(50));
        assertThat(Money.ofPence(25).multiply(3)).isEqualTo(Money.ofPence(75));
        assertThat(Money.ofPence(25).multiply(0)).isEqualTo(Money.ZERO);
        assertThat(Money.ofPence(25)).isLessThan(Money.ofPence(50));
        assertThat(Money.ofPence(50)).isGreaterThan(Money.ZERO);
        assertThat(Money.ZERO.compareTo(Money.ofPence(0))).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, Long.MIN_VALUE})
    void rejectsNegativeAmounts(long amount) {
        assertThatThrownBy(() -> Money.ofPence(amount)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeResultsAndMultipliers() {
        assertThatThrownBy(() -> Money.ZERO.subtract(Money.ofPence(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.ZERO.multiply(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsExplicitlyOnOverflow() {
        Money maximum = Money.ofPence(Long.MAX_VALUE);
        assertThatThrownBy(() -> maximum.add(Money.ofPence(1))).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> maximum.multiply(2)).isInstanceOf(ArithmeticException.class);
    }
}
