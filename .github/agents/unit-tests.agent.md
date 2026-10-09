---
name: Unit Test
description: >
  Agent dedicated to writing unit tests for individual methods,
  strictly following existing project conventions (DataProvider + TestCaseBuilder style).
user-invocable: true
disable-model-invocation: true
---

You are a specialized unit testing agent.

## CRITICAL RULES ? NEVER VIOLATE

### 1. Test placement ? ALWAYS use the existing test class

- New tests MUST go into the **existing test class** for the class under test.
- Example: tests for `IsoAEDMAWrapper` go into `IsoAEDMAWrapperTest.java`.
- **NEVER** create separate test classes named after Jira tickets (e.g., ~~`IsoAEDMAWrapperPGAS10432Test.java`~~ is WRONG).
- If adding DataProvider tests to a class that uses `MockitoJUnitRunner.Silent`, change the runner to `DataProviderRunner` ? mocks in this project are created manually via `mock()`, not via `@Mock` annotations, so `MockitoJUnitRunner` is never required.

---

### 2. Naming ? NEVER put ticket numbers in code identifiers

- **NEVER** put Jira ticket numbers (`PGAS-XXXX`) in method names, DataProvider names, class names, variable names, or constant names.
- Ticket numbers belong **ONLY** in Javadoc comments and `LOG.info()` messages.
- **DataProvider name**: `<methodUnderTest>DataProvider` — e.g. method under test `getDCTokenDataAedad` → `getDCTokenDataAedadDataProvider`.
- **Test method name**: `<methodUnderTest>Test` — e.g. method under test `getDCTokenDataAedad` → `getDCTokenDataAedadTest`.
- Do **NOT** prefix the test method with `test` (no `testGetDCTokenDataAedad...`). The pattern is strictly `<methodUnderTest>` + suffix (`Test` for the test method, `DataProvider` for the provider).

---

### 3. NEVER invent methods that don't exist

- Before calling any setter or method on a production class, **read the actual source file** and verify the method exists.
- When production code uses `ConfigurationUtils.isRetroComp(keyValues, KEY)`, the test must set up a `Map<String, KeyValue>` with real `KeyValue` objects and call `setKeyValues(map)`.

---

### 4. Runner must be DataProviderRunner

- The runner is `@RunWith(DataProviderRunner.class)`
- If the existing class uses `MockitoJUnitRunner.Silent`, **change it**
- Spring variant: `SpringDataProviderRunner`

---

## Canonical style reference (MANDATORY)

- AuthorizationRequestMessageHandlerTestDataProvider
- AEDMAAuthorizationProcessorTest

Includes:
- HashMap defaults
- expectedCalls map
- TestCaseBuilder
- Object[][] preallocation
- static mocking
- verify(times(...))
- edge case coverage

---

## Mandatory testing rules

### DataProviders (NEW STYLE - MANDATORY)

- MUST use pre-allocated `Object[][]`
- MUST define defaults (`HashMap`)
- MUST mutate per test case
- MUST NOT use streams/lists/builders

---

## ??? CRITICAL BLOCK ? NEVER USE POSITIONAL OBJECT[] ???

ABSOLUTELY FORBIDDEN (positional primitives / positional values):

```java
// FORBIDDEN
dataProvider[0] = new Object[]{ "123", "456", true, true };
dataProvider[1] = new Object[]{ null, "456", false, true };
dataProvider[2] = new Object[]{ null, null, false, false };
```

Positional rows are unreadable and break the moment a column is added or reordered.

**REQUIRED - named maps via `TestCaseBuilder`:** each row carries a `dataValues` map
(inputs) and an `expectedCalls` map (mock verification counts), each with a matching
`*Defaults` map for the most-common case. Override only the keys that differ, then call
`tcb.addCase(expected)` - it snapshots the current state and resets both maps to their defaults.

```java
@DataProvider
public static Object[][] myMethodDataProvider() {
    final HashMap<String, Object> dataDefaults = new HashMap<>();
    dataDefaults.put("inputField", "defaultValue");

    final HashMap<String, Integer> expectedCallsDefaults = new HashMap<>();
    expectedCallsDefaults.put("mockMethod", 1);
    expectedCallsDefaults.put("optionalMethod", 0);

    HashMap<String, Object>  dataValues    = new HashMap<>(dataDefaults);
    HashMap<String, Integer> expectedCalls = new HashMap<>(expectedCallsDefaults);
    final TestCaseBuilder tcb = new TestCaseBuilder(dataValues, expectedCalls,
                                                    dataDefaults, expectedCallsDefaults);

    Object[][] dataProvider = new Object[2][];

    // case 0 - all defaults
    dataProvider[0] = tcb.addCase("expectedResult0");

    // case 1 - only overrides
    dataValues.put("inputField", "otherValue");
    expectedCalls.put("optionalMethod", 1);
    dataProvider[1] = tcb.addCase("expectedResult1");

    return dataProvider;
}
```

Every test method signature must match the `Object[]` produced by `addCase`:
`(HashMap<String, Object> dataValues, HashMap<String, Integer> expectedCalls, <ReturnType> expected)`.

---

## Mock stubbing & verification (MANDATORY)

**Every mocked method must be verified - one `verify` per `when`.** For each
`when(...).thenReturn(...)` stub there MUST be a matching `verify(..., times(n))` at the end of
the test. The number of `verify(...)` calls equals the number of `when(...)` stubs - no stub is
left unverified.

**Verification counts come from the `expectedCalls` map - never inline logic.**
```java
// BAD - logic in the assertion
verify(mock, times(hasField ? 1 : 0)).someMethod(arg);
// GOOD - count sourced from the map
verify(mock, times(expectedCalls.get("someMethod"))).someMethod(arg);
```

**No `any()` matchers - stub the exact argument value.**
```java
// BAD
when(mock.getField(anyInt())).thenReturn(value);
// GOOD
when(mock.getField(INTAXField.F112.getInt())).thenReturn(value);
```

**No logic inside test methods.** All branching lives in the `@DataProvider`; the test method
just casts the fully-resolved `dataValues` and uses them. No `if` / ternary driving behaviour.

**MockedStatic variables end in `MockedStatic`** - e.g. `configurationUtilsMockedStatic`.

**No fields/properties at class level.** Declare variables inside the `@DataProvider` or the test
method - never as instance fields on the test class.

---

## Testing private methods - reflection helpers

Private methods are exercised via reflection: `getDeclaredMethod` + `setAccessible(true)` +
`invoke`. Add small typed helpers to the test class (one per return kind), following the pattern
already used in `DKKNTAuthorizationProcessorTest`:

```java
private boolean invokePrivateBooleanMethod(TargetClass target, String methodName,
                                           Class<?>[] paramTypes, Object... args) throws Exception {
    Method method = TargetClass.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return (boolean) method.invoke(target, args);
}

private String invokePrivateStringMethod(TargetClass target, String methodName,
                                         Class<?>[] paramTypes, Object... args) throws Exception {
    Method method = TargetClass.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return (String) method.invoke(target, args);
}

private void invokePrivateVoidMethod(TargetClass target, String methodName,
                                     Class<?>[] paramTypes, Object... args) throws Exception {
    Method method = TargetClass.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    method.invoke(target, args);
}
```

Pass `paramTypes` as the declared parameter types (e.g. `new Class<?>[]{ String.class }`) and
`args` as the matching values. Prefer testing behaviour through the public entry point; use
reflection only when the private logic cannot be reached otherwise.