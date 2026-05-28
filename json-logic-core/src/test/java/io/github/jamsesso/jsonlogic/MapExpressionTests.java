package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapExpressionTests {

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMap(String name, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"map\": [\n" +
                  "  {\"var\": \"\"},\n" +
                  "  {\"*\": [{\"var\": \"\"}, 2]}\n" +
                  "]}";
    int[] data = new int[] {1, 2, 3};
    Object result = jsonLogic.apply(json, data);

    assertEquals(3, ((List) result).size());
    assertEquals(2.0, ((List) result).get(0));
    assertEquals(4.0, ((List) result).get(1));
    assertEquals(6.0, ((List) result).get(2));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMapWithObjectItems(String name, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"map\": [\n" +
                  "  {\"var\": \"users\"},\n" +
                  "  {\"cat\": [{\"var\": \"name\"}, \" hello\"]}\n" +
                  "]}";
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("users", List.of(
        Collections.singletonMap("name", "Bob"),
        Collections.singletonMap("name", "Joe")));
    Object result = jsonLogic.apply(json, data);

    assertEquals(List.of("Bob hello", "Joe hello"), result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMapCurrentItemVar(String name, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"map\": [\n" +
                  "  {\"var\": \"\"},\n" +
                  "  {\"cat\": [{\"var\": \"\"}, \" x\"]}\n" +
                  "]}";
    Object result = jsonLogic.apply(json, List.of("a", "b", "c"));

    assertEquals(List.of("a x", "b x", "c x"), result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInvalidMap(String name, JsonLogic jsonLogic) {
    String json =  "{\"map\": [\n" +
        "  {\"var\": \"\"},\n" +
        "  {\"*\": [{}, 2]}\n" +
        // -------  ^  ---------
        "]}";

    String expectedErrorJsonPath = "$.map[1].*[0]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }
}
