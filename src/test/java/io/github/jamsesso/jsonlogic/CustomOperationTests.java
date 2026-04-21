package io.github.jamsesso.jsonlogic;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Year;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@RunWith(Parameterized.class)
public class CustomOperationTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> modes() {
    return Arrays.asList(
        new Object[]{"interpreter", false},
        new Object[]{"compiled",    true}
    );
  }

  private final boolean compilationEnabled;

  public CustomOperationTests(String name, boolean compilationEnabled) {
    this.compilationEnabled = compilationEnabled;
  }

  private JsonLogic newJsonLogic() {
    return new JsonLogic(compilationEnabled);
  }

  // ---- basic custom operator registration ----

  @Test
  public void greet() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("greet", args -> String.format("Hello %s!", args[0]));
    Assert.assertEquals("Hello json-logic!", jsonLogic.apply("{\"greet\": [\"json-logic\"]}", null));
  }

  @Test
  public void greetWithUppercaseName() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("Greet", args -> String.format("Hello %s!", args[0]));
    Assert.assertEquals("Hello json-logic!", jsonLogic.apply("{\"Greet\": [\"json-logic\"]}", null));
  }

  // ---- isUpperCase - predicate operator that inspects its argument ----

  @Test
  public void isUpperCase_trueForAllCaps() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("isUpperCase", args -> {
      final String value = (String) args[0];
      return value.equals(value.toUpperCase());
    });
    Assert.assertEquals(Boolean.TRUE, jsonLogic.apply("{\"isUpperCase\":[\"HELLO\"]}", null));
  }

  @Test
  public void isUpperCase_falseForMixedCase() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("isUpperCase", args -> {
      final String value = (String) args[0];
      return value.equals(value.toUpperCase());
    });
    Assert.assertEquals(Boolean.FALSE, jsonLogic.apply("{\"isUpperCase\":[\"Hello\"]}", null));
  }

  @Test
  public void isUpperCase_usedInsideIf() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("isUpperCase", args -> {
      final String value = (String) args[0];
      return value.equals(value.toUpperCase());
    });
    final String rule = "{\"if\":[{\"isUpperCase\":[{\"var\":\"name\"}]},\"shouting\",\"normal\"]}";
    Assert.assertEquals("shouting", jsonLogic.apply(rule, Map.of("name", "ALICE")));
    Assert.assertEquals("normal",   jsonLogic.apply(rule, Map.of("name", "Alice")));
  }

  // ---- currentYear - zero-argument expression ----

  @Test
  public void currentYear_returnsCurrentYear() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("currentYear", args -> (double) Year.now().getValue());
    Assert.assertEquals(
        (double) Year.now().getValue(),
        jsonLogic.apply("{\"currentYear\":[]}", null));
  }

  @Test
  public void currentYear_usedInsideComparison() throws JsonLogicException {
    final var jsonLogic = newJsonLogic();
    jsonLogic.addOperation("currentYear", args -> (double) Year.now().getValue());
    final String rule = "{\"==\":[{\"var\":\"year\"},{\"currentYear\":[]}]}";
    final double thisYear = Year.now().getValue();
    Assert.assertEquals(Boolean.TRUE,  jsonLogic.apply(rule, Map.of("year", thisYear)));
    Assert.assertEquals(Boolean.FALSE, jsonLogic.apply(rule, Map.of("year", thisYear - 1)));
  }

  // ---- cache invalidation: operator registered after enableCompilation ----

  @Test
  public void operationRegisteredAfterConstruction_isPickedUp() throws JsonLogicException {
    Assume.assumeTrue("only relevant in compiled mode", compilationEnabled);
    final var jsonLogic = new JsonLogic();
    jsonLogic.addOperation("double", args -> ((Number) args[0]).doubleValue() * 2);
    Assert.assertEquals(10.0, jsonLogic.apply("{\"double\":[5]}", null));
  }
}
