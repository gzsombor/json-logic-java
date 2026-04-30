package io.github.jamsesso.jsonlogic.bench;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.Map;

@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class JmhJsonLogicBenchmark {

  @Param({"false", "true"})
  private boolean compiled;

  private JsonLogic jsonLogic;

  // 5-clause mixed numeric+string "and" chain.
  private String logic5;
  private Map<String, Object> data5;

  // if (role == "admin" && region == "us") "full-access" else "limited"
  private String logicStr2;
  private Map<String, Object> dataStr2;

  // if (role == "editor" && region == "eu" && tier == "pro") "eu-pro-editor" else "default"
  private String logicStr3;
  private Map<String, Object> dataStr3;

  // if (role == "viewer" && region == "ap" && tier == "free" && lang == "en") ... else "default"
  private String logicStr4;
  private Map<String, Object> dataStr4;

  // 5-branch if/elseif dispatch table: status -> human-readable label.
  private String logicDispatch;
  private Map<String, Object> dataDispatchHit;   // matches the 3rd branch ("rejected")
  private Map<String, Object> dataDispatchMiss;  // falls through to "Unknown"

  // 20-clause mixed numeric+string "and" chain.
  private String logic20;
  private Map<String, Object> data20;

  // 'a' appears 3 times, 'b' appears 3 times → exercises var deduplication.
  private String lookupRepeated;
  private Map<String, Object> dataLookupRepeated;

  // if (Set.of("cust1".."cust5").contains(customer)) "ok" else "not_ok"
  private String logicInSet;
  private Map<String, Object> dataInSetHit;   // customer = "cust3" → "ok"
  private Map<String, Object> dataInSetMiss;  // customer = "unknown" → "not_ok"

  // (a + b + 10) * 3  — two vars + two constants, exercises addScalars + mulScalars
  private String logicArithmetic;
  private Map<String, Object> dataArithmetic;

  @Setup
  public void setup() {
    jsonLogic = new JsonLogic(compiled);

    logic5 = "{\"and\":[{\">\":[{\"var\":\"score\"},10]},{\"<\":[{\"var\":\"count\"},100]},"
        + "{\"==\":[{\"var\":\"category\"},\"x\"]},{\"!=\":[{\"var\":\"tag\"},\"y\"]},"
        + "{\">=\":[{\"var\":\"offset\"},0]}]}";
    data5 = new HashMap<>();
    data5.put("score", 42);
    data5.put("count", 50);
    data5.put("category", "x");
    data5.put("tag", "z");
    data5.put("offset", 0);

    logicStr2 = "{\"if\":[{\"and\":[{\"==\":[{\"var\":\"role\"},\"admin\"]},"
        + "{\"==\":[{\"var\":\"region\"},\"us\"]}]},"
        + "\"full-access\",\"limited\"]}";
    dataStr2 = new HashMap<>();
    dataStr2.put("role", "admin");
    dataStr2.put("region", "us");

    logicStr3 = "{\"if\":[{\"and\":[{\"==\":[{\"var\":\"role\"},\"editor\"]},"
        + "{\"==\":[{\"var\":\"region\"},\"eu\"]},"
        + "{\"==\":[{\"var\":\"tier\"},\"pro\"]}]},"
        + "\"eu-pro-editor\",\"default\"]}";
    dataStr3 = new HashMap<>();
    dataStr3.put("role", "editor");
    dataStr3.put("region", "eu");
    dataStr3.put("tier", "pro");

    logicStr4 = "{\"if\":[{\"and\":[{\"==\":[{\"var\":\"role\"},\"viewer\"]},"
        + "{\"==\":[{\"var\":\"region\"},\"ap\"]},"
        + "{\"==\":[{\"var\":\"tier\"},\"free\"]},"
        + "{\"==\":[{\"var\":\"lang\"},\"en\"]}]},"
        + "\"ap-free-viewer-en\",\"default\"]}";
    dataStr4 = new HashMap<>();
    dataStr4.put("role", "viewer");
    dataStr4.put("region", "ap");
    dataStr4.put("tier", "free");
    dataStr4.put("lang", "en");

    logicDispatch = "{\"if\":["
        + "{\"==\":[{\"var\":\"status\"},\"pending\"]},\"Pending Review\","
        + "{\"==\":[{\"var\":\"status\"},\"approved\"]},\"Approved\","
        + "{\"==\":[{\"var\":\"status\"},\"rejected\"]},\"Rejected\","
        + "{\"==\":[{\"var\":\"status\"},\"cancelled\"]},\"Cancelled\","
        + "\"Unknown\"]}";
    dataDispatchHit = new HashMap<>();
    dataDispatchHit.put("status", "rejected");
    dataDispatchMiss = new HashMap<>();
    dataDispatchMiss.put("status", "archived");

    logic20 = "{\"and\":["
        + "{\">\":[{\"var\":\"n0\"},0]},"
        + "{\">\":[{\"var\":\"n1\"},1]},"
        + "{\">\":[{\"var\":\"n2\"},2]},"
        + "{\">\":[{\"var\":\"n3\"},3]},"
        + "{\">\":[{\"var\":\"n4\"},4]},"
        + "{\">\":[{\"var\":\"n5\"},5]},"
        + "{\">\":[{\"var\":\"n6\"},6]},"
        + "{\">\":[{\"var\":\"n7\"},7]},"
        + "{\">\":[{\"var\":\"n8\"},8]},"
        + "{\">\":[{\"var\":\"n9\"},9]},"
        + "{\"==\":[{\"var\":\"s0\"},\"a\"]},"
        + "{\"==\":[{\"var\":\"s1\"},\"b\"]},"
        + "{\"==\":[{\"var\":\"s2\"},\"c\"]},"
        + "{\"==\":[{\"var\":\"s3\"},\"d\"]},"
        + "{\"==\":[{\"var\":\"s4\"},\"e\"]},"
        + "{\"==\":[{\"var\":\"s5\"},\"f\"]},"
        + "{\"==\":[{\"var\":\"s6\"},\"g\"]},"
        + "{\"==\":[{\"var\":\"s7\"},\"h\"]},"
        + "{\"==\":[{\"var\":\"s8\"},\"i\"]},"
        + "{\"==\":[{\"var\":\"s9\"},\"j\"]}"
        + "]}";
    data20 = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      data20.put("n" + i, i + 1);
    }
    final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
    for (int i = 0; i < 10; i++) {
      data20.put("s" + i, letters[i]);
    }

    lookupRepeated = "{\"and\":[{\">\":[{\"var\":\"a\"},10]},{\"<\":[{\"var\":\"a\"},100]}"
        + ",{\">=\":[{\"var\":\"b\"},0]},{\"<=\":[{\"var\":\"b\"},50]}"
        + ",{\"!=\":[{\"var\":\"a\"},{\"var\":\"b\"}]}]}";
    dataLookupRepeated = new HashMap<>();
    dataLookupRepeated.put("a", 42);
    dataLookupRepeated.put("b", 30);
    dataLookupRepeated.put("c", "x");
    dataLookupRepeated.put("d", "z");
    dataLookupRepeated.put("e", 0);

    logicInSet = "{\"if\":[{\"in\":[{\"var\":\"customer\"},"
        + "[\"cust1\",\"cust2\",\"cust3\",\"cust4\",\"cust5\"]]},"
        + "\"ok\",\"not_ok\"]}";
    dataInSetHit = new HashMap<>();
    dataInSetHit.put("customer", "cust3");
    dataInSetMiss = new HashMap<>();
    dataInSetMiss.put("customer", "unknown");

    logicArithmetic = "{\"*\":[{\"+\":[{\"var\":\"a\"},{\"var\":\"b\"},10]},3]}";
    dataArithmetic = new HashMap<>();
    dataArithmetic.put("a", 7);
    dataArithmetic.put("b", 3);
  }

  @Benchmark
  public Object evaluateFive() throws JsonLogicException {
    return jsonLogic.apply(logic5, data5);
  }

  @Benchmark
  public Object evaluateTwoStringComparisons() throws JsonLogicException {
    return jsonLogic.apply(logicStr2, dataStr2);
  }

  @Benchmark
  public Object evaluateThreeStringComparisons() throws JsonLogicException {
    return jsonLogic.apply(logicStr3, dataStr3);
  }

  @Benchmark
  public Object evaluateFourStringComparisons() throws JsonLogicException {
    return jsonLogic.apply(logicStr4, dataStr4);
  }

  @Benchmark
  public Object evaluateDispatchTableHit() throws JsonLogicException {
    return jsonLogic.apply(logicDispatch, dataDispatchHit);
  }

  @Benchmark
  public Object evaluateDispatchTableMiss() throws JsonLogicException {
    return jsonLogic.apply(logicDispatch, dataDispatchMiss);
  }

  @Benchmark
  public Object evaluateTwentyClauses() throws JsonLogicException {
    return jsonLogic.apply(logic20, data20);
  }

  @Benchmark
  public Object evaluateRepeatedLookup() throws JsonLogicException {
    return jsonLogic.apply(lookupRepeated, dataLookupRepeated);
  }

  @Benchmark
  public Object evaluateInSetHit() throws JsonLogicException {
    return jsonLogic.apply(logicInSet, dataInSetHit);
  }

  @Benchmark
  public Object evaluateInSetMiss() throws JsonLogicException {
    return jsonLogic.apply(logicInSet, dataInSetMiss);
  }

  @Benchmark
  public Object evaluateArithmetic() throws JsonLogicException {
    return jsonLogic.apply(logicArithmetic, dataArithmetic);
  }
}
