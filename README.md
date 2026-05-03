# json-logic-java

This parser accepts [JsonLogic](http://jsonlogic.com) rules and executes them in Java without Nashorn.

The JsonLogic format is designed to allow you to share rules (logic) between front-end and back-end code (regardless of language difference), even to store logic along with a record in a database.
JsonLogic is documented extensively at [JsonLogic.com](http://jsonlogic.com), including examples of every [supported operation](http://jsonlogic.com/operations.html) and a place to [try out rules in your browser](http://jsonlogic.com/play.html).

## Installation

```xml
<dependency>
  <groupId>io.github.gzsombor</groupId>
  <artifactId>json-logic-java</artifactId>
  <version>1.1.3</version>
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

### Results

Throughput on an Intel i9-12950HX, JDK 17, 3 forks × 5 s warmup + 5 s measurement.
Higher is better (ops/s = rule evaluations per second).

| Scenario | Interpreter (ops/s) | Compiled (ops/s) | Speedup |
|---|--:|--:|--:|
| Dispatch table miss | 764,520 | 17,012,825 | **22.2×** |
| Dispatch table hit | 1,856,461 | 22,125,525 | **11.9×** |
| In-set check, hit | 2,142,007 | 20,220,420 | **9.4×** |
| In-set check, miss | 1,950,122 | 20,755,424 | **10.6×** |
| Repeated var lookup (same key used 5×) | 1,371,671 | 12,990,380 | **9.5×** |
| Two string comparisons | 2,873,332 | 13,030,453 | **4.5×** |
| Three string comparisons | 1,780,765 | 9,084,658 | **5.1×** |
| Four string comparisons | 1,696,041 | 6,894,653 | **4.1×** |
| Five mixed operations | 1,294,813 | 5,551,687 | **4.3×** |
| Twenty-clause AND chain | 290,905 | 1,210,222 | **4.2×** |

Key observations:

- **Dispatch overhead is eliminated.** The interpreter pays a map lookup + virtual dispatch on every operator; the compiler emits a direct Java call. For rules that consist of a single cached lookup, the compiled path is up to ~22× faster.
- **`in` against a literal set is now compiled.** The haystack is emitted as a `private static final HashSet<Object>` field, allocated once at class-load time; each evaluation is a single `HashSet.contains` call. This lifts `in`-set throughput from ~2M to ~20M ops/s (~10×).
- **Repeated variable access scales well.** The compiler hoists repeated `{"var":"x"}` lookups into `final` locals, reducing five map lookups to one. That alone accounts for the ~9.5× gain on the repeated-lookup benchmark vs ~4–5× for single-use vars.
- **Complex rules still benefit.** Even a twenty-clause AND chain — the worst case for compilation overhead — sees a ~4× improvement, because every intermediate truthiness check and var resolution is a direct primitive operation rather than a virtual dispatch through the evaluator tree.

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
