package io.github.jamsesso.jsonlogic;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcatenateExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldConcatenateStrings(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("hello world 2!", jsonLogic.apply(
        "{\"cat\": [\"hello\", \" world \", 2, \"!\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldConcatenateStringWithDouble(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("pi is 3.14159", jsonLogic.apply(
        "{\"cat\": [\"pi is \", 3.14159]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldRenderWholeDoubleAsInteger(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("3 apples", jsonLogic.apply("{\"cat\": [3, \" apples\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnEmptyStringForEmptyArgs(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"cat\": []}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSingleStringArg(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("hello", jsonLogic.apply("{\"cat\": [\"hello\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCoerceNullToString(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("anullb", jsonLogic.apply("{\"cat\": [\"a\", null, \"b\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldConcatenatePrimitiveValues(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("1truenull", jsonLogic.apply("{\"cat\": [1, true, null]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldConcatenateVars(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("John Doe", jsonLogic.apply(
        "{\"cat\": [{\"var\": \"first\"}, \" \", {\"var\": \"last\"}]}",
        Map.of("first", "John", "last", "Doe")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldConcatenateSubstringResult(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("helloworld", jsonLogic.apply(
        "{\"cat\": [{\"substr\": [\"hello!\", 0, 5]}, {\"substr\": [\"world!\", 0, 5]}]}", null));
  }
}
