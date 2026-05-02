package io.github.jamsesso.jsonlogic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MissingExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final String label;
  private final JsonLogic jsonLogic;

  public MissingExpressionTests(String label, JsonLogic jsonLogic) {
    this.label = label;
    this.jsonLogic = jsonLogic;
  }

  @Test
  public void testMissing() throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("a", "apple");
      put("c", "carrot");
    }};
    Object result = jsonLogic.apply("{\"missing\": [\"a\", \"b\"]}", data);

    assertEquals(1, ((List) result).size());
    assertEquals("b", ((List) result).get(0));
  }

  @Test
  public void testMissingSomeUnderThreshold() throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("a", "apple");
      put("c", "carrot");
    }};
    Object result = jsonLogic.apply("{\"missing_some\": [1, [\"a\", \"b\"]]}", data);

    assertEquals(0, ((List) result).size());
  }

  @Test
  public void testMissingSomeOverThreshold() throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("a", "apple");
    }};
    Object result = jsonLogic.apply("{\"missing_some\": [2, [\"a\", \"b\", \"c\"]]}", data);

    assertEquals(2, ((List) result).size());
    assertEquals("b", ((List) result).get(0));
    assertEquals("c", ((List) result).get(1));
  }

  @Test
  public void testMissingSomeComplexExpression() throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("first_name", "Bruce");
      put("last_name", "Wayne");
    }};
    // JSON: {"if":[{"merge": [{"missing":["first_name", "last_name"]}, {"missing_some": [1, ["cell_phone", "home_phone"]]}]}, "We require...", "OK to proceed"]}
    String json = "{\"if\" :[\n" +
                  "  {\"merge\": [\n" +
                  "    {\"missing\":[\"first_name\", \"last_name\"]},\n" +
                  "    {\"missing_some\": [1, [\"cell_phone\", \"home_phone\"]]}\n" +
                  "  ]},\n" +
                  "  \"We require first name, last name, and one phone number.\",\n" +
                  "  \"OK to proceed\"\n" +
                  "]}";
    Object result = jsonLogic.apply(json, data);

    assertEquals("We require first name, last name, and one phone number.", result);
  }

  @Test
  public void testMissingSomeWithNullData() throws JsonLogicException {
    Object result = jsonLogic.apply("{\"missing_some\": [2, [\"a\", \"b\", \"c\"]]}", null);

    assertEquals(3, ((List) result).size());
    assertEquals("a", ((List) result).get(0));
    assertEquals("b", ((List) result).get(1));
    assertEquals("c", ((List) result).get(2));
  }

  @Test
  public void testMissingSomeWithZeroThreshold() throws JsonLogicException {
    Object result = jsonLogic.apply("{\"missing_some\": [0, [\"a\", \"b\", \"c\"]]}", null);

    assertEquals(0, ((List) result).size());
  }
}
