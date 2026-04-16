package io.github.jamsesso.jsonlogic.bench;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.HashMap;
import java.util.Map;

@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
public class JmhJsonLogicBenchmark {
  private JsonLogic jsonLogic;
  private JsonLogic jsonLogicCompiled;  // same rules, JIT-compiled path

  // Original: 5-clause mixed numeric+string "and" chain
  private String logic5;
  private Map<String, Object> data5;

  // 2 string vars compared to fixed values; return one of two string results.
  // Rule: if (role == "admin" && region == "us") "full-access" else "limited"
  private String logicStr2;
  private Map<String, Object> dataStr2;

  // 3 string vars compared to fixed values; return one of two string results.
  // Rule: if (role == "editor" && region == "eu" && tier == "pro") "eu-pro-editor" else "default"
  private String logicStr3;
  private Map<String, Object> dataStr3;

  // 4 string vars compared to fixed values; return one of two string results.
  // Rule: if (role == "viewer" && region == "ap" && tier == "free" && lang == "en") "ap-free-viewer-en" else "default"
  private String logicStr4;
  private Map<String, Object> dataStr4;

  // Dispatch table: 5-branch if/elseif chain matching a single string var against fixed literals.
  // Simulates routing logic: status -> human-readable label.
  // Rule: if status=="pending" -> "Pending Review"
  //       elif status=="approved" -> "Approved"
  //       elif status=="rejected" -> "Rejected"
  //       elif status=="cancelled" -> "Cancelled"
  //       else "Unknown"
  private String logicDispatch;
  private Map<String, Object> dataDispatchHit;   // matches the 3rd branch ("rejected")
  private Map<String, Object> dataDispatchMiss;  // falls through to "Unknown"

  // 20-clause mixed numeric+string "and" chain.
  // Exercises the interpreter's per-node dispatch and the compiler's ability to fold
  // many comparisons into a single inlined method.
  private String logic20;
  private Map<String, Object> data20;

  @Setup
  public void setup() {
    jsonLogic = new JsonLogic(false);
    jsonLogicCompiled = new JsonLogic();

    // --- original ---
    logic5 = """
        {"and":[{">":[{"var":"score"},10]},{"<":[{"var":"count"},100]},\
        {"==":[{"var":"category"},"x"]},{"!=":[{"var":"tag"},"y"]},\
        {">=":[{"var":"offset"},0]}]}""";
    data5 = new HashMap<>();
    data5.put("score", 42);
    data5.put("count", 50);
    data5.put("category", "x");
    data5.put("tag", "z");
    data5.put("offset", 0);

    // --- 2 string comparisons ---
    logicStr2 = """
        {"if":[{"and":[{"==":[{"var":"role"},"admin"]},\
        {"==":[{"var":"region"},"us"]}]},\
        "full-access","limited"]}""";
    dataStr2 = new HashMap<>();
    dataStr2.put("role", "admin");
    dataStr2.put("region", "us");

    // --- 3 string comparisons ---
    logicStr3 = """
        {"if":[{"and":[{"==":[{"var":"role"},"editor"]},\
        {"==":[{"var":"region"},"eu"]},\
        {"==":[{"var":"tier"},"pro"]}]},\
        "eu-pro-editor","default"]}""";
    dataStr3 = new HashMap<>();
    dataStr3.put("role", "editor");
    dataStr3.put("region", "eu");
    dataStr3.put("tier", "pro");

    // --- 4 string comparisons ---
    logicStr4 = """
        {"if":[{"and":[{"==":[{"var":"role"},"viewer"]},\
        {"==":[{"var":"region"},"ap"]},\
        {"==":[{"var":"tier"},"free"]},\
        {"==":[{"var":"lang"},"en"]}]},\
        "ap-free-viewer-en","default"]}""";
    dataStr4 = new HashMap<>();
    dataStr4.put("role", "viewer");
    dataStr4.put("region", "ap");
    dataStr4.put("tier", "free");
    dataStr4.put("lang", "en");

    // --- dispatch table (5-branch if/elseif) ---
    logicDispatch = """
        {"if":[
          {"==":[{"var":"status"},"pending"]},"Pending Review",
          {"==":[{"var":"status"},"approved"]},"Approved",
          {"==":[{"var":"status"},"rejected"]},"Rejected",
          {"==":[{"var":"status"},"cancelled"]},"Cancelled",
          "Unknown"]}""";
    dataDispatchHit = new HashMap<>();
    dataDispatchHit.put("status", "rejected");   // matches branch 3
    dataDispatchMiss = new HashMap<>();
    dataDispatchMiss.put("status", "archived");  // falls through to "Unknown"

    // --- 20-clause and ---
    logic20 = """
        {"and":[
          {">":[{"var":"n0"},0]},
          {">":[{"var":"n1"},1]},
          {">":[{"var":"n2"},2]},
          {">":[{"var":"n3"},3]},
          {">":[{"var":"n4"},4]},
          {">":[{"var":"n5"},5]},
          {">":[{"var":"n6"},6]},
          {">":[{"var":"n7"},7]},
          {">":[{"var":"n8"},8]},
          {">":[{"var":"n9"},9]},
          {"==":[{"var":"s0"},"a"]},
          {"==":[{"var":"s1"},"b"]},
          {"==":[{"var":"s2"},"c"]},
          {"==":[{"var":"s3"},"d"]},
          {"==":[{"var":"s4"},"e"]},
          {"==":[{"var":"s5"},"f"]},
          {"==":[{"var":"s6"},"g"]},
          {"==":[{"var":"s7"},"h"]},
          {"==":[{"var":"s8"},"i"]},
          {"==":[{"var":"s9"},"j"]}
        ]}""";
    data20 = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      data20.put("n" + i, i + 1);
    }
    final String[] letters = {"a","b","c","d","e","f","g","h","i","j"};
    for (int i = 0; i < 10; i++) {
      data20.put("s" + i, letters[i]);
    }
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

  // ---- compiled variants ----

  @Benchmark
  public Object compiledEvaluateFive() throws JsonLogicException {
    return jsonLogicCompiled.apply(logic5, data5);
  }

  @Benchmark
  public Object compiledEvaluateTwoStringComparisons() throws JsonLogicException {
    return jsonLogicCompiled.apply(logicStr2, dataStr2);
  }

  @Benchmark
  public Object compiledEvaluateThreeStringComparisons() throws JsonLogicException {
    return jsonLogicCompiled.apply(logicStr3, dataStr3);
  }

  @Benchmark
  public Object compiledEvaluateFourStringComparisons() throws JsonLogicException {
    return jsonLogicCompiled.apply(logicStr4, dataStr4);
  }

  @Benchmark
  public Object compiledEvaluateDispatchTableHit() throws JsonLogicException {
    return jsonLogicCompiled.apply(logicDispatch, dataDispatchHit);
  }

  @Benchmark
  public Object compiledEvaluateDispatchTableMiss() throws JsonLogicException {
    return jsonLogicCompiled.apply(logicDispatch, dataDispatchMiss);
  }

  @Benchmark
  public Object evaluateTwentyClauses() throws JsonLogicException {
    return jsonLogic.apply(logic20, data20);
  }

  @Benchmark
  public Object compiledEvaluateTwentyClauses() throws JsonLogicException {
    return jsonLogicCompiled.apply(logic20, data20);
  }
}
