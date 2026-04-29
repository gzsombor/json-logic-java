# json-logic-java

This parser accepts [JsonLogic](http://jsonlogic.com) rules and executes them in Java without Nashorn.

The JsonLogic format is designed to allow you to share rules (logic) between front-end and back-end code (regardless of language difference), even to store logic along with a record in a database.
JsonLogic is documented extensively at [JsonLogic.com](http://jsonlogic.com), including examples of every [supported operation](http://jsonlogic.com/operations.html) and a place to [try out rules in your browser](http://jsonlogic.com/play.html).

## Installation

```xml
<dependency>
  <groupId>io.github.gzsombor</groupId>
  <artifactId>json-logic-java</artifactId>
  <version>1.1.2</version>
</dependency>
```

## Performance

By default, the forked `JsonLogic` compiles each unique rule into a native Java method at first use via `javax.tools`, then caches and reuses it - delivering a **3–18× throughput improvement** over the tree-walking interpreter depending on rule complexity (see [Benchmarks](#benchmarks) below). If no compiler is available a warning is logged and the interpreter is used as a fallback. To opt out of compilation entirely, pass `false` to the constructor:

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
| Dispatch table hit (cached rule, no data) | 1,633,768 | 19,694,163 | **12.1×** |
| Dispatch table miss (uncached rule) | 1,136,814 | 20,649,754 | **18.2×** |
| Two string comparisons | 2,859,606 | 11,506,996 | **4.0×** |
| Repeated var lookup (same key used 5×) | 1,334,866 | 12,962,381 | **9.7×** |
| Three string comparisons | 2,253,688 | 8,968,601 | **4.0×** |
| Four string comparisons | 1,595,199 | 6,733,857 | **4.2×** |
| Five mixed operations | 1,338,222 | 5,324,886 | **4.0×** |
| Twenty-clause AND chain | 362,342 | 1,084,053 | **3.0×** |

Key observations:

- **Dispatch overhead is eliminated.** The interpreter pays a map lookup + virtual dispatch on every operator; the compiler emits a direct Java call. For rules that consist of a single cached lookup, the compiled path is ~12–18× faster.
- **Repeated variable access scales well.** The compiler hoists repeated `{"var":"x"}` lookups into `final` locals, reducing five map lookups to one. That alone accounts for the ~9.7× gain on the repeated-lookup benchmark vs ~4× for single-use vars.
- **Complex rules still benefit.** Even a twenty-clause AND chain — the worst case for compilation overhead — sees a 3× improvement, because every intermediate truthiness check and var resolution is a direct primitive operation rather than a virtual dispatch through the evaluator tree.

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
