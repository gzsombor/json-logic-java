package io.github.jamsesso.jsonlogic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static io.github.jamsesso.jsonlogic.JsonLogicExceptionTestUtility.testErrorJsonPath;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ArrayHasExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public ArrayHasExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  @Test
  public void testSomeWithNull() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"and\":[{\"some\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"\"},[\"apple\"]]}]}]}", "{\"fruits\":null}"));
  }

  @Test
  public void testSomeEmptyArray() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"\"}, 0]}]}", null));
  }

  @Test
  public void testSomeAll() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 3]}]}", null));
    assertEquals(true, jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 1]}]}", null));
  }

  @Test
  public void testNoneWithNull() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"and\":[{\"none\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"\"},[\"apple\"]]}]}]}", "{\"fruits\":null}"));
  }

  @Test
  public void testNoneEmptyArray() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"\"}, 0]}]}", null));
  }

  @Test
  public void testNoneAll() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 3]}]}", null));
    assertEquals(false, jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, 2]}]}", null));
  }

  @Test
  public void testInvalidArrayHasExpression() {
    String json = "{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"\"}, {}]}]}";
    // -----------------------------------------------------------  ^  ----
    String expectedErrorJsonPath = "$.some[1].>[1]";

    testErrorJsonPath(jsonLogic, json, expectedErrorJsonPath);
  }

  // ---- array equality via === (strict) ---------------------------------------

  @Test
  public void testStrictEqualityIdenticalArrays() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"===\": [[1, 2, 3], [1, 2, 3]]}", null));
  }

  @Test
  public void testStrictEqualityEmptyArrays() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"===\": [[], []]}", null));
  }

  @Test
  public void testStrictEqualityDifferentElements() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"===\": [[1, 2], [1, 3]]}", null));
  }

  @Test
  public void testStrictEqualityLeftLonger() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"===\": [[1, 2, 3], [1, 2]]}", null));
  }

  @Test
  public void testStrictEqualityRightLonger() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"===\": [[1, 2], [1, 2, 3]]}", null));
  }

  // ---- array equality via == (loose) -----------------------------------------
  //
  // JS loose equality for objects/arrays is reference-based, not value-based.
  // Two distinct array literals are never == even with identical elements.
  // Empty arrays are non-truthy, so the non-truthy fallback fires for []==[]→true.

  @Test
  public void testLooseEqualityIdenticalArrays() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2, 3], [1, 2, 3]]}", null));
  }

  @Test
  public void testLooseEqualityEmptyArrays() throws JsonLogicException {
    assertEquals(true, jsonLogic.apply("{\"==\": [[], []]}", null));
  }

  @Test
  public void testLooseEqualityDifferentElements() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2], [1, 3]]}", null));
  }

  @Test
  public void testLooseEqualityLeftLonger() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2, 3], [1, 2]]}", null));
  }

  @Test
  public void testLooseEqualityRightLonger() throws JsonLogicException {
    assertEquals(false, jsonLogic.apply("{\"==\": [[1, 2], [1, 2, 3]]}", null));
  }
}
