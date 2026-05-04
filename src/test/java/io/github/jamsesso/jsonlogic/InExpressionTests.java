package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class InExpressionTests {
  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStringIn(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"in\": [\"race\", \"racecar\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testStringNotIn(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"in\": [\"race\", \"clouds\"]}", null));
    assertEquals(false, jsonLogic.apply("{\"in\": [null, \"clouds\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testArrayIn(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"in\": [1, [1, 2, 3]]}", null));
    assertEquals(true, jsonLogic.apply("{\"in\": [4.56, [1, 2, 3, 4.56]]}", null));
    assertEquals(true, jsonLogic.apply("{\"in\": [null, [1, 2, 3, null]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testArrayNotIn(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"in\": [5, [1, 2, 3]]}", null));
    assertEquals(false, jsonLogic.apply("{\"in\": [null, [1, 2, 3]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInVariableInt(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map data = Collections.singletonMap("list", Arrays.asList(1, 2, 3));
    assertEquals(true, jsonLogic.apply("{\"in\": [2, {\"var\": \"list\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testNotInVariableInt(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map data = Collections.singletonMap("list", Arrays.asList(1, 2, 3));
    assertEquals(false, jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", data));
    assertEquals(false, jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testAllVariables(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map data = Stream.of(new Object[][]{
        {"list", Arrays.asList(1, 2, 3)},
        {"value", 3}
    }).collect(Collectors.toMap(o -> o[0], o -> o[1]));

    assertEquals(true, jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", data));
    assertEquals(false, jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testSingleArgument(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertFalse((boolean) jsonLogic.apply("{\"in\": [\"Spring\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testBadSecondArgument(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertFalse((boolean) jsonLogic.apply("{\"in\": [\"Spring\", 3]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInWithDoubleQuoteInHaystackHit(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "say \"hello\"");
    assertEquals(true, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"say \\\"hello\\\"\", \"other\"]]}",
        data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInWithDoubleQuoteInHaystackMiss(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "say 'hello'");
    assertEquals(false, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"say \\\"hello\\\"\", \"other\"]]}",
        data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInWithSingleQuoteInHaystackHit(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "it's");
    assertEquals(true, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"it's\", \"other\"]]}",
        data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInWithSingleQuoteInHaystackMiss(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "its");
    assertEquals(false, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"it's\", \"other\"]]}",
        data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInWithBackslashInHaystackHit(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "C:\\Users");
    assertEquals(true, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"C:\\\\Users\", \"other\"]]}",
        data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testInWithBackslashInHaystackMiss(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "C:/Users");
    assertEquals(false, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"C:\\\\Users\", \"other\"]]}",
        data));
  }
}
