package io.github.jamsesso.jsonlogic;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubstringExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSuffixFromPositiveStart(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("logic", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 4]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSuffixFromNegativeStart(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("logic", jsonLogic.apply("{\"substr\": [\"jsonlogic\", -5]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSubstringWithPositiveLength(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("son", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 1, 3]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSubstringWithNegativeLength(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("log", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 4, -2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnEmptyStringWhenNegativeStartExceedsLength(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", -40]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnEmptyStringWhenStartExceedsLength(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 20, -40]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnFullStringWhenStartIsZero(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("jsonlogic", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 0]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnEmptyStringWhenLengthIsZero(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 2, 0]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnLastCharWithNegativeStartOfMinusOne(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("c", jsonLogic.apply("{\"substr\": [\"jsonlogic\", -1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSubstringFromVar(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("world", jsonLogic.apply(
        "{\"substr\": [{\"var\": \"s\"}, 6]}", Map.of("s", "hello world")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnSubstringFromVarWithLength(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("hello", jsonLogic.apply(
        "{\"substr\": [{\"var\": \"s\"}, 0, 5]}", Map.of("s", "hello world")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnEmptyStringWhenSubstrStartPositiveExceedsLength(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 20, -1]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldThrowWhenArgCountTooFew(String label, JsonLogic jsonLogic) {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\"]}", "$.substr");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldThrowWhenArgCountTooMany(String label, JsonLogic jsonLogic) {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\", 1, 2, 3]}", "$.substr");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldThrowWhenStartArgIsNotANumber(String label, JsonLogic jsonLogic) {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\", \"a\", 3]}", "$.substr[1]");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldThrowWhenLengthArgIsNotANumber(String label, JsonLogic jsonLogic) {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\", 1, \"a\"]}", "$.substr[2]");
  }
}
