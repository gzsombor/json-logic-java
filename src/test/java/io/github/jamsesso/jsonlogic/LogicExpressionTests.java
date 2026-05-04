package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;

public class LogicExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testOr() throws JsonLogicException {
    assertEquals("a", jsonLogic.apply("{\"or\": [0, false, \"a\"]}", null));
  }

  @Test
  public void testAnd() throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"and\": [true, \"\", 3]}", null));
  }

  @Test
  public void testInvalidLogicExpression() {
    String json = "{\"or\": [0, {}, \"a\"]}";
    // -----------------------  ^  ----------
    String expectedErrorJsonPath = "$.or[1]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }
}
