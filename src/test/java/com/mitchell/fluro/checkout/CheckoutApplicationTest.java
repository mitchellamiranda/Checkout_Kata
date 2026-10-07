package com.mitchell.fluro.checkout;

import com.mitchell.fluro.checkout.application.checkout.Checkout;
import com.mitchell.fluro.checkout.application.checkout.CheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Money;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class CheckoutApplicationTest {

    @Test
    void startsWithoutWebInfrastructure(@Autowired ConfigurableApplicationContext context) {
        assertThat(context.isActive()).isTrue();
        assertThat(context.containsBean("webServerFactory")).isFalse();
    }

    @Test
    void wiresConfiguredRulesIntoIsolatedTransactions(@Autowired CheckoutFactory factory) {
        Checkout checkout = factory.create();
        checkout.scan("B");
        checkout.scan("A");
        checkout.scan("B");
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(175));
        assertThat(factory.create().getTotal()).isEqualTo(Money.ZERO);
    }

    @Test
    void loadsAllExercisePromotionsFromConfiguration(@Autowired CheckoutFactory factory) {
        Checkout checkout = factory.create();
        for (String sku : new String[]{"A", "B", "B", "C", "C", "C", "C", "D", "E"}) {
            checkout.scan(sku);
        }
        assertThat(checkout.getTotal()).isEqualTo(Money.ofPence(550));
    }
}
