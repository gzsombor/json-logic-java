package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayHasExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSomeWithNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"and\":[{\"some\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"\"},[\"apple\"]]}]}]}", "{\"fruits\":null}"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSomeEmptyArray(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"\"}, 0]}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSomeAll(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 3]}]}", null));
    assertEquals(true, jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 1]}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testNoneWithNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"and\":[{\"none\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"\"},[\"apple\"]]}]}]}", "{\"fruits\":null}"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testNoneEmptyArray(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"\"}, 0]}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testNoneAll(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 3]}]}", null));
    assertEquals(false, jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 2]}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInvalidArrayHasExpression(String label, JsonLogic jsonLogic) {
    String json = "{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, {}]}]}";
    String expectedErrorJsonPath = "$.some[1].>[1]";
    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStrictEqualityIdenticalArrays(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"===\": [[1, 2, 3], [1, 2, 3]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStrictEqualityEmptyArrays(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"===\": [[], []]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStrictEqualityDifferentElements(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"===\": [[1, 2], [1, 3]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStrictEqualityLeftLonger(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"===\": [[1, 2, 3], [1, 2]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStrictEqualityRightLonger(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"===\": [[1, 2], [1, 2, 3]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testLooseEqualityIdenticalArrays(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2, 3], [1, 2, 3]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testLooseEqualityEmptyArrays(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"==\": [[], []]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testLooseEqualityDifferentElements(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2], [1, 3]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testLooseEqualityLeftLonger(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2, 3], [1, 2]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testLooseEqualityRightLonger(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2], [1, 2, 3]]}", null));
  }
}
