package io.github.jamsesso.jsonlogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class JsonLogicDefaultCompilerTest {
  @Test
  public void shouldEnableCompilationWithMainArtifact() throws JsonLogicException {
    final var jsonLogic = new JsonLogic();

    assertTrue(jsonLogic.isCompilationEnabled());
    assertEquals(20.0, jsonLogic.apply("{\"*\":[{\"var\":\"x\"},2]}", Map.of("x", 10)));
  }
}
