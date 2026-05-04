package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MissingExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMissing(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("a", "apple");
      put("c", "carrot");
    }};
    Object result = jsonLogic.apply("{\"missing\": [\"a\", \"b\"]}", data);

    assertEquals(1, ((List) result).size());
    assertEquals("b", ((List) result).get(0));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMissingSomeUnderThreshold(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("a", "apple");
      put("c", "carrot");
    }};
    Object result = jsonLogic.apply("{\"missing_some\": [1, [\"a\", \"b\"]]}", data);

    assertEquals(0, ((List) result).size());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMissingSomeOverThreshold(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("a", "apple");
    }};
    Object result = jsonLogic.apply("{\"missing_some\": [2, [\"a\", \"b\", \"c\"]]}", data);

    assertEquals(2, ((List) result).size());
    assertEquals("b", ((List) result).get(0));
    assertEquals("c", ((List) result).get(1));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMissingSomeComplexExpression(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("first_name", "Bruce");
      put("last_name", "Wayne");
    }};
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

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMissingSomeWithNullData(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Object result = jsonLogic.apply("{\"missing_some\": [2, [\"a\", \"b\", \"c\"]]}", null);

    assertEquals(3, ((List) result).size());
    assertEquals("a", ((List) result).get(0));
    assertEquals("b", ((List) result).get(1));
    assertEquals("c", ((List) result).get(2));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMissingSomeWithZeroThreshold(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Object result = jsonLogic.apply("{\"missing_some\": [0, [\"a\", \"b\", \"c\"]]}", null);

    assertEquals(0, ((List) result).size());
  }
}
