# json-logic-java

This parser accepts [JsonLogic](http://jsonlogic.com) rules and executes them in Java without Nashorn.

The JsonLogic format is designed to allow you to share rules (logic) between front-end and back-end code (regardless of language difference), even to store logic along with a record in a database.
JsonLogic is documented extensively at [JsonLogic.com](http://jsonlogic.com), including examples of every [supported operation](http://jsonlogic.com/operations.html) and a place to [try out rules in your browser](http://jsonlogic.com/play.html).

## Installation

```xml
<dependency>
  <groupId>io.github.gzsombor</groupId>
  <artifactId>json-logic-java</artifactId>
  <version>1.1.5</version>
</dependency>
```

## Performance

By default, the forked `JsonLogic` compiles each unique rule into a native Java method at first use via `javax.tools`, then caches and reuses it - delivering a **3–26× throughput improvement** over the tree-walking interpreter for fully-compiled rules (see [Benchmarks](#benchmarks) below). Rules that contain operators not yet supported by the compiler fall back to the interpreter for that sub-expression, so compilation adds no benefit in those cases. If no compiler is available a warning is logged and the interpreter is used as a fallback. To opt out of compilation entirely, pass `false` to the constructor:

```java
JsonLogic jsonLogic = new JsonLogic(false);
```

## Benchmarks

JMH benchmarks comparing interpreter vs compiled throughput are in `src/jmh`. Run them with:

```bash
gradle jmh
```

To run a specific benchmark, pass its name (or a substring) via `jmhArgs`:

```bash
gradle jmh -PjmhArgs="TwentyClauses"
```

To measure compilation cost and break-even iteration counts, run:

```bash
gradle jmh -PjmhArgs="CompilationBreakEven"
```

### Results

Throughput on an Intel i9-12950HX, JDK 11.0.31, 1 fork x 3 x 2 s warmup + 3 x 2 s measurement.
Higher is better (ops/s = rule evaluations per second).

| Scenario | Interpreter (ops/s) | Compiled (ops/s) | Speedup |
|---|--:|--:|--:|
| Arithmetic | 1,514,153 | 9,564,291 | **6.3x** |
| String concatenation | 1,633,513 | 5,197,715 | **3.2x** |
| Dispatch table hit | 1,181,843 | 16,661,645 | **14.1x** |
| Dispatch table miss | 865,786 | 16,103,533 | **18.6x** |
| Dynamic `in` check, hit | 2,542,062 | 5,170,363 | **2.0x** |
| Five mixed operations | 1,002,965 | 4,379,188 | **4.4x** |
| FizzBuzz conditional chain | 343,359 | 4,663,748 | **13.6x** |
| Four string comparisons | 1,259,449 | 5,010,306 | **4.0x** |
| In-set check, hit | 1,229,081 | 14,393,627 | **11.7x** |
| In-set check, miss | 1,281,681 | 15,483,167 | **12.1x** |
| Map double | 806,350 | 10,531,734 | **13.1x** |
| Reduce count | 555,280 | 14,400,722 | **25.9x** |
| Reduce sum | 563,396 | 14,186,725 | **25.2x** |
| Repeated var lookup (same key used 3x) | 1,073,593 | 16,317,285 | **15.2x** |
| Substring | 3,704,492 | 16,411,099 | **4.4x** |
| Three string comparisons | 1,810,214 | 8,246,147 | **4.6x** |
| Twenty-clause AND chain | 300,949 | 1,303,189 | **4.3x** |
| Two string comparisons | 2,537,095 | 10,979,852 | **4.3x** |

Key observations:

- **Dispatch overhead is eliminated.** The interpreter pays a map lookup + virtual dispatch on every operator; the compiler emits direct Java code. Dispatch-table rules are ~14-19x faster when compiled.
- **`in` against a literal set is compiled.** The haystack is emitted as a `private static final HashSet<Object>` field, allocated once at class-load time; each evaluation is a single `HashSet.contains` call. Literal `in` checks improve by ~12x.
- **Repeated variable access and reductions scale well.** The compiler hoists repeated `{"var":"x"}` lookups into `final` locals and emits tight loops for supported `reduce` shapes, reaching ~15-26x throughput gains in these benchmarks.
- **Collection transforms are fully compiled.** `map` now compiles to a tight loop with direct element access, reaching ~13x improvement over the interpreter.
- **Complex rules still benefit.** Even a twenty-clause AND chain sees a ~4x improvement, because every intermediate truthiness check and var resolution is direct code rather than virtual dispatch through the evaluator tree.

### Compilation break-even

Compilation is a one-time cost paid on first use of a unique rule, then the compiled function is cached. The break-even estimate is `compile time / (interpreter eval time - compiled eval time)`.

Measured with JMH average-time mode on JDK 11.0.31:

| Scenario | Compile time | Interpreter eval | Compiled eval | Break-even |
|---|--:|--:|--:|--:|
| Five mixed operations | 11,684 us | 0.884 us | 0.206 us | ~17,200 evals |
| Twenty-clause AND chain | 12,324 us | 3.217 us | 0.911 us | ~5,300 evals |
| Dispatch table miss | 11,573 us | 0.973 us | 0.070 us | ~12,800 evals |

As a rule of thumb, compilation pays off for rules evaluated thousands to tens of thousands of times. For one-off or rarely executed rules, use `new JsonLogic(false)` to avoid the cold compilation cost.

## Comparison operator compatibility

The numeric comparison operators (`>`, `>=`, `<`, `<=`) follow the official JavaScript JsonLogic implementation more closely for type coercion. In particular, `null` is coerced to `0` for comparisons, and booleans are coerced to `1` or `0`.

Examples:

| Rule | Result |
|---|---:|
| `{">": [1, null]}` | `true` |
| `{">": [null, 1]}` | `false` |
| `{">=": [null, null]}` | `true` |
| `{">": [true, false]}` | `true` |

This is a behavioural change from older json-logic-java releases that treated `null` as non-numeric and returned `false` for comparisons such as `{">": [1, null]}`.

## Examples

The public API for json-logic-java attempts to mimic the public API of the original Javascript implementation as close as possible.
For this reason, the API is loosely typed in many places.
This implementation relies on duck-typing for maps/dictionaries and arrays: if it looks and feels like an array, we treat it like an array.

```java
// Create a new JsonLogic instance. JsonLogic is thread safe.
JsonLogic jsonLogic = new JsonLogic();

// Set up some JSON and some data.
String expression = "{\"*\": [{\"var\": \"x\"}, 2]}";
Map<String, Integer> data = new HashMap<>();
data.put("x", 10);

// Evaluate the result.
double result = (double) jsonLogic.apply(expression, data);
assert result == 20.0;
```

You can add your own operations like so:

```java
// Register an operation.
jsonLogic.addOperation("greet", (args) -> "Hello, " + args[0] + "!");

// Evaluate the result.
String result = (String) jsonLogic.apply("{\"greet\": [\"Sam\"]}", null);
assert "Hello, Sam!".equals(result);
```

There is a `truthy` static method that mimics the truthy-ness rules of Javascript:

```java
assert JsonLogic.truthy(0) == false;
assert JsonLogic.truthy(1) == true;
assert JsonLogic.truthy("") == false;
assert JsonLogic.truthy("Hello world!") == true;

// etc...
```
