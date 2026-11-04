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
| Arithmetic | 3,913,430 | 42,788,100 | 48,508,328 | **10.9x** | **12.4x** |
| Dispatch table hit | 4,169,013 | 48,036,657 | 30,820,665 | **11.5x** | **7.4x** |
| Dispatch table miss | 3,381,616 | 41,433,380 | 18,436,580 | **12.3x** | **5.5x** |
| Dynamic `in` check, hit | 6,462,928 | 9,525,125 | 10,064,912 | **1.5x** | **1.6x** |
| Five mixed operations | 1,735,734 | 10,503,973 | 12,247,333 | **6.1x** | **7.1x** |
| FizzBuzz conditional chain | 550,902 | 10,284,218 | 10,197,730 | **18.7x** | **18.5x** |
| Four string comparisons | 2,358,126 | 17,900,114 | 20,406,405 | **7.6x** | **8.7x** |
| In-set check, hit | 3,791,140 | 43,915,478 | 48,928,095 | **11.6x** | **12.9x** |
| In-set check, miss | 4,224,244 | 43,687,012 | 51,791,699 | **10.3x** | **12.3x** |
| Map double | 1,488,960 | 18,324,717 | 21,291,033 | **12.3x** | **14.3x** |
| Reduce count | 1,042,113 | 31,540,678 | 36,465,970 | **30.3x** | **35.0x** |
| Reduce sum | 898,404 | 27,102,752 | 32,173,942 | **30.2x** | **35.8x** |
| Repeated var lookup (same key used 3x) | 1,714,567 | 42,091,960 | 15,370,926 | **24.5x** | **9.0x** |
| String concatenation | 3,720,389 | 16,013,684 | 17,253,417 | **4.3x** | **4.6x** |
| Substring | 6,218,028 | 31,837,718 | 36,874,209 | **5.1x** | **5.9x** |
| Three string comparisons | 3,204,790 | 27,130,956 | 31,471,547 | **8.5x** | **9.8x** |
| Twenty-clause AND chain | 459,886 | 2,424,002 | 2,152,473 | **5.3x** | **4.7x** |
| Two string comparisons | 4,613,754 | 33,225,169 | 37,230,766 | **7.2x** | **8.1x** |

Key observations:

- **Both compilers beat the interpreter in every scenario**, by roughly 1.5x-36x. The interpreter pays a map lookup + virtual dispatch on every operator; the compilers emit direct code.
- **`javac` and `jep484` are on par for most rules.** Most scenarios, including `map` and `reduce`, are within about 15% of each other, with `jep484` slightly ahead more often than not.
- **`javac` still leads on repeated variable lookups (~2.7x) and dispatch tables (~1.6-2.3x).** The `javac` compiler hoists repeated `{"var":"x"}` lookups into `final` locals; `jep484` resolves each occurrence separately.
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
