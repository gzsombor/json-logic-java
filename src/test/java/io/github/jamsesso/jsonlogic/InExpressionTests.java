package io.github.jamsesso.jsonlogic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class InExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public InExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  @Test
  public void testStringIn() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"in\": [\"race\", \"racecar\"]}", null));
  }

  @Test
  public void testStringNotIn() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"in\": [\"race\", \"clouds\"]}", null));
    assertEquals(false, jsonLogic.apply("{\"in\": [null, \"clouds\"]}", null));
  }

  @Test
  public void testArrayIn() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"in\": [1, [1, 2, 3]]}", null));
    assertEquals(true, jsonLogic.apply("{\"in\": [4.56, [1, 2, 3, 4.56]]}", null));
    assertEquals(true, jsonLogic.apply("{\"in\": [null, [1, 2, 3, null]]}", null));
  }

  @Test
  public void testArrayNotIn() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"in\": [5, [1, 2, 3]]}", null));
    assertEquals(false, jsonLogic.apply("{\"in\": [null, [1, 2, 3]]}", null));
  }

  @Test
  public void testInVariableInt() throws JsonLogicException {
    Map data = Collections.singletonMap("list", Arrays.asList(1, 2, 3));
    assertEquals(true, jsonLogic.apply("{\"in\": [2, {\"var\": \"list\"}]}", data));
  }

  @Test
  public void testNotInVariableInt() throws JsonLogicException {
    Map data = Collections.singletonMap("list", Arrays.asList(1, 2, 3));
    assertEquals(false, jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", data));
    assertEquals(false, jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", null));
  }

  @Test
  public void testAllVariables() throws JsonLogicException {
    Map data = Stream.of(new Object[][]{
        {"list", Arrays.asList(1, 2, 3)},
        {"value", 3}
    }).collect(Collectors.toMap(o -> o[0], o -> o[1]));

    assertEquals(true, jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", data));
    assertEquals(false, jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", null));
  }

  @Test
  public void testSingleArgument() throws JsonLogicException {
    assertFalse((boolean) jsonLogic.apply("{\"in\": [\"Spring\"]}", null));
  }

  @Test
  public void testBadSecondArgument() throws JsonLogicException {
    assertFalse((boolean) jsonLogic.apply("{\"in\": [\"Spring\", 3]}", null));
  }

  @Test
  public void testInWithDoubleQuoteInHaystackHit() throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "say \"hello\"");
    assertEquals(true, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"say \\\"hello\\\"\", \"other\"]]}",
        data));
  }

  @Test
  public void testInWithDoubleQuoteInHaystackMiss() throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "say 'hello'");
    assertEquals(false, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"say \\\"hello\\\"\", \"other\"]]}",
        data));
  }

  @Test
  public void testInWithSingleQuoteInHaystackHit() throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "it's");
    assertEquals(true, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"it's\", \"other\"]]}",
        data));
  }

  @Test
  public void testInWithSingleQuoteInHaystackMiss() throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "its");
    assertEquals(false, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"it's\", \"other\"]]}",
        data));
  }

  @Test
  public void testInWithBackslashInHaystackHit() throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "C:\\Users");
    assertEquals(true, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"C:\\\\Users\", \"other\"]]}",
        data));
  }

  @Test
  public void testInWithBackslashInHaystackMiss() throws JsonLogicException {
    Map<String, Object> data = Collections.singletonMap("v", "C:/Users");
    assertEquals(false, jsonLogic.apply(
        "{\"in\": [{\"var\": \"v\"}, [\"C:\\\\Users\", \"other\"]]}",
        data));
  }
}
