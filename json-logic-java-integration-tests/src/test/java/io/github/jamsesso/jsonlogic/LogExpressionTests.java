package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDoesLog(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("hello world", jsonLogic.apply("{\"log\": \"hello world\"}", null));
  }
}
