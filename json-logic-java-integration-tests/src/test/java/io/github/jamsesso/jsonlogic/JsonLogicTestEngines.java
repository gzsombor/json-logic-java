package io.github.jamsesso.jsonlogic;

import java.util.stream.Stream;

public final class JsonLogicTestEngines {
  private JsonLogicTestEngines() {}

  public static Stream<Object[]> engines() {
    return Stream.of(
        new Object[]{"interpreter", interpreter()},
        new Object[]{"compiled", compiled()});
  }

  public static JsonLogic interpreter() {
    return new JsonLogic(false);
  }

  public static JsonLogic compiled() {
    return new JsonLogic(true, true);
  }
}
