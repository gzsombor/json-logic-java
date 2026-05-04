package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testDoesLog() throws JsonLogicException {
    assertEquals("hello world", jsonLogic.apply("{\"log\": \"hello world\"}", null));
  }
}
