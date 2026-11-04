# json-logic-java

This parser accepts [JsonLogic](http://jsonlogic.com) rules and executes them in Java without Nashorn.

The JsonLogic format is designed to allow you to share rules (logic) between front-end and back-end code (regardless of language difference), even to store logic along with a record in a database.
JsonLogic is documented extensively at [JsonLogic.com](http://jsonlogic.com), including examples of every [supported operation](http://jsonlogic.com/operations.html) and a place to [try out rules in your browser](http://jsonlogic.com/play.html).

## Installation

```xml
<dependency>
  <groupId>io.github.gzsombor</groupId>
  <artifactId>json-logic-java</artifactId>
  <version>1.2.0</version>
</dependency>
```

## Performance

By default, the forked `JsonLogic` compiles each unique rule into a native Java method at first use (via `javax.tools` with the `javac` compiler, or directly as bytecode with the JDK 24 `jep484` compiler), then caches and reuses it - delivering roughly a **2–33× throughput improvement** over the tree-walking interpreter for fully-compiled rules (see [Benchmarks](#benchmarks) below). Rules that contain operators not yet supported by the compiler fall back to the interpreter for that sub-expression, so compilation adds no benefit in those cases. If no compiler is available a warning is logged and the interpreter is used as a fallback. To opt out of compilation entirely, pass `false` to the constructor:

```java
JsonLogic jsonLogic = new JsonLogic(false);
```

## Benchmarks

JMH benchmarks are in `src/jmh`. They compare three execution engines:

| Engine | Module | Description |
|---|---|---|
| `interpreter` | `json-logic-core` | Tree-walking evaluator (`new JsonLogic(false)`) |
| `javac` | `json-logic-java-compiler-javac` | Generates Java source and compiles it with `javax.tools` (runs on JDK 11+) |
| `jep484` | `json-logic-jep484` | Generates bytecode directly with the JDK 24 ClassFile API ([JEP 484](https://openjdk.org/jeps/484)) |

### Prerequisites

- JDK 11 (the default toolchain for the modules) **and** JDK 24 (needed to compile the `jep484` module and the benchmarks, and to run them). Gradle toolchains locate them automatically; if they are installed in a non-standard location, point Gradle at them in a local `gradle.properties`:

  ```properties
  org.gradle.java.installations.paths=/path/to/jdk-11,/path/to/jdk-24
  ```

- A full JDK (not just a JRE) is required for the `javac` engine, since it needs `javax.tools.JavaCompiler` at runtime.

### Running the benchmarks

Each benchmark has an `engine` parameter (`interpreter`, `javac`, `jep484`), so a plain run measures all three engines side by side:

```bash
gradle jmh
```

To run a specific benchmark, pass its name (or a regex/substring) via `jmhArgs`:

```bash
gradle jmh -PjmhArgs="JmhJsonLogicBenchmark"
```

To compare only some engines, restrict the `engine` parameter with JMH's `-p` option:

```bash
# interpreter vs. javac only
gradle jmh -PjmhArgs="JmhJsonLogicBenchmark -p engine=interpreter,javac"

# only the jep484 compiler
gradle jmh -PjmhArgs="JmhJsonLogicBenchmark -p engine=jep484"
```

Any other JMH command-line option can be passed the same way, e.g. a quick (less precise) smoke run with fewer iterations, or JSON output for later comparison:

```bash
gradle jmh -PjmhArgs="JmhJsonLogicBenchmark -wi 1 -i 2 -f 1"
gradle jmh -PjmhArgs="JmhJsonLogicBenchmark -rf json -rff /tmp/results.json"
```

Note that `jmhArgs` is split on whitespace, so it cannot contain quoted arguments with spaces. Run `gradle jmh -PjmhArgs="-h"` to list all JMH options.

For stable numbers, close other CPU-heavy applications and avoid running on a laptop on battery power.

### Compilation cost: `javac` vs. `jep484`

Compiled rules pay a one-time cost on first use. To measure it for both compilers (the interpreter has no compile step), run:

```bash
gradle jmh -PjmhArgs="CompilationBreakEven"
```

This benchmark is parameterized by `ruleShape` (`fiveClauses`, `twentyClauses`, `dispatchMiss`) and `compilerEngine` (`javac`, `jep484`); filter with `-p`, for example `-p compilerEngine=jep484`.

### Reading the results

JMH prints one row per benchmark and `engine` value. The score is throughput in ops/s (higher is better), so the speedup of a compiler over the interpreter is `score(compiler) / score(interpreter)` for the same benchmark method. Compare rows only from the same run on the same machine.

### Results

Throughput on an Intel i9-12950HX, JDK 24.0.2, 1 fork x 3 x 2 s warmup + 3 x 2 s measurement.
Higher is better (ops/s = rule evaluations per second). Speedups are relative to the interpreter.

| Scenario | Interpreter (ops/s) | javac (ops/s) | jep484 (ops/s) | javac speedup | jep484 speedup |
|---|--:|--:|--:|--:|--:|
| Arithmetic | 4,876,320 | 45,828,969 | 39,441,600 | **9.4x** | **8.1x** |
| Dispatch table hit | 4,486,138 | 45,471,401 | 51,152,383 | **10.1x** | **11.4x** |
| Dispatch table miss | 3,308,082 | 42,275,471 | 51,269,094 | **12.8x** | **15.5x** |
| Dynamic `in` check, hit | 5,500,815 | 9,340,658 | 10,712,456 | **1.7x** | **1.9x** |
| Five mixed operations | 1,767,877 | 14,932,931 | 14,704,452 | **8.4x** | **8.3x** |
| FizzBuzz conditional chain | 592,999 | 11,882,401 | 13,682,929 | **20.0x** | **23.1x** |
| Four string comparisons | 2,408,044 | 18,129,474 | 19,689,931 | **7.5x** | **8.2x** |
| In-set check, hit | 4,175,287 | 48,419,134 | 56,585,535 | **11.6x** | **13.6x** |
| In-set check, miss | 4,812,438 | 43,152,809 | 56,239,276 | **9.0x** | **11.7x** |
| Map double | 1,729,361 | 21,439,089 | 24,030,634 | **12.4x** | **13.9x** |
| Reduce count | 1,203,321 | 35,733,459 | 39,687,539 | **29.7x** | **33.0x** |
| Reduce sum | 1,100,784 | 33,372,076 | 36,336,539 | **30.3x** | **33.0x** |
| Repeated var lookup (same key used 3x) | 1,971,797 | 48,116,604 | 51,758,687 | **24.4x** | **26.2x** |
| String concatenation | 4,084,042 | 18,028,481 | 17,171,821 | **4.4x** | **4.2x** |
| Substring | 7,618,476 | 40,359,040 | 43,146,428 | **5.3x** | **5.7x** |
| Three string comparisons | 3,711,999 | 23,884,985 | 34,134,392 | **6.4x** | **9.2x** |
| Twenty-clause AND chain | 542,808 | 2,785,912 | 2,202,235 | **5.1x** | **4.1x** |
| Two string comparisons | 4,818,257 | 38,238,443 | 42,718,284 | **7.9x** | **8.9x** |

Key observations:

- **Both compilers beat the interpreter in every scenario**, by roughly 2x-33x. The interpreter pays a map lookup + virtual dispatch on every operator; the compilers emit direct code.
- **`javac` and `jep484` are on par.** Every scenario, including `map`, `reduce`, dispatch tables and repeated variable lookups, is within about 1.5x between the two compilers, and neither is consistently ahead.
- **Variables are resolved at most once per evaluation.** A `{"var":"x"}` read is cached in a local on first use, so repeated lookups of the same key cost one resolution. The lookup stays lazy, so a variable that is only read in a branch that is not taken is never resolved.
- **`in` against a literal set is compiled** to a single `HashSet.contains` on a set allocated once at class-load time.
- **Complex rules still benefit.** Even a twenty-clause AND chain sees a ~4x improvement.

The numbers vary between runs and machines; use them for relative comparison only.

### Compilation break-even

Compilation is a one-time cost paid on first use of a unique rule, then the compiled function is cached. The break-even estimate is `compile time / (interpreter eval time - compiled eval time)`.

Measured with JMH average-time mode on JDK 24.0.2:

| Scenario | Compiler | Compile time | Interpreter eval | Compiled eval | Break-even |
|---|---|--:|--:|--:|--:|
| Five mixed operations | javac | 15,723 us | 0.523 us | 0.073 us | ~34,933 evals |
| Five mixed operations | jep484 | 137 us | 0.525 us | 0.072 us | ~302 evals |
| Twenty-clause AND chain | javac | 15,471 us | 1.977 us | 0.417 us | ~9,913 evals |
| Twenty-clause AND chain | jep484 | 175 us | 1.963 us | 0.473 us | ~118 evals |
| Dispatch table miss | javac | 14,701 us | 0.297 us | 0.022 us | ~53,530 evals |
| Dispatch table miss | jep484 | 103 us | 0.283 us | 0.055 us | ~451 evals |

The `jep484` compiler generates bytecode directly instead of invoking `javac`, so compiling a rule is ~100x cheaper (about 0.1-0.2 ms vs. 15 ms) and pays off after a few hundred evaluations rather than tens of thousands. With `javac`, compilation pays off for rules evaluated thousands to tens of thousands of times; for one-off or rarely executed rules, use `new JsonLogic(false)` to avoid the cold compilation cost.

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
