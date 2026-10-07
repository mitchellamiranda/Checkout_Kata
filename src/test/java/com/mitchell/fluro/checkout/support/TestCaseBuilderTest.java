package com.mitchell.fluro.checkout.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseBuilderTest {

    @ParameterizedTest
    @MethodSource("addCaseDataProvider")
    @DisplayName("Rows snapshot both maps and restore defensive defaults")
    void addCaseTest(HashMap<String, Object> dataValues,
                     HashMap<String, Integer> expectedCalls, String expected) {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("input", "default");
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        expectedCallsDefaults.put("call", 1);
        HashMap<String, Object> workingData = new HashMap<>(dataValues);
        HashMap<String, Integer> workingCalls = new HashMap<>(expectedCalls);
        TestCaseBuilder tcb = new TestCaseBuilder(
                workingData, workingCalls, dataDefaults, expectedCallsDefaults);
        dataDefaults.put("input", "changed externally");
        expectedCallsDefaults.put("call", 99);

        Object[] row = tcb.addCase(expected);

        assertThat(row).hasSize(3);
        assertThat(row[0]).isEqualTo(dataValues).isNotSameAs(workingData);
        assertThat(row[1]).isEqualTo(expectedCalls).isNotSameAs(workingCalls);
        assertThat(row[2]).isEqualTo(expected);
        assertThat(workingData).isEqualTo(Map.of("input", "default"));
        assertThat(workingCalls).isEqualTo(Map.of("call", 1));
        workingData.put("extra", "later");
        workingCalls.put("extra", 2);
        Object[] nextRow = tcb.addCase("next");
        assertThat(row[0]).isEqualTo(dataValues).isNotSameAs(nextRow[0]);
        assertThat(row[1]).isEqualTo(expectedCalls).isNotSameAs(nextRow[1]);
        assertThat(workingData).isEqualTo(Map.of("input", "default"));
        assertThat(workingCalls).isEqualTo(Map.of("call", 1));
    }

    static Object[][] addCaseDataProvider() {
        HashMap<String, Object> dataDefaults = new HashMap<>();
        dataDefaults.put("input", "default");
        HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
        expectedCallsDefaults.put("call", 1);
        HashMap<String, Object> dataValues = new HashMap<>(dataDefaults);
        HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
        TestCaseBuilder tcb = new TestCaseBuilder(
                dataValues, expectedCalls, dataDefaults, expectedCallsDefaults);
        Object[][] dataProvider = new Object[3][];
        dataProvider[0] = tcb.addCase("default result");
        dataValues.put("input", "override");
        dataValues.put("extra", "case only");
        expectedCalls.put("call", 0);
        expectedCalls.put("extra", 3);
        dataProvider[1] = tcb.addCase("overridden result");
        dataValues.clear();
        expectedCalls.clear();
        dataProvider[2] = tcb.addCase(null);
        return dataProvider;
    }
}
