# Copilot instructions

## Collaboration and scope

- Do not assume missing requirements, business rules, API behaviour or user intent.
  Read the current code and README first. If a decision remains ambiguous, ask
  the user before implementing it; do not silently choose a behaviour.
- Ask before changing the technology stack, architecture, public contracts,
  promotion semantics, configuration mechanism or dependency set.
- Keep responses concise and direct ("caveman ultra"). Keep code, documentation
  and commit messages professional and clear enough for a technical interview.
- Preserve unrelated and uncommitted work. Never revert, stage or commit another
  change merely because it is present in the working tree.
- Do not commit or push unless requested. When requested, follow the commit
  conventions below. Do not turn explanatory questions into unsolicited edits.

## Project requirements

This is the Fluro checkout take-home exercise. Prioritize correctness,
maintainability, extensibility and interview-quality Java engineering.

- Use Java 21, Spring Boot 3.x, Maven, JUnit 5, AssertJ and JaCoCo.
- Keep the domain and application usable as a plain Java library without Spring.
- Keep Spring XML dependency injection and the optional console demonstrations.
  Do not replace XML with YAML or Java `@Bean` factories.
- Do not add a database, REST controllers, frontend, interactive menu, deployment
  infrastructure or external services without explicit approval.
- Read the supplied exercise PDF when available and the README for requirements.
  If requirements conflict, ask which takes precedence.

## Architecture and SOLID

Use packages under `com.mitchell.fluro.checkout`:

| Package | Responsibility |
| --- | --- |
| `domain.model` | Immutable money, items and basket snapshots |
| `domain.pricing` | Pricing snapshots and bundle values |
| `domain.promotion` | Promotion strategies and candidate results |
| `domain.service` | Pricing coordination and exact allocation |
| `application.checkout` | Transaction orchestration and factory |
| `infrastructure` | Spring XML bootstrap |
| `demo` | Standalone demo and optional Spring runner |

- Prefix all project-owned interfaces with `I`, such as `ICheckout`,
  `ICheckoutFactory`, `IPricingEngine`, `IPromotionOptimizer` and `IPromotion`.
- Use constructor injection for required service dependencies. Keep dependencies
  final; do not construct service collaborators inside their consumers.
- Separate scanning, transaction creation, offer generation and allocation.
  Avoid god objects, promotion-type switches and if/else dispatch chains.
- Depend on small behavioural interfaces. Do not mechanically add interfaces to
  immutable values such as `Money`, `PricingRule`, `PricingRules` or `PromotionResult`.
- Honour interface contracts: replacement implementations must preserve exact
  pricing, non-mutation and transaction isolation, not merely compile.
- New promotions implement `IPromotion` and register as XML beans. Existing
  promotion classes and pricing algorithms must not need type-specific changes.
- The factory intentionally constructs fresh `Checkout` transactions. This is
  different from a consumer constructing its own pricing service.

## Pricing and transaction contracts

- All amounts are integer pence. Use checked arithmetic; reject negative values
  and overflow. Do not introduce currency conversion or rounding assumptions.
- Bundled prices: A=50, B=75, C=25, D=150, E=200.
- Bundled offers: two B for 125; buy three C and get a fourth scanned C free;
  one D plus one E for 300. Keep these values in configuration, not algorithms.
- Scan order must not affect the total. Repeated total calculations must not
  mutate the basket or consume offers; scanning can continue afterwards.
- Repeat promotions for complete groups. Charge leftovers at unit price.
  Do not create unscanned free items or allocate one item to multiple offers.
- Choose the globally lowest basket total, not the largest immediate discount.
  Combine compatible offers; ignore offers that cost more than unit pricing.
- Each `IPromotion` returns all eligible repeatable bundle alternatives.
  Allocation belongs to the optimizer, not the promotion implementation.
- Preserve exact search, independent-group optimization and the independent
  exhaustive oracle. Do not silently replace exact pricing with a heuristic.
- `CheckoutFactory` receives only its `IPricingEngine`; `create(PricingRules)`
  requires an explicit rules snapshot on every call. Do not restore a factory-
  held snapshot or a no-argument `create()`.
- Existing transactions retain their original prices and promotions, including
  for later scans. Null rules and invalid scans fail explicitly.
- Keep baskets and pricing collections immutable. Custom promotions must also
  be immutable. A checkout is thread-confined, not a shared singleton.
- Rule loading/refresh is the caller's responsibility. Editing XML does not
  automatically refresh an already-running transaction.

## XML and demonstrations

- `ApplicationContext.xml` imports pricing, service and optional demo contexts.
- `PricingRulesContext.xml` defines item, money, catalogue and promotion beans.
  Use unique bean IDs for multiple instances of the same promotion type.
- `pricingRules` constructor-autowires all `Item` and `IPromotion` beans.
  Keep the explicit catalogue consistent with item beans referenced by offers.
- `CheckoutServicesContext.xml` connects services through `<constructor-arg ref>`.
  Service wiring must remain usable without a configured rules snapshot.
- `DemoContext.xml` enables `CheckoutDemoRunner` only under the `demo` profile.
  Keep `CheckoutDemo.main` independently runnable without Spring.
- Demo totals must come from real checkout calculations, never hardcoded output.
- Preserve `--checkout.context` support for an external root. Invalid or missing
  resources must fail startup rather than silently load alternative prices.
- Treat XML as trusted application configuration, not untrusted input.

## Tests: use the `Unit Test` agent

- Always invoke the **`Unit Test` agent** to create, modify or refactor tests.
  Give it the relevant implementation, desired behaviour, existing conventions
  and allowed scope. Review its changes and results before finishing.
- If that agent is unavailable, tell the user and ask how to proceed. Do not
  silently bypass this requirement.
- Explicitly tell the agent to retain the approved **JUnit 5 adaptation**.
  Use `@ParameterizedTest` and `@MethodSource`, not JUnit 4 DataProvider runners.
  Do not introduce JUnit Vintage or Mockito merely to match agent examples.
- Reuse `src/test/java/com/mitchell/fluro/checkout/support/TestCaseBuilder.java`.
  Providers preallocate `Object[][]`; each row contains
  `HashMap<String, Object> dataValues`, `HashMap<String, Integer> expectedCalls`
  and a typed expected result.
- Maintain defaults for both maps. `addCase(expected)` snapshots the maps and
  resets them. Copies are shallow: create fresh mutable fixtures for each case.
- Name methods `<methodUnderTest>Test` and `<methodUnderTest>DataProvider`.
  Use `constructorTest` and `constructorDataProvider` for constructors.
  Use nested contexts and meaningful display names to distinguish scenarios.
- Avoid shared fixture fields and positional input rows. Put conditional setup
  and generated scenarios in providers/helpers; keep test bodies focused on
  invocation and assertions. Do not hide entire tests in executable map values.
- Use real implementations or recording doubles where appropriate.
  Assert exact inputs and interaction counts through `expectedCalls`.
  Preserve method-parameter injection for Spring integration where necessary.
- Cover unit prices, empty baskets, every promotion threshold, repeated offers,
  remainders, overlaps, scan order, large baskets, invalid inputs, immutability,
  overflow, dependency injection and per-transaction rule changes.
- Preserve the 250 deterministic oracle scenarios, both registration orders,
  scan-order permutations, large-basket cases and actual XML integration.
- Never weaken assertions, delete useful scenarios or exclude production code
  merely to satisfy coverage. Aim to retain full meaningful line/branch coverage.

## Validation and documentation

Use the existing Maven build; do not add tooling just to run validation.
Start with targeted tests, then run `clean verify` for a completed code change.
Documentation-only edits do not require a build.

```powershell
.\mvnw.cmd '-Dtest=RelevantTestClass' test
.\mvnw.cmd clean verify
```

Standard Maven equivalents: `mvn clean test` and `mvn verify`.
JaCoCo report: `target/site/jacoco/index.html`. Current enforced minimums are
95% lines and 90% branches; do not lower them. Compiler warnings fail the build.
Do not claim a passing build, coverage result or demonstration without evidence.

Keep README commands, assumptions, API examples and demo output consistent with
the implementation. Explain algorithmic limits honestly. Automated tests are the
primary demonstration; console demos provide a short visual walkthrough.

## Commit conventions

When committing is requested, use separate logical commits, not one large
unrelated change. Keep implementation and its necessary regression coverage
together when separating them would leave a broken intermediate revision.

Allowed prefixes only:
`feat`, `fix`, `style`, `refactor`, `build`, `perf`, `docs`, `test`, `ci`,
`chore`, `revert`.

Use `<prefix>: <meaningful summary>` and a body explaining the change and its
reason. Keep history incremental; do not rewrite or squash it without approval.
Stage only intended files. Push only when requested, and report any failure
plainly rather than claiming completion.
