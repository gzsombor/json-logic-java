package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AllExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testEmptyArray(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"all\": [[], {\">\": [{\"var\": \"\"}, 0]}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testAll(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 0]}]}", null));
    assertEquals(false, jsonLogic.apply("{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 1]}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInvalidAll(String name, JsonLogic jsonLogic) {
    String json = "{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, {}]}]}";
    // ----------------------------------------------------------  ^  ----
    String expectedErrorJsonPath = "$.all[1].>[1]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }
}
