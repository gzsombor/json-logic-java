package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MergeExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMerge(String name, JsonLogic jsonLogic) throws JsonLogicException {
    Object result = jsonLogic.apply("{\"merge\": [[1, 2], [3, 4]]}", null);

    assertEquals(4, ((List) result).size());
    assertEquals(1.0, ((List) result).get(0));
    assertEquals(2.0, ((List) result).get(1));
    assertEquals(3.0, ((List) result).get(2));
    assertEquals(4.0, ((List) result).get(3));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMergeWithNonArrays(String name, JsonLogic jsonLogic) throws JsonLogicException {
    Object result = jsonLogic.apply("{\"merge\": [1, 2, [3, 4]]}", null);

    assertEquals(4, ((List) result).size());
    assertEquals(1.0, ((List) result).get(0));
    assertEquals(2.0, ((List) result).get(1));
    assertEquals(3.0, ((List) result).get(2));
    assertEquals(4.0, ((List) result).get(3));
  }
}
