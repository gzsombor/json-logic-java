package io.github.jamsesso.jsonlogic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class NumericComparisonExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true).setStrictCompilation(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public NumericComparisonExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  // ==================== NUMBER COMPARISONS ====================

  @Test
  public void testLessThanWithNumbers() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1, 2]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [2, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 1]}", null));
  }

  @Test
  public void testLessThanOrEqualWithNumbers() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 2]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<=\" : [2, 1]}", null));
  }

  @Test
  public void testGreaterThanWithNumbers() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [2, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [1, 2]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [1, 1]}", null));
  }

  @Test
  public void testGreaterThanOrEqualWithNumbers() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [2, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [1, 2]}", null));
  }

  @Test
  public void testDecimalComparisons() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1.5, 1.4]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1.4, 1.5]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [1.5, 1.5]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1.5, 1.5]}", null));
  }

  @Test
  public void testNegativeNumberComparisons() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [0, -1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [-1, -2]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [-2, -1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [-1, 0]}", null));
  }

  // ==================== BETWEEN COMPARISONS (3 ARGUMENTS) ====================

  @Test
  public void testBetweenExclusive() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1, 2, 3]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 1, 3]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 4, 3]}", null));
  }

  @Test
  public void testBetweenInclusive() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 1, 3]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 2, 3]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 3, 3]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<=\" : [1, 4, 3]}", null));
  }

  @Test
  public void testGtBetweenExclusive() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [3, 2, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [3, 1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [3, 4, 1]}", null));
  }

  @Test
  public void testGtBetweenInclusive() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 3, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 2, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [3, 0, 1]}", null));
  }

  @Test
  public void testEdgeCases() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 1, 1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [3, 1, 3, 1]}", null));
  }

  // ==================== VARIABLE COMPARISONS ====================

  @Test
  public void testLessThanWithVariables() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("a", 1);
    data.put("b", 2);
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [{\"var\":\"a\"}, {\"var\":\"b\"}]}", data));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [{\"var\":\"b\"}, {\"var\":\"a\"}]}", data));
  }

  @Test
  public void testGreaterThanOrEqualWithVariables() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("score", 85);
    data.put("passing", 70);
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [{\"var\":\"score\"}, {\"var\":\"passing\"}]}", data));
  }

  @Test
  public void testBetweenWithVariables() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("min", 0);
    data.put("value", 50);
    data.put("max", 100);
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [{\"var\":\"min\"}, {\"var\":\"value\"}, {\"var\":\"max\"}]}", data));
  }

  @Test
  public void testVariableVsLiteral() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("x", 5);
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"x\"}, 3]}", data));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [{\"var\":\"x\"}, 10]}", data));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"x\"}, 10]}", data));
  }

  // ==================== NULL HANDLING ====================

  @Test
  public void testNullUsesJavascriptNumericCoercion() throws JsonLogicException {
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [null, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1, null]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [null, null]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [null, null]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [null, null]}", null));
  }

  @Test
  public void testNullVariableUsesJavascriptNumericCoercion() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("missing", null);
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"missing\"}, 5]}", data));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [5, {\"var\":\"missing\"}]}", data));
  }

  @Test
  public void testMissingVariableReturnsFalse() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"nonexistent\"}, 5]}", data));
  }

  @Test
  public void testNullInBetweenUsesJavascriptNumericCoercion() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [null, 5, 10]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, null, 10]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 5, null]}", null));
  }

  // ==================== STRING HANDLING ====================

  @Test
  public void testNumericStrings() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"10\", \"5\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [\"5\", \"10\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [\"5\", \"5\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [\"5\", \"5\"]}", null));
  }

  @Test
  public void testDecimalStrings() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"1.5\", \"1.4\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [\"1.4\", \"1.5\"]}", null));
  }

  @Test
  public void testNegativeNumberStrings() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"-1\", \"-2\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [\"-2\", \"-1\"]}", null));
  }

  @Test
  public void testInvalidStringsReturnFalse() throws JsonLogicException {
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [\"abc\", 5]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [5, \"abc\"]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [\"notanumber\", \"10\"]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [\"\", 5]}", null));
  }

  @Test
  public void testBooleanUsesJavascriptNumericCoercion() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [true, false]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [false, false]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [false, null]}", null));
  }

  @Test
  public void testMixedStringAndNumber() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"10\", 5]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [5, \"10\"]}", null));
  }

  @Test
  public void testStringVariableComparison() throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("a", "10");
    data.put("b", 5);
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"a\"}, {\"var\":\"b\"}]}", data));
  }

  // ==================== BOOLEAN AND OTHER TYPES ====================

  @Test
  public void testBooleanReturnsFalse() throws JsonLogicException {
    Object result = jsonLogic.apply("{\">\" : [true, false]}", null);
    assertNotNull(result);
  }

  @Test
  public void testArrayReturnsFalse() throws JsonLogicException {
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [[1, 2], 5]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [5, [1, 2]]}", null));
  }

  // ==================== EDGE CASES ====================

  @Test
  public void testZeroComparisons() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [0, 0]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1, 0]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [0, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [0, 0]}", null));
  }

  @Test
  public void testVeryLargeNumbers() throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1e10, 1e9]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1e-10, 1e-9]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1.7976931348623157e308, 1.0]}", null));
  }
}
