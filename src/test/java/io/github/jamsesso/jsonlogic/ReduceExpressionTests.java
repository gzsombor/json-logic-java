package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReduceExpressionTests {

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testReduce(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"reduce\":[\n" +
                  "    {\"var\":\"\"},\n" +
                  "    {\"+\":[{\"var\":\"current\"}, {\"var\":\"accumulator\"}]},\n" +
                  "    0\n" +
                  "]}";
    int[] data = new int[] {1, 2, 3, 4, 5, 6};
    Object result = jsonLogic.apply(json, data);

    assertEquals(21.0, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldResolveOuterDataVariablesInReducer(String label, JsonLogic jsonLogic)
      throws JsonLogicException {
    String json = "{\n" +
            "  \"reduce\": [\n" +
            "    {\n" +
            "      \"var\": \"data\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"*\": [\n" +
            "        {\n" +
            "          \"var\": \"multiplicator\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"+\": [\n" +
            "            {\n" +
            "              \"var\": \"current\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"var\": \"accumulator\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    0\n" +
            "  ]\n" +
            "}";
    final Map<String, Object> data = new HashMap<>();
    data.put("multiplicator", 2);
    data.put("data", new int[] {1, 2});

    Object result = jsonLogic.apply(json, data);

    assertEquals(8.0, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReduceStaticNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String json = "{\"reduce\":[[1,2,3],{\"+\":[{\"var\":\"current\"},{\"var\":\"accumulator\"}]},10]}";

    Object result = jsonLogic.apply(json, null);

    assertEquals(16.0, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnInitialAccumulatorWhenInputIsNull(String label, JsonLogic jsonLogic)
      throws JsonLogicException {
    String json = "{\"reduce\":[{\"var\":\"items\"},{\"+\":[{\"var\":\"current\"},{\"var\":\"accumulator\"}]},7]}";
    final Map<String, Object> data = new HashMap<>();
    data.put("items", null);

    Object result = jsonLogic.apply(json, data);

    assertEquals(7.0, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldResolveVariableInitialAccumulator(String label, JsonLogic jsonLogic)
      throws JsonLogicException {
    String json = "{\"reduce\":[[1,2],{\"+\":[{\"var\":\"current\"},{\"var\":\"accumulator\"}]},{\"var\":\"myVar\"}]}";
    final Map<String, Object> data = new HashMap<>();
    data.put("myVar", 100);

    Object result = jsonLogic.apply(json, data);

    assertEquals(103.0, result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenReducerCurrentValueIsNull(String label, JsonLogic jsonLogic)
      throws JsonLogicException {
    String json = "{\"reduce\":[[1,null,2],{\"+\":[{\"var\":\"current\"},{\"var\":\"accumulator\"}]},0]}";

    Object result = jsonLogic.apply(json, null);

    assertEquals(null, result);
  }
}
