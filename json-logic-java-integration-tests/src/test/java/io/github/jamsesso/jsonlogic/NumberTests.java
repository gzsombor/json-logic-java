package io.github.jamsesso.jsonlogic;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumberTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testConvertAllNumericInputToDouble(String name, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Number> numbers = new HashMap<String, Number>() {{
      put("double", 1D);
      put("float", 1F);
      put("int", 1);
      put("short", (short) 1);
      put("long", 1L);
    }};

    assertEquals(1D, jsonLogic.apply("{\"var\": \"double\"}", numbers));
    assertEquals(1D, jsonLogic.apply("{\"var\": \"float\"}", numbers));
    assertEquals(1D, jsonLogic.apply("{\"var\": \"int\"}", numbers));
    assertEquals(1D, jsonLogic.apply("{\"var\": \"short\"}", numbers));
    assertEquals(1D, jsonLogic.apply("{\"var\": \"long\"}", numbers));
  }
}
