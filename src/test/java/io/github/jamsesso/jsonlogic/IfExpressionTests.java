package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IfExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfTrue(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"if\": [true, \"yes\", \"no\"]}";
    Object result = jsonLogic.apply(json, null);
    assertEquals("yes", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfFalse(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"if\": [false, \"yes\", \"no\"]}";
    Object result = jsonLogic.apply(json, null);
    assertEquals("no", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfElseIfElse(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"if\": [\n" +
                  "  {\"<\": [50, 0]}, \"freezing\",\n" +
                  "  {\"<\": [50, 100]}, \"liquid\",\n" +
                  "  \"gas\"\n" +
                  "]}";
    Object result = jsonLogic.apply(json, null);
    assertEquals("liquid", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfElseJsonPath_pos0(String label, JsonLogic jsonLogic) {
    String json = "{\"if\": [{}, \"yes\", \"no\"]}";
    String expectedErrorJsonPath = "$.if[0]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfElseJsonPath_pos1(String label, JsonLogic jsonLogic) {
    String json = "{\"if\": [true, {}, \"no\"]}";
    String expectedErrorJsonPath = "$.if[1]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfElseJsonPath_pos2(String label, JsonLogic jsonLogic) {
    String json = "{\"if\": [false, \"yes\", {}]}";
    String expectedErrorJsonPath = "$.if[2]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfEmptyArray(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"if\": []}";
    Object result = jsonLogic.apply(json, null);
    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfSingleValueTrue(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"if\": [true]}";
    Object result = jsonLogic.apply(json, null);
    assertEquals(Boolean.TRUE, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testIfSingleValueFalse(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"if\": [false]}";
    Object result = jsonLogic.apply(json, null);
    assertEquals(Boolean.FALSE, result);
  }
}
