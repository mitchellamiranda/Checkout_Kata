package com.mitchell.fluro.checkout.support;

import java.util.HashMap;

/**
 * Builds named-input JUnit 5 method-source rows and restores the working maps after each case.
 * Map snapshots are shallow: providers must create fresh mutable fixtures for each case.
 */
public final class TestCaseBuilder {

    private final HashMap<String, Object> dataValues;
    private final HashMap<String, Integer> expectedCalls;
    private final HashMap<String, Object> dataDefaults;
    private final HashMap<String, Integer> expectedCallsDefaults;

    public TestCaseBuilder(HashMap<String, Object> dataValues,
                           HashMap<String, Integer> expectedCalls,
                           HashMap<String, Object> dataDefaults,
                           HashMap<String, Integer> expectedCallsDefaults) {
        this.dataValues = dataValues;
        this.expectedCalls = expectedCalls;
        this.dataDefaults = new HashMap<>(dataDefaults);
        this.expectedCallsDefaults = new HashMap<>(expectedCallsDefaults);
    }

    public <T> Object[] addCase(T expected) {
        Object[] row = {new HashMap<>(dataValues), new HashMap<>(expectedCalls), expected};
        dataValues.clear();
        dataValues.putAll(dataDefaults);
        expectedCalls.clear();
        expectedCalls.putAll(expectedCallsDefaults);
        return row;
    }
}
