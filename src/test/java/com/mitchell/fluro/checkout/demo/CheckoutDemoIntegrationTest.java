package com.mitchell.fluro.checkout.demo;

import com.mitchell.fluro.checkout.CheckoutApplication;
import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CheckoutDemoIntegrationTest {

    @Nested
    @DisplayName("The XML demo profile is opt-in and Boot invokes its runner")
    class ProfileStartup {

        @ParameterizedTest
        @MethodSource("runDataProvider")
        void runTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                     String expected, CapturedOutput output) {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CheckoutApplication.class)
                    .web(WebApplicationType.NONE)
                    .bannerMode(Banner.Mode.OFF)
                    .logStartupInfo(false)
                    .profiles((String[]) dataValues.get("profiles"))
                    .run()) {
                assertThat(context.isActive()).isTrue();
                assertThat(context.getBeansOfType(CommandLineRunner.class))
                        .hasSize(expectedCalls.get("runners"));
                assertThat(context.getBeansOfType(CheckoutDemoRunner.class).keySet())
                        .isEqualTo(dataValues.get("runnerNames"));
                assertThat(context.getBeansOfType(CheckoutDemo.class)).isEmpty();
                assertThat(context.containsBean("checkoutDemo")).isEqualTo(dataValues.get("demoPresent"));
                assertThat(output.getOut()).isEqualTo(expected);
                assertThat(output.getErr()).isEmpty();
            }
        }

        static Object[][] runDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("profiles", new String[0]);
            dataDefaults.put("runnerNames", java.util.Set.of());
            dataDefaults.put("demoPresent", false);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            expectedCallsDefaults.put("runners", 0);
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[3][];

            dataProvider[0] = tcb.addCase("");

            dataValues.put("profiles", new String[]{"demo"});
            dataValues.put("runnerNames", java.util.Set.of("checkoutDemo"));
            dataValues.put("demoPresent", true);
            expectedCalls.put("runners", 1);
            dataProvider[1] = tcb.addCase(String.join(System.lineSeparator(), List.of(
                    "B A B -> 175p",
                    "B B A -> 175p",
                    "A B B C C C C D E -> 550p",
                    "A A B B B B C C C C C C C C D D E E -> 1100p")) + System.lineSeparator());

            dataValues.put("profiles", new String[]{"unrelated"});
            dataProvider[2] = tcb.addCase("");
            return dataProvider;
        }
    }
}
