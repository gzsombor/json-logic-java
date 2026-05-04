package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke-tests for specific rules to make it easy to inspect generated source and
 * assert individual behaviours without running the full fixture suite.
 */
public class RuleSourceGeneratorTest {

  private JsonLogic compiled;

  @BeforeEach
  public void setUp() {
    compiled = new JsonLogic();
  }

  // ---- primitives ----

  @Test
  public void nullLiteral() throws JsonLogicException {
    assertNull(compiled.apply("null", null));
  }

  @Test
  public void trueLiteral() throws JsonLogicException {
    assertEquals(Boolean.TRUE, compiled.apply("true", null));
  }

  @Test
  public void numberLiteral() throws JsonLogicException {
    assertEquals(42.0, compiled.apply("42", null));
  }

  @Test
  public void stringLiteral() throws JsonLogicException {
    assertEquals("hello", compiled.apply("\"hello\"", null));
  }

  // ---- var ----

  @Test
  public void varSimpleKey() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("x", 7);
    assertEquals(7.0, compiled.apply("{\"var\":\"x\"}", data));
  }

  @Test
  public void varMissingKeyReturnsDefault() throws JsonLogicException {
    assertEquals("fallback",
        compiled.apply("{\"var\":[\"missing\",\"fallback\"]}", new HashMap<>()));
  }

  @Test
  public void varEmptyKeyReturnsData() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("a", 1);
    assertSame(data, compiled.apply("{\"var\":\"\"}", data));
  }

  // ---- equality ----

  @Test
  public void looseEqSameType() throws JsonLogicException {
    assertEquals(Boolean.TRUE, compiled.apply("{\"==\":[1,1]}", null));
  }

  @Test
  public void looseEqDifferentType() throws JsonLogicException {
    assertEquals(Boolean.TRUE, compiled.apply("{\"==\":[1,\"1\"]}", null));
  }

  @Test
  public void looseNeq() throws JsonLogicException {
    assertEquals(Boolean.TRUE, compiled.apply("{\"!=\":[1,2]}", null));
  }

  @Test
  public void strictEq() throws JsonLogicException {
    assertEquals(Boolean.TRUE,  compiled.apply("{\"===\":[1,1]}", null));
    assertEquals(Boolean.FALSE, compiled.apply("{\"===\":[1,\"1\"]}", null));
  }

  // ---- boolean logic ----

  @Test
  public void andShortCircuitsOnFalse() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("flag", false);
    assertEquals(Boolean.FALSE,
        compiled.apply("{\"and\":[false,true]}", null));
  }

  @Test
  public void orShortCircuitsOnTrue() throws JsonLogicException {
    assertEquals(Boolean.TRUE,
        compiled.apply("{\"or\":[true,false]}", null));
  }

  @Test
  public void andReturnsLastWhenAllTruthy() throws JsonLogicException {
    assertEquals("last",
        compiled.apply("{\"and\":[true,true,\"last\"]}", null));
  }

  @Test
  public void orReturnsFalsyLast() throws JsonLogicException {
    assertEquals(Boolean.FALSE,
        compiled.apply("{\"or\":[false,false]}", null));
  }

  // ---- if ----

  @Test
  public void ifTrue() throws JsonLogicException {
    assertEquals("yes", compiled.apply("{\"if\":[true,\"yes\",\"no\"]}", null));
  }

  @Test
  public void ifFalse() throws JsonLogicException {
    assertEquals("no", compiled.apply("{\"if\":[false,\"yes\",\"no\"]}", null));
  }

  @Test
  public void ifElseIfChain() throws JsonLogicException {
    assertEquals("c",
        compiled.apply("{\"if\":[false,\"a\",false,\"b\",\"c\"]}", null));
  }

  @Test
  public void ifNoElseMissingBranch() throws JsonLogicException {
    assertNull(compiled.apply("{\"if\":[false,\"a\"]}", null));
  }

  // ---- numeric comparisons ----

  @Test
  public void greaterThan() throws JsonLogicException {
    assertEquals(Boolean.TRUE,  compiled.apply("{\">\":[10,5]}", null));
    assertEquals(Boolean.FALSE, compiled.apply("{\">\":[5,10]}", null));
  }

  @Test
  public void betweenExclusive() throws JsonLogicException {
    assertEquals(Boolean.TRUE,
        compiled.apply("{\"<\":[1,5,10]}", null));
    assertEquals(Boolean.FALSE,
        compiled.apply("{\"<\":[1,1,10]}", null));
  }

  // ---- arithmetic ----

  @Test
  public void addition() throws JsonLogicException {
    assertEquals(3.0, compiled.apply("{\"+\":[1,2]}", null));
  }

  @Test
  public void unaryMinus() throws JsonLogicException {
    assertEquals(-5.0, compiled.apply("{\"-\":[5]}", null));
  }

  @Test
  public void minMax() throws JsonLogicException {
    assertEquals(1.0, compiled.apply("{\"min\":[3,1,2]}", null));
    assertEquals(3.0, compiled.apply("{\"max\":[3,1,2]}", null));
  }

  // ---- not ----

  @Test
  public void notTrue() throws JsonLogicException {
    assertEquals(Boolean.FALSE, compiled.apply("{\"!\":[true]}", null));
  }

  @Test
  public void doubleNot() throws JsonLogicException {
    assertEquals(Boolean.TRUE, compiled.apply("{\"!!\":[1]}", null));
  }

  // ---- string rule with var (benchmark scenario) ----

  @Test
  public void twoStringComparisons() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("role", "admin");
    data.put("region", "us");
    String rule = "{\"if\":[{\"and\":[{\"==\":[{\"var\":\"role\"},\"admin\"]},"
        + "{\"==\":[{\"var\":\"region\"},\"us\"]}]},"
        + "\"full-access\",\"limited\"]}";
    assertEquals("full-access", compiled.apply(rule, data));

    data.put("role", "user");
    assertEquals("limited", compiled.apply(rule, data));
  }

  // ---- fallback for unknown operators ----

  @Test
  public void customOperatorFallsBackToInterpreter() throws JsonLogicException {
    JsonLogic withCustom = new JsonLogic();
    withCustom.addOperation("double", args -> ((Number) args[0]).doubleValue() * 2);

    assertEquals(10.0, withCustom.apply("{\"double\":[5]}", null));
  }

  // ---- var deduplication ----

  @Test
  public void repeatedVarIsResolvedOnlyOnce() throws Exception {
    Map<String, Object> data = new HashMap<>();
    data.put("score", 75);
    String rule = "{\"and\":[{\">=\":[{\"var\":\"score\"},50]},{\"<\":[{\"var\":\"score\"},100]}]}";
    assertEquals(Boolean.TRUE, compiled.apply(rule, data));

    data.put("score", 25);
    assertEquals(Boolean.FALSE, compiled.apply(rule, data));

    data.put("score", 100);
    assertEquals(Boolean.FALSE, compiled.apply(rule, data));
  }

  @Test
  public void repeatedVarGeneratesOneFinalLocal() throws Exception {
    final String ruleJson = "{\"and\":[{\">\":[{\"var\":\"x\"},0]},{\"<\":[{\"var\":\"x\"},10]}]}";
    final var generator = new RuleSourceGenerator();
    final String source = generator.generate(
        io.github.jamsesso.jsonlogic.ast.JsonLogicParser.parse(ruleJson), "TestRule");

    int resolveVarCount = countOccurrences(source, "resolveVarChecked(data, \"x\", null)");
    assertEquals(1, resolveVarCount, "Expected exactly 1 resolveVarChecked call for 'x'");

    assertTrue(source.contains("final Object var_x_"),
        "Expected a final Object local for var 'x'");
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int idx = 0;
    while ((idx = haystack.indexOf(needle, idx)) != -1) {
      count++;
      idx += needle.length();
    }
    return count;
  }

  // ---- toString ----

  @Test
  public void compiledRuleToStringContainsRuleJson() throws Exception {
    final String ruleJson = "{\"==\":[{\"var\":\"x\"},1]}";
    final var evaluator = new JsonLogicEvaluator(java.util.Collections.emptyList());
    final var compiler = new JsonLogicCompiler(evaluator);
    final var rule = compiler.compile(ruleJson, JsonLogicParser.parse(ruleJson));

    assertEquals("CompiledRule(" + ruleJson + ")", rule.toString());
  }
}
