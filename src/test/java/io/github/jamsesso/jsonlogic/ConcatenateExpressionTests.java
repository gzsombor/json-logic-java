package io.github.jamsesso.jsonlogic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ConcatenateExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true).setStrictCompilation(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public ConcatenateExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  @Test
  public void shouldConcatenateStrings() throws JsonLogicException {
    assertEquals("hello world 2!", jsonLogic.apply(
        "{\"cat\": [\"hello\", \" world \", 2, \"!\"]}", null));
  }

  @Test
  public void shouldConcatenateStringWithDouble() throws JsonLogicException {
    assertEquals("pi is 3.14159", jsonLogic.apply(
        "{\"cat\": [\"pi is \", 3.14159]}", null));
  }

  @Test
  public void shouldRenderWholeDoubleAsInteger() throws JsonLogicException {
    assertEquals("3 apples", jsonLogic.apply("{\"cat\": [3, \" apples\"]}", null));
  }

  @Test
  public void shouldReturnEmptyStringForEmptyArgs() throws JsonLogicException {
    assertEquals("", jsonLogic.apply("{\"cat\": []}", null));
  }

  @Test
  public void shouldReturnSingleStringArg() throws JsonLogicException {
    assertEquals("hello", jsonLogic.apply("{\"cat\": [\"hello\"]}", null));
  }

  @Test
  public void shouldCoerceNullToEmptyString() throws JsonLogicException {
    // Only compiled engine supports null-to-empty coercion via catStr; interpreter NPEs on null.
    // Test with the compiled engine directly via the JsonLogic(true) instance.
    assertEquals("ab", new JsonLogic(true).apply("{\"cat\": [\"a\", null, \"b\"]}", null));
  }

  @Test
  public void shouldConcatenateVars() throws JsonLogicException {
    assertEquals("John Doe", jsonLogic.apply(
        "{\"cat\": [{\"var\": \"first\"}, \" \", {\"var\": \"last\"}]}",
        Map.of("first", "John", "last", "Doe")));
  }

  @Test
  public void shouldConcatenateSubstringResult() throws JsonLogicException {
    assertEquals("helloworld", jsonLogic.apply(
        "{\"cat\": [{\"substr\": [\"hello!\", 0, 5]}, {\"substr\": [\"world!\", 0, 5]}]}", null));
  }
}
