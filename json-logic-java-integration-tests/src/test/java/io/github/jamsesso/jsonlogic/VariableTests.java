package io.github.jamsesso.jsonlogic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class VariableTests {

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testEmptyString(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(3.14, jsonLogic.apply("{\"var\": \"\"}", 3.14));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testMapAccess(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Double> data = new HashMap<String, Double>() {{
      put("pi", 3.14);
    }};

    assertEquals(3.14, jsonLogic.apply("{\"var\": \"pi\"}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testDefaultValue(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(3.14, jsonLogic.apply("{\"var\": [\"pi\", 3.14]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testUndefined(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"var\": [\"pi\"]}", null));
    assertNull(jsonLogic.apply("{\"var\": \"\"}", null));
    assertNull(jsonLogic.apply("{\"var\": 0}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testArrayAccess(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String[] data = new String[] {"hello", "world"};

    assertEquals("hello", jsonLogic.apply("{\"var\": 0}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": 1}", data));
    assertNull(jsonLogic.apply("{\"var\": 2}", data));
    assertNull(jsonLogic.apply("{\"var\": 3}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testArrayAccessWithStringKeys(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String[] data = new String[] {"hello", "world"};

    assertEquals("hello", jsonLogic.apply("{\"var\": \"0\"}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": \"1\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"2\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"3\"}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testListAccess(String label, JsonLogic jsonLogic) throws JsonLogicException {
    List<String> data = Arrays.asList("hello", "world");

    assertEquals("hello", jsonLogic.apply("{\"var\": 0}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": 1}", data));
    assertNull(jsonLogic.apply("{\"var\": 2}", data));
    assertNull(jsonLogic.apply("{\"var\": 3}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testListAccessWithStringKeys(String label, JsonLogic jsonLogic) throws JsonLogicException {
    List<String> data = Arrays.asList("hello", "world");

    assertEquals("hello", jsonLogic.apply("{\"var\": \"0\"}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": \"1\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"2\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"3\"}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void testComplexAccess(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("users", Arrays.asList(
        new HashMap<String, Object>() {{
          put("name", "John");
          put("followers", 1337);
        }},
        new HashMap<String, Object>() {{
          put("name", "Jane");
          put("followers", 2048);
        }}
      ));
    }};

    assertEquals("John", jsonLogic.apply("{\"var\": \"users.0.name\"}", data));
    assertEquals(1337.0, jsonLogic.apply("{\"var\": \"users.0.followers\"}", data));
    assertEquals("Jane", jsonLogic.apply("{\"var\": \"users.1.name\"}", data));
    assertEquals(2048.0, jsonLogic.apply("{\"var\": \"users.1.followers\"}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void missingNestedMapKey_returnsDefault(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"a.b.c\", \"fallback\"]}";
    Map<String, Object> data = map("a", map("b", new HashMap<>()));

    Object result = jsonLogic.apply(rule, data);

    assertEquals("fallback", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void presentNullLeaf_returnsNull_notDefault(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"user.age\", 42]}";
    Map<String, Object> user = new HashMap<>();
    user.put("age", null);
    Map<String, Object> data = map("user", user);

    Object result = jsonLogic.apply(rule, data);

    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void defaultValueFromOtherVariable(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{ \"var\": [ \"a\", { \"var\": \"b\" } ] }";
    Map<String, Object> data = map("b", 123.0);
    assertEquals(123.0, jsonLogic.apply(rule, data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void intermediateNull_returnsNull_notDefault(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"a.b.c\", \"fallback\"]}";
    Map<String, Object> data = map("a", map("b", null));

    Object result = jsonLogic.apply(rule, data);

    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void nonTraversableIntermediate_returnsNull_notDefault(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"a.b\", \"fallback\"]}";
    Map<String, Object> data = map("a", 5);

    Object result = jsonLogic.apply(rule, data);

    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void arrayIndexWithinBounds_returnsElement_asDoubleForNumbers(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"items.1\", 999]}";
    Map<String, Object> data = map("items", Arrays.asList(10, 20));

    Object result = jsonLogic.apply(rule, data);

    assertTrue(result instanceof Number);
    assertEquals(20.0, ((Number) result).doubleValue(), 0.0);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void arrayIndexOutOfBounds_returnsDefault(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"items.2\", \"missing\"]}";
    Map<String, Object> data = map("items", Arrays.asList(10, 20));

    Object result = jsonLogic.apply(rule, data);

    assertEquals("missing", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void arrayElementPresentButNull_returnsNull_notDefault(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [\"items.0\", \"missing\"]}";
    Map<String, Object> data = map("items", Collections.singletonList(null));

    Object result = jsonLogic.apply(rule, data);

    assertNull(result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void topLevelNumericIndex_overList_works(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": [1, \"missing\"]}";
    List<String> data = Arrays.asList("apple", "banana", "carrot");

    Object result = jsonLogic.apply(rule, data);

    assertEquals("banana", result);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void emptyVarKey_returnsWholeDataObject(String label, JsonLogic jsonLogic) throws JsonLogicException {
    String rule = "{\"var\": \"\"}";
    Map<String, Object> data = map("x", 1);

    Object result = jsonLogic.apply(rule, data);

    assertSame(data, result, "Should return the same data instance");
  }

  private static Map<String, Object> map(Object... kv) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }
}
