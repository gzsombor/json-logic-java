package io.github.jamsesso.jsonlogic;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IfExpressionTests {
  static Stream<Object[]> enginesAndOperators() {
    return JsonLogicTestEngines.engines()
        .flatMap(engine -> Stream.of(
            new Object[]{engine[0] + " if", engine[1], "if"},
            new Object[]{engine[0] + " ?:", engine[1], "?:"}
        ));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfTrue(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[true, \"yes\", \"no\"]");
    Object result = jsonLogic.apply(json, null);
    assertEquals("yes", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfFalse(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[false, \"yes\", \"no\"]");
    Object result = jsonLogic.apply(json, null);
    assertEquals("no", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfTwoParametersWithTruthyString(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[\"condition\", \"yes\"]");
    Object result = jsonLogic.apply(json, null);
    assertEquals("yes", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfTwoParametersWithEmptyString(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[\"\", \"yes\"]");
    Object result = jsonLogic.apply(json, null);
    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfTwoParametersWithTrue(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[true, \"yes\"]");
    Object result = jsonLogic.apply(json, null);
    assertEquals("yes", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfTwoParametersWithFalse(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[false, \"yes\"]");
    Object result = jsonLogic.apply(json, null);
    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfTwoParametersWithNull(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[null, \"yes\"]");
    Object result = jsonLogic.apply(json, null);
    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfElseIfElse(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[\n" +
                                 "  {\"<\": [50, 0]}, \"freezing\",\n" +
                                 "  {\"<\": [50, 100]}, \"liquid\",\n" +
                                 "  \"gas\"\n" +
                                 "]");
    Object result = jsonLogic.apply(json, null);
    assertEquals("liquid", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfElseJsonPath_pos0(String label, JsonLogic jsonLogic, String operator) {
    String json = rule(operator, "[{}, \"yes\", \"no\"]");
    String expectedErrorJsonPath = "$." + operator + "[0]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfElseJsonPath_pos1(String label, JsonLogic jsonLogic, String operator) {
    String json = rule(operator, "[true, {}, \"no\"]");
    String expectedErrorJsonPath = "$." + operator + "[1]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfElseJsonPath_pos2(String label, JsonLogic jsonLogic, String operator) {
    String json = rule(operator, "[false, \"yes\", {}]");
    String expectedErrorJsonPath = "$." + operator + "[2]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfEmptyArray(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[]");
    Object result = jsonLogic.apply(json, null);
    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfSingleValueTrue(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[true]");
    Object result = jsonLogic.apply(json, null);
    assertEquals(Boolean.TRUE, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("enginesAndOperators")
  public void testIfSingleValueFalse(String label, JsonLogic jsonLogic, String operator)
      throws JsonLogicException {
    String json = rule(operator, "[false]");
    Object result = jsonLogic.apply(json, null);
    assertEquals(Boolean.FALSE, result);
  }

  private static String rule(String operator, String arguments) {
    return "{\"" + operator + "\": " + arguments + "}";
  }
}
