package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Year;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomOperationTests {
  // ---- basic custom operator registration ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void greet(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("greet", args -> String.format("Hello %s!", args[0]));
    assertEquals("Hello json-logic!", jsonLogic.apply("{\"greet\": [\"json-logic\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void greetWithUppercaseName(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("Greet", args -> String.format("Hello %s!", args[0]));
    assertEquals("Hello json-logic!", jsonLogic.apply("{\"Greet\": [\"json-logic\"]}", null));
  }

  // ---- isUpperCase ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void isUpperCase_trueForAllCaps(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("isUpperCase", args -> {
      final String value = (String) args[0];
      return value.equals(value.toUpperCase());
    });
    assertEquals(Boolean.TRUE, jsonLogic.apply("{\"isUpperCase\":[\"HELLO\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void isUpperCase_falseForMixedCase(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("isUpperCase", args -> {
      final String value = (String) args[0];
      return value.equals(value.toUpperCase());
    });
    assertEquals(Boolean.FALSE, jsonLogic.apply("{\"isUpperCase\":[\"Hello\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void isUpperCase_usedInsideIf(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("isUpperCase", args -> {
      final String value = (String) args[0];
      return value.equals(value.toUpperCase());
    });
    final String rule = "{\"if\":[{\"isUpperCase\":[{\"var\":\"name\"}]},\"shouting\",\"normal\"]}";
    assertEquals("shouting", jsonLogic.apply(rule, Map.of("name", "ALICE")));
    assertEquals("normal",   jsonLogic.apply(rule, Map.of("name", "Alice")));
  }

  // ---- currentYear ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void currentYear_returnsCurrentYear(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("currentYear", args -> (double) Year.now().getValue());
    assertEquals(
        (double) Year.now().getValue(),
        jsonLogic.apply("{\"currentYear\":[]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void currentYear_usedInsideComparison(String name, JsonLogic jsonLogic) throws JsonLogicException {
    jsonLogic.addOperation("currentYear", args -> (double) Year.now().getValue());
    final String rule = "{\"==\":[{\"var\":\"year\"},{\"currentYear\":[]}]}";
    final double thisYear = Year.now().getValue();
    assertEquals(Boolean.TRUE,  jsonLogic.apply(rule, Map.of("year", thisYear)));
    assertEquals(Boolean.FALSE, jsonLogic.apply(rule, Map.of("year", thisYear - 1)));
  }

  // ---- cache invalidation ----

  @Test
  public void operationRegisteredAfterConstruction_isPickedUp() throws JsonLogicException {
    final var jsonLogic = new JsonLogic();
    jsonLogic.addOperation("double", args -> ((Number) args[0]).doubleValue() * 2);
    assertEquals(10.0, jsonLogic.apply("{\"double\":[5]}", null));
  }
}
