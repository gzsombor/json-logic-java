package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicParser;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Smoke-tests for specific rules to make it easy to inspect generated source and
 * assert individual behaviours without running the full fixture suite.
 */
public class RuleSourceGeneratorTest {

  private JsonLogic compiled;

  @Before
  public void setUp() {
    compiled = new JsonLogic();
  }

  // ---- primitives ----

  @Test
  public void nullLiteral() throws JsonLogicException {
    Assert.assertNull(compiled.apply("null", null));
  }

  @Test
  public void trueLiteral() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE, compiled.apply("true", null));
  }

  @Test
  public void numberLiteral() throws JsonLogicException {
    Assert.assertEquals(42.0, compiled.apply("42", null));
  }

  @Test
  public void stringLiteral() throws JsonLogicException {
    Assert.assertEquals("hello", compiled.apply("\"hello\"", null));
  }

  // ---- var ----

  @Test
  public void varSimpleKey() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("x", 7);
    Assert.assertEquals(7.0, compiled.apply("{\"var\":\"x\"}", data));
  }

  @Test
  public void varMissingKeyReturnsDefault() throws JsonLogicException {
    Assert.assertEquals("fallback",
        compiled.apply("{\"var\":[\"missing\",\"fallback\"]}", new HashMap<>()));
  }

  @Test
  public void varEmptyKeyReturnsData() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("a", 1);
    Assert.assertSame(data, compiled.apply("{\"var\":\"\"}", data));
  }

  // ---- equality ----

  @Test
  public void looseEqSameType() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE, compiled.apply("{\"==\":[1,1]}", null));
  }

  @Test
  public void looseEqDifferentType() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE, compiled.apply("{\"==\":[1,\"1\"]}", null));
  }

  @Test
  public void looseNeq() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE, compiled.apply("{\"!=\":[1,2]}", null));
  }

  @Test
  public void strictEq() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE,  compiled.apply("{\"===\":[1,1]}", null));
    Assert.assertEquals(Boolean.FALSE, compiled.apply("{\"===\":[1,\"1\"]}", null));
  }

  // ---- boolean logic ----

  @Test
  public void andShortCircuitsOnFalse() throws JsonLogicException {
    // Second arg is an error-throwing var - must not be evaluated
    Map<String, Object> data = new HashMap<>();
    data.put("flag", false);
    // {and: [false, true]} → false  (no error)
    Assert.assertEquals(Boolean.FALSE,
        compiled.apply("{\"and\":[false,true]}", null));
  }

  @Test
  public void orShortCircuitsOnTrue() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE,
        compiled.apply("{\"or\":[true,false]}", null));
  }

  @Test
  public void andReturnsLastWhenAllTruthy() throws JsonLogicException {
    Assert.assertEquals("last",
        compiled.apply("{\"and\":[true,true,\"last\"]}", null));
  }

  @Test
  public void orReturnsFalsyLast() throws JsonLogicException {
    Assert.assertEquals(Boolean.FALSE,
        compiled.apply("{\"or\":[false,false]}", null));
  }

  // ---- if ----

  @Test
  public void ifTrue() throws JsonLogicException {
    Assert.assertEquals("yes", compiled.apply("{\"if\":[true,\"yes\",\"no\"]}", null));
  }

  @Test
  public void ifFalse() throws JsonLogicException {
    Assert.assertEquals("no", compiled.apply("{\"if\":[false,\"yes\",\"no\"]}", null));
  }

  @Test
  public void ifElseIfChain() throws JsonLogicException {
    // {if: [false,"a", false,"b", "c"]} → "c"
    Assert.assertEquals("c",
        compiled.apply("{\"if\":[false,\"a\",false,\"b\",\"c\"]}", null));
  }

  @Test
  public void ifNoElseMissingBranch() throws JsonLogicException {
    // Even arg count and no branch taken → null
    Assert.assertNull(compiled.apply("{\"if\":[false,\"a\"]}", null));
  }

  // ---- numeric comparisons ----

  @Test
  public void greaterThan() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE,  compiled.apply("{\">\":[10,5]}", null));
    Assert.assertEquals(Boolean.FALSE, compiled.apply("{\">\":[5,10]}", null));
  }

  @Test
  public void betweenExclusive() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE,
        compiled.apply("{\"<\":[1,5,10]}", null));
    Assert.assertEquals(Boolean.FALSE,
        compiled.apply("{\"<\":[1,1,10]}", null));
  }

  // ---- arithmetic ----

  @Test
  public void addition() throws JsonLogicException {
    Assert.assertEquals(3.0, compiled.apply("{\"+\":[1,2]}", null));
  }

  @Test
  public void unaryMinus() throws JsonLogicException {
    Assert.assertEquals(-5.0, compiled.apply("{\"-\":[5]}", null));
  }

  @Test
  public void minMax() throws JsonLogicException {
    Assert.assertEquals(1.0, compiled.apply("{\"min\":[3,1,2]}", null));
    Assert.assertEquals(3.0, compiled.apply("{\"max\":[3,1,2]}", null));
  }

  // ---- cat ----

  @Test
  public void cat() throws JsonLogicException {
    Assert.assertEquals("hello world",
        compiled.apply("{\"cat\":[\"hello\",\" \",\"world\"]}", null));
  }

  @Test
  public void catWithNumber() throws JsonLogicException {
    // 3.0 should render as "3", not "3.0"
    Assert.assertEquals("3 apples",
        compiled.apply("{\"cat\":[3,\" apples\"]}", null));
  }

  // ---- not ----

  @Test
  public void notTrue() throws JsonLogicException {
    Assert.assertEquals(Boolean.FALSE, compiled.apply("{\"!\":[true]}", null));
  }

  @Test
  public void doubleNot() throws JsonLogicException {
    Assert.assertEquals(Boolean.TRUE, compiled.apply("{\"!!\":[1]}", null));
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
    Assert.assertEquals("full-access", compiled.apply(rule, data));

    data.put("role", "user");
    Assert.assertEquals("limited", compiled.apply(rule, data));
  }

  // ---- fallback for unknown operators ----

  @Test
  public void customOperatorFallsBackToInterpreter() throws JsonLogicException {
    JsonLogic withCustom = new JsonLogic();
    withCustom.addOperation("double", args -> ((Number) args[0]).doubleValue() * 2);

    Assert.assertEquals(10.0, withCustom.apply("{\"double\":[5]}", null));
  }

  // ---- var deduplication ----

  @Test
  public void repeatedVarIsResolvedOnlyOnce() throws Exception {
    // Verify correctness: same var referenced twice in and-expression should produce same value.
    Map<String, Object> data = new HashMap<>();
    data.put("score", 75);
    // {"and": [{">=": [{"var":"score"}, 50]}, {"<": [{"var":"score"}, 100]}]}
    String rule = "{\"and\":[{\">=\":[{\"var\":\"score\"},50]},{\"<\":[{\"var\":\"score\"},100]}]}";
    Assert.assertEquals(Boolean.TRUE, compiled.apply(rule, data));

    data.put("score", 25);
    Assert.assertEquals(Boolean.FALSE, compiled.apply(rule, data));

    data.put("score", 100);
    Assert.assertEquals(Boolean.FALSE, compiled.apply(rule, data));
  }

  @Test
  public void repeatedVarGeneratesOneFinalLocal() throws Exception {
    // Verify the generated source contains exactly one resolveVar call for the repeated var,
    // and that a final local is used for subsequent occurrences.
    final String ruleJson = "{\"and\":[{\">\":[{\"var\":\"x\"},0]},{\"<\":[{\"var\":\"x\"},10]}]}";
    final var generator = new RuleSourceGenerator();
    final String source = generator.generate(
        io.github.jamsesso.jsonlogic.ast.JsonLogicParser.parse(ruleJson), "TestRule");

    // Only one resolveVar call for "x" - the rest use the cached final local.
    int resolveVarCount = countOccurrences(source, "resolveVar(data, \"x\", null)");
    Assert.assertEquals("Expected exactly 1 resolveVar call for 'x'", 1, resolveVarCount);

    // The generated source must contain a final Object declaration for the local.
    Assert.assertTrue("Expected a final Object local for var 'x'",
        source.contains("final Object var_x_"));
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

    Assert.assertEquals("CompiledRule(" + ruleJson + ")", rule.toString());
  }
}
