package com.mitchell.fluro.checkout;

import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.application.checkout.ICheckoutFactory;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class CheckoutApplicationTest {

    @Nested
    @DisplayName("Main starts a non-web application")
    class Startup {

        @ParameterizedTest
        @MethodSource("mainDataProvider")
        void mainTest(HashMap<String, Object> dataValues,
                      HashMap<String, Integer> expectedCalls, Boolean expected,
                      @Autowired ConfigurableApplicationContext context) {
            assertThat(context.isActive()).isEqualTo(expected);
            assertThat(context.containsBean((String) dataValues.get("webServerBean")))
                    .isEqualTo(dataValues.get("webServerPresent"));
        }

        static Object[][] mainDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("webServerBean", "webServerFactory");
            dataDefaults.put("webServerPresent", false);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[1][];
            dataProvider[0] = tcb.addCase(true);
            return dataProvider;
        }
    }

    @Nested
    @DisplayName("Main wires configured promotions into isolated transactions")
    class ConfiguredTransactions {

        @ParameterizedTest
        @MethodSource("mainDataProvider")
        void mainTest(HashMap<String, Object> dataValues,
                      HashMap<String, Integer> expectedCalls, Money expected,
                      @Autowired ICheckoutFactory factory,
                      @Autowired @Qualifier("pricingRules") PricingRules rules) {
            ICheckout checkout = scanItems(factory.create(rules), (String[]) dataValues.get("skus"));
            assertThat(checkout.getTotal()).isEqualTo(expected);
            assertThat(factory.create(rules).getTotal()).isEqualTo(dataValues.get("emptyTotal"));
        }

        static Object[][] mainDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("emptyTotal", Money.ZERO);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[2][];
            dataValues.put("skus", new String[]{"B", "A", "B"});
            dataProvider[0] = tcb.addCase(Money.ofPence(175));
            dataValues.put("skus", new String[]{"A", "B", "B", "C", "C", "C", "C", "D", "E"});
            dataProvider[1] = tcb.addCase(Money.ofPence(550));
            return dataProvider;
        }
    }

    private static ICheckout scanItems(ICheckout checkout, String[] skus) {
        for (String sku : skus) {
            checkout.scan(sku);
        }
        return checkout;
    }
}
