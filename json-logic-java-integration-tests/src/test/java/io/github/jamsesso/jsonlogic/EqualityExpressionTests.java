package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EqualityExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSameValueSameType(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"==\": [1, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSameValueDifferentType(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"==\": [1, 1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDifferentValueDifferentType(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"==\": [[], false]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testEmptyStringAndZeroComparison(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"==\": [\" \", 0]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInvalidArgumentCountEquality(String name, JsonLogic jsonLogic) {
    String json = "{\"==\": [1]}";
    String expectedErrorJsonPath = "$.==";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInvalidEquality(String name, JsonLogic jsonLogic) {
    String json = "{\"==\": [{}, true]}";
    String expectedErrorJsonPath = "$.==[0]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }
}
