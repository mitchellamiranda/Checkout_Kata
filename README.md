# Fluro Checkout

A Java 21 checkout and pricing engine for the Fluro take-home exercise. Items may
be scanned in any order. Each transaction receives a snapshot of its catalogue
and promotions; the engine finds the lowest total without allocating an item to
more than one promotion.

Spring Boot 3 provides configuration and dependency injection only. There is no
database, REST API, UI, web server or external service.

## Exercise rules

All amounts are integer pence.

| SKU | Unit price | Promotion |
| --- | ---: | --- |
| A | 50 | None |
| B | 75 | Two for 125 |
| C | 25 | Buy three, get a fourth free |
| D | 150 | One D and one E for 300 |
| E | 200 | Participates in the same D + E deal |

Promotions repeat for every complete eligible group; remaining items keep their
unit prices. Three C cost 75, four C also cost 75, and eight C cost 150. Free
items must actually be scanned. No item is added implicitly.

For example, `BAB` costs 175, and `ABBCCCCDE` costs 550. Offers that would
increase the total are ignored. Overlapping offers compete for scanned items;
they never stack on the same item.

## Running

Requires **JDK 21** (`JAVA_HOME` pointing to it) and Maven 3.6.3 or newer.
The included wrapper downloads Maven 3.9.9 if Maven is not installed.
Only the initial dependency download requires network access.

```shell
mvn clean test
mvn verify
```

Windows PowerShell, without a system Maven installation:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd verify
```

On macOS/Linux, use `sh mvnw clean test` and `sh mvnw verify`.

`verify` runs the tests, packages the application, generates the JaCoCo report
at `target/site/jacoco/index.html`, and enforces at least **95% line coverage**
and **90% branch coverage**, with no production-code coverage exclusions.
Java compiler warnings fail the build.

The packaged Spring application can also be started:

```powershell
java -jar target\checkout-1.0.0-SNAPSHOT.jar
```

It initializes configuration and exits normally: there is deliberately no
interactive CLI or server. The tests and Java API are the exercise's entry points.

## Example usage

The application API works without starting Spring:

```java
import com.mitchell.fluro.checkout.application.checkout.Checkout;
import com.mitchell.fluro.checkout.domain.model.Item;
import com.mitchell.fluro.checkout.domain.model.Money;
import com.mitchell.fluro.checkout.domain.pricing.PricingRules;
import com.mitchell.fluro.checkout.domain.promotion.BuyNGetOneFreePromotion;
import com.mitchell.fluro.checkout.domain.promotion.MealDealPromotion;
import com.mitchell.fluro.checkout.domain.promotion.MultiPricePromotion;

import java.util.List;
import java.util.Map;

var pricingRules = new PricingRules(
        List.of(
                new Item("A", Money.ofPence(50)),
                new Item("B", Money.ofPence(75)),
                new Item("C", Money.ofPence(25)),
                new Item("D", Money.ofPence(150)),
                new Item("E", Money.ofPence(200))),
        List.of(
                new MultiPricePromotion("B", 2, Money.ofPence(125)),
                new BuyNGetOneFreePromotion("C", 3),
                new MealDealPromotion(Map.of("D", 1, "E", 1), Money.ofPence(300))));

var checkout = new Checkout(pricingRules);
checkout.scan("B");
checkout.scan("A");
checkout.scan("B");

Money total = checkout.getTotal(); // Money[pence=175]
long pence = total.pence();        // 175
```

In a Spring application, inject `CheckoutFactory` and call `create()` for each
transaction. The factory shares immutable rules and a stateless pricing engine,
**not** a mutable checkout. Calling `getTotal()` does not consume promotions or
change the basket; scanning may continue afterwards.

### Changing prices

The supplied price list is data in `src/main/resources/application.yml`, not
constants inside checkout or promotion algorithms. Spring's external
configuration can override it without recompiling the engine, for example by
supplying an external `application.yml` when starting the packaged application.

The configuration has a `checkout.unit-prices` map and three typed promotion
lists: `multi-prices`, `buy-n-get-one-free`, and `meal-deals`. Amounts are pence;
quantities are positive integers. Use an explicit empty list (`[]`) to disable
a promotion family. All four configuration sections are required; missing deal
prices are rejected rather than interpreted as zero.

For the plain Java API, construct another `PricingRules` instance and pass it
to a new `Checkout`. The one-argument `PricingRules(items)` constructor enables
unit pricing only. Existing transactions retain their original rules.
Configuration is loaded at startup; there is no background refresh mechanism.

## Architecture

All packages are beneath `com.mitchell.fluro.checkout`.

| Layer | Responsibility |
| --- | --- |
| `domain.model` | Immutable `Money`, `Item` and `Basket` value objects and invariants |
| `domain.pricing` | Catalogue snapshot (`PricingRules`) and atomic bundle exchange (`PricingRule`) |
| `domain.promotion` | `Promotion` strategies and immutable candidate results |
| `domain.service` | Unit valuation, offer coordination and exact allocation |
| `application.checkout` | Stateful transaction orchestration and transaction factory |
| `infrastructure` | Spring binding, configuration validation and dependency composition |

Tests mirror these packages under `src/test/java`. Domain and application
classes have no Spring dependencies or annotations. There are no repository
interfaces or adapters for infrastructure that the exercise does not need.

### Domain design

`Money` uses checked `long` arithmetic in pence, avoiding floating-point
rounding. Negative values, negative results and arithmetic overflow fail
explicitly; there is no saturation or wraparound. Totals must fit in a signed
`long`, including the undiscounted basket valuation.

`Basket` stores quantities rather than one object per scan. Its contents and
exposed snapshots are immutable. A SKU cannot have conflicting prices within a
basket. Counts must be positive and fit in an `int`; absence represents zero.
SKUs are case-sensitive, nonblank strings without surrounding whitespace, so
multi-character SKUs are supported too. Unknown or invalid scans throw before
changing the transaction.

`Checkout` is intentionally mutable and **not thread-safe**: it belongs to one
transaction and one owning thread. Its dependencies are immutable or stateless.
Collections are defensively copied; custom promotion implementations must also
honour the immutability contract.

### Promotion engine: strategies, not condition chains

```java
public interface Promotion {
    PromotionResult apply(Basket basket);
}
```

Each strategy describes eligible alternatives for the **original** basket.
`PromotionResult` contains zero or more `PricingRule` candidates. A candidate
specifies the SKU quantities consumed by **one** application and its total
price. It is repeatable while sufficient unallocated items remain.

`MultiPricePromotion`, `BuyNGetOneFreePromotion` and `MealDealPromotion` express
their own offer semantics. They do not allocate items, mutate baskets or decide
which competing offer wins. The buy-N strategy derives its price from the
transaction's actual unit price. Meal deals support more than two SKUs and
different quantities per SKU.

The Strategy pattern isolates these variations. The pricing engine depends
only on `Promotion` and bundle values; it has no promotion-type switches or
`instanceof` dispatch.

### Pricing engine: why not greedy?

Taking the largest discount first is not generally optimal. At 100p per A,
with four for 250 and three for 200, six A should cost 400. Taking the largest
single saving first would instead charge 450.

The engine:

1. Values the basket at unit prices and gathers all candidate bundles.
2. Removes identical candidates and ignores ineligible or non-saving offers.
3. Groups offers by shared SKUs, including transitive overlap. Disjoint groups cannot compete for an item.
4. Solves a one-offer group directly using the maximum complete bundle count.
5. Uses memoized exact search for overlapping groups, then subtracts the best total savings.

For remaining quantities `q`, the recurrence is:

```text
bestSavings(q) = max(
    0,
    saving(offer) + bestSavings(q - offer.quantities)
    for every offer that fits
)
```

Every offer consumes a positive quantity, so the state graph is acyclic.
An explicit post-order stack avoids recursive call-stack overflow. Zero saving
is always available, retaining unit pricing for leftovers. Exploring every
feasible next exchange proves optimality for the supported bundle model.

For a connected group with `k` SKUs, `m` offers and `S` reachable quantity
states, search takes approximately `O(S * m * k)` time and `O(S * k)` memo
storage. The pending work stack can additionally hold `O(m * depth * k)` data.
Worst-case
`S` is bounded by `product(quantity_i + 1)`: many overlapping SKUs can still be
expensive. The algorithm does **not** claim polynomial scaling for arbitrary
promotion graphs. Group construction is quadratic in the number of offers in
the worst case; single-offer components require no state enumeration.
The supplied price list consists entirely of independent one-offer components.

## Extending the solution

Add a new immutable `Promotion` implementation and register an instance in
the `PricingRules` promotion list. Existing promotion classes and the allocation
engine remain unchanged: this is the Open/Closed boundary. The composition
root is deliberately allowed to change when registering a new strategy.

For example, a buy-two-get-two-free strategy can emit a bundle consuming four
of its configured SKU for twice that item's transaction unit price. A strategy
with several eligible combinations must return **all** alternatives, not
greedily select one. Returning `PromotionResult.NONE` means no eligible offer.
`PromotionResult.eligible(rule, basket)` handles the common single-bundle case.

Strategies must be deterministic, side-effect-free, and return repeatable
fixed-price exchanges involving items in the basket. Failures propagate rather
than silently falling back to unit pricing. A new strategy that needs external
configuration also needs a typed configuration entry and registration in
`CheckoutConfiguration`; no existing strategy logic changes.

One-time coupons, order-sensitive rewards and discounts that stack on already
discounted items are deliberately **outside** this exchange model. Supporting
them would require explicit additional state or a different allocation contract,
not a misleading implementation of the current interface.

## Testing approach

JUnit 5 and AssertJ tests cover unit prices, promotion boundaries, repetitions,
remainders, invalid inputs, immutability, overflow and transaction isolation.
Nested suites and parameterized scenarios keep the business cases readable.

Interaction tests include greedy counterexamples, competing meal/free-item
offers, duplicate rules, free bundles and scan-order permutations.
An independent exhaustive reference implementation enumerates offer counts for
250 deterministically generated baskets and checks both registration orders.
It does not reuse production eligibility or optimization helpers.

Large-basket cases include five million items with independent promotions,
20,000 items with overlapping offers, and quantities at `Integer.MAX_VALUE`.
Spring tests exercise real YAML binding, the supplied rules, startup and
factory wiring; most tests remain plain, fast unit tests. Mockito is excluded
because no mocks are needed.

## Future improvements

Add an itemized pricing breakdown with chosen offers and savings for receipts
and auditability. Version rule snapshots and effective dates if live price
updates become a requirement. For substantially larger connected promotion
graphs, benchmark representative data before adding dominance pruning,
branch-and-bound or a dedicated optimization solver; any operational search
limit should fail explicitly rather than return an unproven "best" price.

Currency, tax and fractional-pence rounding policies should be introduced only
with explicit business rules. No speculative persistence or network layer is
included.

## Development and submission

Git history records incremental domain, checkout, strategy, optimization,
configuration, test and documentation changes. The private personal repository
is hosted at https://github.com/pt3cmimi/Checkout_Kata. Reviewers need explicit
repository access. The supplied PDF and IDE metadata remain local and are
excluded from version control.

AI assistance was used for implementation, test generation and documentation.
The key interview discussion points are the repeatable-bundle contract, exact
versus greedy allocation, independent oracle tests, and the optimizer's
scalability limits.
