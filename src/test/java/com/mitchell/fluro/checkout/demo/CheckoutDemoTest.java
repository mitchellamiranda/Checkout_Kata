package com.mitchell.fluro.checkout.demo;

import com.mitchell.fluro.checkout.support.TestCaseBuilder;

import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class CheckoutDemoTest {

    @Nested
    @DisplayName("Standalone main preserves both transactions and starts fresh on every invocation")
    class StandaloneExecution {

        @ParameterizedTest
        @MethodSource("mainDataProvider")
        void mainTest(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls,
                      String expected, CapturedOutput output) {
            invokeMain((String[]) dataValues.get("args"), (Integer) dataValues.get("invocations"));

            assertThat(output.getOut()).isEqualTo(expected);
            assertThat(output.getErr()).isEmpty();
        }

        static Object[][] mainDataProvider() {
            HashMap<String, Object> dataDefaults = new HashMap<>();
            dataDefaults.put("args", new String[0]);
            dataDefaults.put("invocations", 1);
            HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
            HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
            HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
            TestCaseBuilder tcb = new TestCaseBuilder(
                    dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
            Object[][] dataProvider = new Object[3][];
            String transcript = "\nStarting checkout demo...\n"
                    + "Scanning items: \n"
                    + "B\nA\nB\n"
                    + "Checkout total: \n"
                    + "Money[pence=175]\n"
                    + "\nStarting checkout demo...\n"
                    + "Scanning items: \n"
                    + "A\nB\nA\nC\nC\nA\n"
                    + "Checkout total: \n"
                    + "Money[pence=325]\n";

            dataProvider[0] = tcb.addCase(transcript);

            dataValues.put("args", new String[]{"ignored", "--another=argument"});
            dataValues.put("invocations", 2);
            dataProvider[1] = tcb.addCase(transcript.repeat(2));

            dataValues.put("args", null);
            dataValues.put("invocations", 3);
            dataProvider[2] = tcb.addCase(transcript.repeat(3));
            return dataProvider;
        }
    }

    private static void invokeMain(String[] args, int invocations) {
        for (int invocation = 0; invocation < invocations; invocation++) {
            CheckoutDemo.main(args);
        }
    }
}
