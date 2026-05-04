package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumericComparisonExpressionTests {

  // ==================== NUMBER COMPARISONS ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareLessThanWithNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1, 2]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [2, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareLessThanOrEqualWithNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 2]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<=\" : [2, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareGreaterThanWithNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [2, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [1, 2]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [1, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareGreaterThanOrEqualWithNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [2, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [1, 2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareDecimalNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1.5, 1.4]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1.4, 1.5]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [1.5, 1.5]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1.5, 1.5]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareNegativeNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [0, -1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [-1, -2]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [-2, -1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [-1, 0]}", null));
  }

  // ==================== BETWEEN COMPARISONS (3 ARGUMENTS) ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSupportExclusiveBetween(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1, 2, 3]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 1, 3]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 4, 3]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSupportInclusiveBetween(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 1, 3]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 2, 3]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [1, 3, 3]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<=\" : [1, 4, 3]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSupportGreaterThanExclusiveBetween(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [3, 2, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [3, 1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [3, 4, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSupportGreaterThanOrEqualInclusiveBetween(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 3, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 2, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [3, 0, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldHandleEdgeCasesWithMoreThanThreeArguments(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [3, 1, 1, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [3, 1, 3, 1]}", null));
  }

  // ==================== VARIABLE COMPARISONS ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareLessThanWithVariables(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("a", 1);
    data.put("b", 2);
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [{\"var\":\"a\"}, {\"var\":\"b\"}]}", data));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [{\"var\":\"b\"}, {\"var\":\"a\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareGreaterThanOrEqualWithVariables(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("score", 85);
    data.put("passing", 70);
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [{\"var\":\"score\"}, {\"var\":\"passing\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSupportBetweenWithVariables(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("min", 0);
    data.put("value", 50);
    data.put("max", 100);
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [{\"var\":\"min\"}, {\"var\":\"value\"}, {\"var\":\"max\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareVariableVsLiteral(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("x", 5);
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"x\"}, 3]}", data));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [{\"var\":\"x\"}, 10]}", data));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"x\"}, 10]}", data));
  }

  // ==================== NULL HANDLING ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldUseJavascriptNumericCoercionForNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [null, 1]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1, null]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [null, null]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [null, null]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [null, null]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldUseJavascriptNumericCoercionForNullVariable(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("missing", null);
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"missing\"}, 5]}", data));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [5, {\"var\":\"missing\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnFalseForMissingVariable(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"nonexistent\"}, 5]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldUseJavascriptNumericCoercionForNullInBetween(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [null, 5, 10]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, null, 10]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [1, 5, null]}", null));
  }

  // ==================== STRING HANDLING ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareNumericStrings(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"10\", \"5\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [\"5\", \"10\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [\"5\", \"5\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [\"5\", \"5\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareDecimalStrings(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"1.5\", \"1.4\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [\"1.4\", \"1.5\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareNegativeNumberStrings(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"-1\", \"-2\"]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [\"-2\", \"-1\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnFalseForInvalidStrings(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [\"abc\", 5]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [5, \"abc\"]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [\"notanumber\", \"10\"]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">=\" : [\"\", 5]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldUseJavascriptNumericCoercionForBooleans(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [true, false]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [false, false]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<=\" : [false, null]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareMixedStringAndNumber(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [\"10\", 5]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [5, \"10\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCompareStringVariable(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<>();
    data.put("a", "10");
    data.put("b", 5);
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [{\"var\":\"a\"}, {\"var\":\"b\"}]}", data));
  }

  // ==================== BOOLEAN AND OTHER TYPES ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNonNullResultForBooleanComparison(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Object result = jsonLogic.apply("{\">\" : [true, false]}", null);
    assertNotNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnFalseForArrayComparison(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [[1, 2], 5]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\">\" : [5, [1, 2]]}", null));
  }

  // ==================== EDGE CASES ====================

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldHandleZeroComparisons(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">=\" : [0, 0]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1, 0]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [0, 1]}", null));
    assertFalse((Boolean) jsonLogic.apply("{\"<\" : [0, 0]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldHandleVeryLargeNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1e10, 1e9]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\"<\" : [1e-10, 1e-9]}", null));
    assertTrue((Boolean) jsonLogic.apply("{\">\" : [1.7976931348623157e308, 1.0]}", null));
  }
}
