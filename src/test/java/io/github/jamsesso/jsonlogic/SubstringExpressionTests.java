package io.github.jamsesso.jsonlogic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SubstringExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public SubstringExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  @Test
  public void shouldReturnSuffixFromPositiveStart() throws JsonLogicException {
    assertEquals("logic", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 4]}", null));
  }

  @Test
  public void shouldReturnSuffixFromNegativeStart() throws JsonLogicException {
    assertEquals("logic", jsonLogic.apply("{\"substr\": [\"jsonlogic\", -5]}", null));
  }

  @Test
  public void shouldReturnSubstringWithPositiveLength() throws JsonLogicException {
    assertEquals("son", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 1, 3]}", null));
  }

  @Test
  public void shouldReturnSubstringWithNegativeLength() throws JsonLogicException {
    assertEquals("log", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 4, -2]}", null));
  }

  @Test
  public void shouldReturnEmptyStringWhenNegativeStartExceedsLength() throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", -40]}", null));
  }

  @Test
  public void shouldReturnEmptyStringWhenStartExceedsLength() throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 20, -40]}", null));
  }

  @Test
  public void shouldReturnFullStringWhenStartIsZero() throws JsonLogicException {
    assertEquals("jsonlogic", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 0]}", null));
  }

  @Test
  public void shouldReturnEmptyStringWhenLengthIsZero() throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 2, 0]}", null));
  }

  @Test
  public void shouldReturnLastCharWithNegativeStartOfMinusOne() throws JsonLogicException {
    assertEquals("c", jsonLogic.apply("{\"substr\": [\"jsonlogic\", -1]}", null));
  }

  @Test
  public void shouldReturnSubstringFromVar() throws JsonLogicException {
    assertEquals("world", jsonLogic.apply(
        "{\"substr\": [{\"var\": \"s\"}, 6]}", Map.of("s", "hello world")));
  }

  @Test
  public void shouldReturnSubstringFromVarWithLength() throws JsonLogicException {
    assertEquals("hello", jsonLogic.apply(
        "{\"substr\": [{\"var\": \"s\"}, 0, 5]}", Map.of("s", "hello world")));
  }

  @Test
  public void shouldReturnEmptyStringWhenSubstrStartPositiveExceedsLength() throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"substr\": [\"jsonlogic\", 20, -1]}", null));
  }

  @Test
  public void shouldThrowWhenArgCountTooFew() {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\"]}", "$.substr");
  }

  @Test
  public void shouldThrowWhenArgCountTooMany() {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\", 1, 2, 3]}", "$.substr");
  }

  @Test
  public void shouldThrowWhenStartArgIsNotANumber() {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\", \"a\", 3]}", "$.substr[1]");
  }

  @Test
  public void shouldThrowWhenLengthArgIsNotANumber() {
    testErrorJsonPath(jsonLogic, "{\"substr\": [\"jsonlogic\", 1, \"a\"]}", "$.substr[2]");
  }
}
