package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSingleBoolean(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"!\": false}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSingleNumber(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"!\": 0}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSingleString(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"!\": \"\"}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSingleArray(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"!\": []}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDoubleBoolean(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"!!\": false}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDoubleNumber(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"!!\": 0}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDoubleString(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"!!\": \"\"}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDoubleArray(String name, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"!!\": [[]]}", null));
  }
}
