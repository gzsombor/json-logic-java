package io.github.jamsesso.jsonlogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class JsonLogicExceptionTestUtility {
  private JsonLogicExceptionTestUtility() {
  }

  public static void testErrorJsonPath(JsonLogic jsonLogic, String json, String expectedErrorJsonPath) {
    try {
      Object result = jsonLogic.apply(json, null);
      fail("Expected JsonLogicException, but got result: " + result);
    } catch (JsonLogicException e) {
      String path = e.getJsonPath();
      assertEquals(expectedErrorJsonPath, path);
    }
  }
}
