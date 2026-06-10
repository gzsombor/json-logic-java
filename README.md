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

By default, the forked `JsonLogic` compiles each unique rule into a native Java method at first use via `javax.tools`, then caches and reuses it - delivering a **4–22× throughput improvement** over the tree-walking interpreter for fully-compiled rules (see [Benchmarks](#benchmarks) below). Rules that contain operators not yet supported by the compiler fall back to the interpreter for that sub-expression, so compilation adds no benefit in those cases. If no compiler is available a warning is logged and the interpreter is used as a fallback. To opt out of compilation entirely, pass `false` to the constructor:

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

Throughput on an Intel i9-12950HX, JDK 17.0.14, 1 fork x 3 x 2 s warmup + 3 x 2 s measurement.
Higher is better (ops/s = rule evaluations per second).

| Scenario | Interpreter (ops/s) | Compiled (ops/s) | Speedup |
|---|--:|--:|--:|
| Arithmetic | 1,391,811 | 7,738,979 | **5.6x** |
| String concatenation | 1,403,041 | 4,553,208 | **3.2x** |
| Dispatch table hit | 1,113,313 | 12,604,718 | **11.3x** |
| Dispatch table miss | 720,690 | 12,279,292 | **17.0x** |
| Dynamic `in` check, hit | 2,411,212 | 4,200,999 | **1.7x** |
| Five mixed operations | 776,904 | 3,252,051 | **4.2x** |
| FizzBuzz conditional chain | 275,689 | 3,909,191 | **14.2x** |
| Four string comparisons | 972,814 | 3,697,499 | **3.8x** |
| In-set check, hit | 1,271,430 | 12,312,758 | **9.7x** |
| In-set check, miss | 1,116,950 | 11,948,308 | **10.7x** |
| Map double | 584,876 | 577,456 | **1.0x** |
| Reduce count | 416,772 | 11,922,612 | **28.6x** |
| Reduce sum | 460,071 | 11,654,549 | **25.3x** |
| Repeated var lookup (same key used 3x) | 801,503 | 7,521,359 | **9.4x** |
| Substring | 2,639,158 | 12,139,819 | **4.6x** |
| Three string comparisons | 1,287,003 | 5,129,518 | **4.0x** |
| Twenty-clause AND chain | 213,580 | 687,116 | **3.2x** |
| Two string comparisons | 1,648,420 | 7,362,681 | **4.5x** |

Key observations:

- **Dispatch overhead is eliminated.** The interpreter pays a map lookup + virtual dispatch on every operator; the compiler emits direct Java code. Dispatch-table rules are ~11-17x faster when compiled.
- **`in` against a literal set is compiled.** The haystack is emitted as a `private static final HashSet<Object>` field, allocated once at class-load time; each evaluation is a single `HashSet.contains` call. Literal `in` checks improve by ~10x.
- **Repeated variable access and reductions scale well.** The compiler hoists repeated `{"var":"x"}` lookups into `final` locals and emits tight loops for supported `reduce` shapes, reaching ~9-29x throughput gains in these benchmarks.
- **Complex rules still benefit.** Even a twenty-clause AND chain sees a ~3x improvement, because every intermediate truthiness check and var resolution is direct code rather than virtual dispatch through the evaluator tree.
- **Fallback-heavy rules may not improve.** `map` currently shows no compiled-path benefit in this benchmark because the compiled rule falls back to interpreter behavior for the transform-heavy expression.

### Compilation break-even

Compilation is a one-time cost paid on first use of a unique rule, then the compiled function is cached. The break-even estimate is `compile time / (interpreter eval time - compiled eval time)`.

Measured with JMH average-time mode on JDK 17.0.14:

| Scenario | Compile time | Interpreter eval | Compiled eval | Break-even |
|---|--:|--:|--:|--:|
| Five mixed operations | 14,942 us | 1.400 us | 0.384 us | ~14,700 evals |
| Twenty-clause AND chain | 17,085 us | 5.099 us | 1.528 us | ~4,800 evals |
| Dispatch table miss | 16,660 us | 1.550 us | 0.124 us | ~11,700 evals |

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
