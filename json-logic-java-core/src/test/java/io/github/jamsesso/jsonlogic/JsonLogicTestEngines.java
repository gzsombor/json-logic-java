package io.github.jamsesso.jsonlogic;

import java.util.stream.Stream;

public final class JsonLogicTestEngines {
  private JsonLogicTestEngines() {}

  public static Stream<Object[]> engines() {
    return Stream.<Object[]>of(new Object[]{"interpreter", new JsonLogic(false)});
  }
}
