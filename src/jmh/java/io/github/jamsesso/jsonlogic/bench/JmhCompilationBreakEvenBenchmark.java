package io.github.jamsesso.jsonlogic.bench;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.compiler.JsonLogicCompiler;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JmhCompilationBreakEvenBenchmark {

  @Param({"fiveClauses", "twentyClauses", "dispatchMiss"})
  private String ruleShape;

  private JsonLogic interpreter;
  private JsonLogic compiled;
  private JsonLogicCompiler compiler;
  private String ruleJson;
  private JsonLogicNode ruleAst;
  private Map<String, Object> data;

  @Setup(Level.Trial)
  public void setup() throws JsonLogicException {
    interpreter = new JsonLogic(false);
    compiled = new JsonLogic();
    compiler = new JsonLogicCompiler(new JsonLogicEvaluator(Collections.emptyList())).setStrictMode(true);

    switch (ruleShape) {
      case "fiveClauses":
        setupFiveClauses();
        break;
      case "twentyClauses":
        setupTwentyClauses();
        break;
      case "dispatchMiss":
        setupDispatchMiss();
        break;
      default:
        throw new IllegalArgumentException("Unknown rule shape: " + ruleShape);
    }

    ruleAst = JsonLogicParser.parse(ruleJson);

    // Populate parse/compile caches so evaluation benchmarks measure steady-state cost only.
    interpreter.apply(ruleJson, data);
    compiled.apply(ruleJson, data);
  }

  @Setup(Level.Invocation)
  public void resetCompilerCache() {
    compiler.invalidate();
  }

  @Benchmark
  public Object interpreterEvaluate() throws JsonLogicException {
    return interpreter.apply(ruleJson, data);
  }

  @Benchmark
  public Object compiledEvaluate() throws JsonLogicException {
    return compiled.apply(ruleJson, data);
  }

  @Benchmark
  public CompiledRule coldCompile() {
    return compiler.compile(ruleJson, ruleAst);
  }

  private void setupFiveClauses() {
    ruleJson = "{\"and\":[{\">\":[{\"var\":\"score\"},10]},{\"<\":[{\"var\":\"count\"},100]},"
        + "{\"==\":[{\"var\":\"category\"},\"x\"]},{\"!=\":[{\"var\":\"tag\"},\"y\"]},"
        + "{\">=\":[{\"var\":\"offset\"},0]}]}";

    data = new HashMap<>();
    data.put("score", 42);
    data.put("count", 50);
    data.put("category", "x");
    data.put("tag", "z");
    data.put("offset", 0);
  }

  private void setupTwentyClauses() {
    ruleJson = "{\"and\":["
        + "{\">\":[{\"var\":\"number0\"},0]},"
        + "{\">\":[{\"var\":\"number1\"},1]},"
        + "{\">\":[{\"var\":\"number2\"},2]},"
        + "{\">\":[{\"var\":\"number3\"},3]},"
        + "{\">\":[{\"var\":\"number4\"},4]},"
        + "{\">\":[{\"var\":\"number5\"},5]},"
        + "{\">\":[{\"var\":\"number6\"},6]},"
        + "{\">\":[{\"var\":\"number7\"},7]},"
        + "{\">\":[{\"var\":\"number8\"},8]},"
        + "{\">\":[{\"var\":\"number9\"},9]},"
        + "{\"==\":[{\"var\":\"string0\"},\"a\"]},"
        + "{\"==\":[{\"var\":\"string1\"},\"b\"]},"
        + "{\"==\":[{\"var\":\"string2\"},\"c\"]},"
        + "{\"==\":[{\"var\":\"string3\"},\"d\"]},"
        + "{\"==\":[{\"var\":\"string4\"},\"e\"]},"
        + "{\"==\":[{\"var\":\"string5\"},\"f\"]},"
        + "{\"==\":[{\"var\":\"string6\"},\"g\"]},"
        + "{\"==\":[{\"var\":\"string7\"},\"h\"]},"
        + "{\"==\":[{\"var\":\"string8\"},\"i\"]},"
        + "{\"==\":[{\"var\":\"string9\"},\"j\"]}"
        + "]}";

    data = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      data.put("number" + i, i + 1);
    }

    final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
    for (int i = 0; i < 10; i++) {
      data.put("string" + i, letters[i]);
    }
  }

  private void setupDispatchMiss() {
    ruleJson = "{\"if\":["
        + "{\"==\":[{\"var\":\"status\"},\"pending\"]},\"Pending Review\","
        + "{\"==\":[{\"var\":\"status\"},\"approved\"]},\"Approved\","
        + "{\"==\":[{\"var\":\"status\"},\"rejected\"]},\"Rejected\","
        + "{\"==\":[{\"var\":\"status\"},\"cancelled\"]},\"Cancelled\","
        + "\"Unknown\"]}";

    data = new HashMap<>();
    data.put("status", "archived");
  }
}
