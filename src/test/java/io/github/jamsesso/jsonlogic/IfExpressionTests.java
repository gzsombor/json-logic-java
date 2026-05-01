package io.github.jamsesso.jsonlogic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class IfExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public IfExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  @Test
  public void testIfTrue() throws JsonLogicException {
    String json = "{\"if\": [true, \"yes\", \"no\"]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals("yes", result);
  }

  @Test
  public void testIfFalse() throws JsonLogicException {
    String json = "{\"if\": [false, \"yes\", \"no\"]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals("no", result);
  }

  @Test
  public void testIfElseIfElse() throws JsonLogicException {
    String json = "{\"if\": [\n" +
                  "  {\"<\": [50, 0]}, \"freezing\",\n" +
                  "  {\"<\": [50, 100]}, \"liquid\",\n" +
                  "  \"gas\"\n" +
                  "]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals("liquid", result);
  }

  @Test
  public void testIfElseJsonPath_pos0() {
    String json = "{\"if\": [{}, \"yes\", \"no\"]}";
    // ---------------------  ^  --------------------
    String expectedErrorJsonPath = "$.if[0]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @Test
  public void testIfElseJsonPath_pos1() {
    String json = "{\"if\": [true, {}, \"no\"]}";
    // ---------------------------  ^  -----------
    String expectedErrorJsonPath = "$.if[1]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @Test
  public void testIfElseJsonPath_pos2() {
    String json = "{\"if\": [false, \"yes\", {}]}";
    // -------------------------------------  ^  ---
    String expectedErrorJsonPath = "$.if[2]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  @Test
  public void testIfEmptyArray() throws JsonLogicException {
    // {"if": []} should return null (no branches, no default)
    String json = "{\"if\": []}";
    Object result = jsonLogic.apply(json, null);

    assertNull(result);
  }

  @Test
  public void testIfSingleValueTrue() throws JsonLogicException {
    // {"if": [true]} should return true (single truthy value)
    String json = "{\"if\": [true]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(Boolean.TRUE, result);
  }

  @Test
  public void testIfSingleValueFalse() throws JsonLogicException {
    // {"if": [false]} should return false (single falsy value)
    String json = "{\"if\": [false]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(Boolean.FALSE, result);
  }
}
