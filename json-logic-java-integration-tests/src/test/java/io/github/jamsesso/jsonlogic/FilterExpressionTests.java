package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testFilter(String name, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"filter\": [\n" +
                  "  {\"var\": \"\"},\n" +
                  "  {\"==\": [{\"%\": [{\"var\": \"\"}, 2]}, 0]}\n" +
                  "]}";
    int[] data = new int[] {1, 2, 3, 4, 5, 6};
    Object result = jsonLogic.apply(json, data);

    assertEquals(3, ((List) result).size());
    assertEquals(2.0, ((List) result).get(0));
    assertEquals(4.0, ((List) result).get(1));
    assertEquals(6.0, ((List) result).get(2));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInvalidFilter(String name, JsonLogic jsonLogic) {
    String json =  "{\"filter\": [\n" +
        "  {\"var\": {}},\n" +
        // --------  ^  ------
        "  {\"==\": [{\"%\": [{\"var\": \"\"}, 2]}, 0]}\n" +
        "]}";

    String expectedErrorJsonPath = "$.filter[0].var";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }
}
