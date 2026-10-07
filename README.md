# Fluro Checkout

A Java 21 checkout library with configurable prices and promotions. Items can be
scanned in any order; the engine chooses the lowest total without allocating an
item to two promotions. Spring Boot 3 and XML provide dependency injection at the
application boundary. Domain and application classes work without Spring.

No database, REST endpoints, frontend, interactive menu or external services.

## Build and demonstrate

Requires **JDK 21** (`JAVA_HOME` set) and Maven 3.6.3+. The included wrapper
downloads Maven 3.9.9 when needed.

```shell
mvn clean test
mvn verify
```

Windows PowerShell without an installed Maven:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd verify
```

On macOS/Linux, use `sh mvnw clean test` and `sh mvnw verify`.

**Automated tests are the primary demonstration.** They cover individual prices,
every promotion, repetitions, remainders, mixed baskets, scan-order permutations,
overlapping offers, invalid inputs and transaction isolation. An independent
exhaustive oracle covers 250 deterministic baskets in both promotion orders.
Large-basket scenarios include five million items and deeply overlapping offers.

`verify` also packages the application and writes coverage to
`target/site/jacoco/index.html`. JaCoCo enforces 95% line and 90% branch coverage,
without production exclusions. Compiler warnings fail the build.

### Optional console demo

After `verify`, run:

```powershell
java -jar target\checkout-1.0.0-SNAPSHOT.jar --spring.profiles.active=demo --spring.main.banner-mode=off --logging.level.root=ERROR
```

The bundled rules produce:

```text
B A B -> 175p
B B A -> 175p
A B B C C C C D E -> 550p
A A B B B B C C C C C C C C D D E E -> 1100p
```

Each line uses a new checkout. Prices come from the XML-configured engine, not
hardcoded results. `DemoContext.xml` enables the runner only under the `demo`
profile. Without that profile the application initializes and exits without a
checkout demonstration. The demo requires SKUs A-E; it is not an interactive CLI.

## Rules and assumptions

All amounts are integer pence.

| SKU | Unit price | Promotion |
| --- | ---: | --- |
| A | 50 | None |
| B | 75 | Two for 125 |
| C | 25 | Buy three, get a fourth free |
| D | 150 | One D and one E for 300 |
| E | 200 | Same D + E deal |

Promotions repeat for complete bundles; leftovers retain unit prices. Free items
must be scanned: three C cost 75p, four also cost 75p, and eight cost 150p.
More expensive offers are ignored. An item cannot participate in two offers.

SKUs are case-sensitive, nonblank strings without surrounding whitespace.
Invalid scans fail without changing the basket. `Money` uses checked `long`
arithmetic; negative amounts and overflow fail explicitly. Quantities fit in
an `int`, and the undiscounted total must fit in a `long`.

A checkout is thread-confined. Its basket snapshots and pricing collections are
immutable; custom promotions must also be immutable. Repeated `getTotal()` calls
do not consume offers, and scanning can continue afterwards.

## Supplying different pricing rules

Pass a rules snapshot to each transaction. Plain Java usage:

```java
import com.mitchell.fluro.checkout.application.checkout.Checkout;
import com.mitchell.fluro.checkout.application.checkout.ICheckout;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;
import com.mitchell.fluro.checkout.domain.service.IPricingEngine;
import com.mitchell.fluro.checkout.domain.service.PricingEngine;
import com.mitchell.fluro.checkout.domain.service.PromotionOptimizer;

import java.util.List;

var rules = new PricingRules(
        List.of(new Item("A", Money.ofPence(50)), new Item("B", Money.ofPence(75))),
        List.of(new MultiPricePromotion("B", 2, Money.ofPence(125))));
IPricingEngine engine = new PricingEngine(new PromotionOptimizer());
ICheckout checkout = new Checkout(rules, engine);
checkout.scan("B");
checkout.scan("A");
checkout.scan("B");
Money total = checkout.getTotal(); // 175p

var revisedRules = new PricingRules(List.of(new Item("A", Money.ofPence(60))));
ICheckout nextTransaction = new Checkout(revisedRules, engine);
```

`PricingRules(items)` enables unit pricing only. Supplying new rules does not
change an existing transaction. The Spring `ICheckoutFactory` uses its injected
snapshot for every `create()` call; it does not refresh prices automatically.

### XML dependency injection

| Resource in `src/main/resources` | Purpose |
| --- | --- |
| `ApplicationContext.xml` | Imports pricing, services and optional demo |
| `PricingRulesContext.xml` | Items, prices, catalogue and promotion beans |
| `CheckoutServicesContext.xml` | Optimizer, pricing engine and checkout factory |
| `DemoContext.xml` | Console runner, enabled by the `demo` profile |

`CheckoutConfiguration` only imports the root. XML constructor references keep
dependencies required and final; no Java `@Bean` factories or setter injection
are needed. The factory is shared, but each checkout has its own basket.

Change money values or promotion constructor arguments in the pricing XML.
Multiple beans of the same promotion type are supported: give each a unique ID.
The `pricingRules` bean constructor-autowires all `Item` and `IPromotion` beans.
The separate `catalogue` validates SKU references in the supplied promotion
expressions; add new items to its explicit list too.

To replace the root without recompiling:

```powershell
java -jar target\checkout-1.0.0-SNAPSHOT.jar --checkout.context=file:./config/ApplicationContext.xml
```

The external root must define or import its rules and services. Relative imports
resolve beside that root; `classpath:CheckoutServicesContext.xml` reuses bundled
services. Import `classpath:DemoContext.xml` and enable `demo` to reuse the demo
with an alternative A-E catalogue. Missing resources and invalid configuration
fail startup. XML is trusted application configuration, not untrusted input.

## Architecture and design decisions

Packages below `com.mitchell.fluro.checkout`:

| Package | Responsibility |
| --- | --- |
| `domain.model` | Immutable `Money`, `Item`, `Basket` |
| `domain.pricing` | Rules snapshot and atomic bundle values |
| `domain.promotion` | `IPromotion` strategies and `PromotionResult` |
| `domain.service` | Pricing coordination and exact allocation |
| `application.checkout` | Checkout API and transaction factory |
| `infrastructure` | XML bootstrap and optional console adapter |

**Strategy and Open/Closed:** Each `IPromotion` returns eligible repeatable
bundles, not a final discount allocation. Add an implementation and register its
XML bean without editing existing strategies or the optimizer. Multiple
alternatives must all be returned so the engine can choose globally.

**Dependency inversion:** `Checkout` receives `IPricingEngine`; `PricingEngine`
receives `IPromotionOptimizer`. XML selects concrete implementations. Small
`I*` contracts separate scanning, creation, pricing, allocation and offer
generation. Immutable data values stay concrete rather than gaining unnecessary
interfaces. Replacement services must preserve non-mutation and optimal-pricing
contracts, not merely implement matching signatures.

**Exact allocation, not greedy:** At 100p per A, with four for 250 and three for
200, six A should cost 400, not 450. The engine deduplicates offers, discards
non-saving candidates, and separates independent SKU groups. Single-offer
groups are solved arithmetically; overlapping groups use memoized exact search:

```text
bestSavings(remaining) = max(
    0,
    offerSaving + bestSavings(remaining - offerQuantities)
    for each offer that fits
)
```

Every offer consumes items, so the search terminates. An explicit stack avoids
recursive stack overflow. For `S` reachable states, `m` offers and `k` SKUs in
one group, search is approximately `O(S * m * k)`; worst-case state count is
`product(quantity_i + 1)`. Large overlapping graphs can still be expensive.
One-time coupons and discounts stacking on already allocated items are outside
the current repeatable-bundle contract.

## Testing conventions and interview walkthrough

JUnit 5 and AssertJ use nested suites and named-map `@MethodSource` cases.
The test-only `TestCaseBuilder` snapshots `dataValues` and `expectedCalls`,
then resets defaults. Mutable fixtures are fresh per case. Names follow
`<method>Test` and `<method>DataProvider`; no JUnit 4 or Mockito is required.
Spring integration exercises actual XML imports, profile wiring, constructor
references and startup failures.

For an interview: run the tests, show the console output, trace
`Checkout -> PricingEngine -> PromotionOptimizer`, then explain a competing-offer
case and the independent oracle. AI assistance was used for implementation,
test generation and documentation; Git history records the incremental work.

## Future improvements

Add itemized allocation/savings for receipts and rule-version/effective-date
support if needed. Benchmark larger connected promotion graphs before adding
pruning or a solver; never silently substitute a heuristic total. Introduce
currency, tax and rounding only with explicit business rules.
