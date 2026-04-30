package io.github.jamsesso.jsonlogic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class MathExpressionTests {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> engines() {
    return Arrays.asList(new Object[][]{
        {"interpreter", new JsonLogic(false)},
        {"compiled",    new JsonLogic(true)},
    });
  }

  private final JsonLogic jsonLogic;

  public MathExpressionTests(String label, JsonLogic jsonLogic) {
    this.jsonLogic = jsonLogic;
  }

  // ---- addition ----

  @Test
  public void shouldAddTwoLiterals() throws JsonLogicException {
    assertEquals(6.0, jsonLogic.apply("{\"+\":[4,2]}", null));
  }

  @Test
  public void shouldAddManyLiterals() throws JsonLogicException {
    assertEquals(10.0, jsonLogic.apply("{\"+\":[2,2,2,2,2]}", null));
  }

  @Test
  public void shouldCoerceSingleStringToNumber() throws JsonLogicException {
    assertEquals(3.14, jsonLogic.apply("{\"+\" : \"3.14\"}", null));
  }

  @Test
  public void shouldUnwrapNestedArrayOnAdd() throws JsonLogicException {
    // Matches reference impl at jsonlogic.com: nested array flattened, only first element kept
    assertEquals(5.0, jsonLogic.apply("{\"+\":[2,[[3,4],5]]}", null));
  }

  @Test
  public void shouldReturnNullForNonNumericAdd() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"+\" : \"foo\"}", null));
    assertNull(jsonLogic.apply("{\"+\" : [\"foo\"]}", null));
    assertNull(jsonLogic.apply("{\"+\" : [1, \"foo\"]}", null));
  }

  @Test
  public void shouldAddVars() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 3, "y", 7);
    assertEquals(10.0, jsonLogic.apply("{\"+\":[{\"var\":\"x\"},{\"var\":\"y\"}]}", data));
  }

  @Test
  public void shouldReturnNullWhenFirstAddArgIsNull() throws JsonLogicException {
    // var "x" absent → null; null + number = null
    assertNull(jsonLogic.apply("{\"+\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenSecondAddArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"+\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- subtraction ----

  @Test
  public void shouldSubtractTwoLiterals() throws JsonLogicException {
    assertEquals(2.0, jsonLogic.apply("{\"-\":[4,2]}", null));
  }

  @Test
  public void shouldNegateSingleLiteral() throws JsonLogicException {
    assertEquals(-2.0, jsonLogic.apply("{\"-\": 2 }", null));
  }

  @Test
  public void shouldNegateSingleStringLiteral() throws JsonLogicException {
    assertEquals(-2.0, jsonLogic.apply("{\"-\": \"2\" }", null));
  }

  @Test
  public void shouldSubtractVarFromLiteral() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 10);
    assertEquals(7.0, jsonLogic.apply("{\"-\":[{\"var\":\"x\"},3]}", data));
  }

  @Test
  public void shouldNegateVar() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 5);
    assertEquals(-5.0, jsonLogic.apply("{\"-\":{\"var\":\"x\"}}", data));
  }

  @Test
  public void shouldReturnNullWhenFirstSubtractArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"-\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenSecondSubtractArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"-\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenNegatingNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"-\":{\"var\":\"x\"}}", Map.of()));
  }

  // ---- multiplication ----

  @Test
  public void shouldMultiplyTwoLiterals() throws JsonLogicException {
    assertEquals(8.0, jsonLogic.apply("{\"*\":[4,2]}", null));
  }

  @Test
  public void shouldMultiplyManyLiterals() throws JsonLogicException {
    assertEquals(32.0, jsonLogic.apply("{\"*\":[2,2,2,2,2]}", null));
  }

  @Test
  public void shouldUnwrapNestedArrayOnMultiply() throws JsonLogicException {
    assertEquals(6.0, jsonLogic.apply("{\"*\":[2,[[3, 4], 5]]}", null));
  }

  @Test
  public void shouldReturnNullWhenMultiplyingByEmptyArray() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"*\":[2,[]]}", null));
  }

  @Test
  public void shouldMultiplyVarByLiteral() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 6);
    assertEquals(12.0, jsonLogic.apply("{\"*\":[{\"var\":\"x\"},2]}", data));
  }

  @Test
  public void shouldReturnNullWhenFirstMultiplyArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"*\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenSecondMultiplyArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"*\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- division ----

  @Test
  public void shouldDivideTwoLiterals() throws JsonLogicException {
    assertEquals(2.0, jsonLogic.apply("{\"/\":[4,2]}", null));
  }

  @Test
  public void shouldReturnInfinityOnDivideByZero() throws JsonLogicException {
    assertEquals(Double.POSITIVE_INFINITY, jsonLogic.apply("{\"/\":[4,0]}", null));
  }

  @Test
  public void shouldReturnNullForDivideWithSingleArg() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"/\": [0]}", null));
  }

  @Test
  public void shouldDivideVarByLiteral() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 9);
    assertEquals(3.0, jsonLogic.apply("{\"/\":[{\"var\":\"x\"},3]}", data));
  }

  @Test
  public void shouldReturnNullWhenDividendIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"/\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenDivisorIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"/\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- modulo ----

  @Test
  public void shouldComputeModulo() throws JsonLogicException {
    assertEquals(1.0, jsonLogic.apply("{\"%\": [101,2]}", null));
  }

  @Test
  public void shouldComputeModuloWithVar() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 10);
    assertEquals(1.0, jsonLogic.apply("{\"%\":[{\"var\":\"x\"},3]}", data));
  }

  @Test
  public void shouldReturnNullWhenModuloDividendIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"%\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenModuloDivisorIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"%\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- min / max ----

  @Test
  public void shouldReturnMinOfLiterals() throws JsonLogicException {
    assertEquals(1.0, jsonLogic.apply("{\"min\":[1,2,3]}", null));
  }

  @Test
  public void shouldReturnMaxOfLiterals() throws JsonLogicException {
    assertEquals(3.0, jsonLogic.apply("{\"max\":[1,2,3]}", null));
  }

  @Test
  public void shouldReturnMinWithVar() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 5);
    assertEquals(2.0, jsonLogic.apply("{\"min\":[{\"var\":\"x\"},10,2]}", data));
  }

  @Test
  public void shouldReturnMaxWithVar() throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 99);
    assertEquals(99.0, jsonLogic.apply("{\"max\":[1,{\"var\":\"x\"},3]}", data));
  }

  @Test
  public void shouldReturnNullWhenFirstMinArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"min\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenSecondMinArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"min\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenFirstMaxArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"max\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @Test
  public void shouldReturnNullWhenSecondMaxArgIsNull() throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"max\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- composed expressions ----

  @Test
  public void shouldComputeLinearExpression() throws JsonLogicException {
    // (x * 2) + y
    Map<String, Object> data = Map.of("x", 4, "y", 3);
    assertEquals(11.0, jsonLogic.apply("{\"+\":[{\"*\":[{\"var\":\"x\"},2]},{\"var\":\"y\"}]}", data));
  }

  @Test
  public void shouldComputeNestedArithmetic() throws JsonLogicException {
    // (10 - x) / y
    Map<String, Object> data = Map.of("x", 4, "y", 3);
    assertEquals(2.0, jsonLogic.apply("{\"/\":[{\"-\":[10,{\"var\":\"x\"}]},{\"var\":\"y\"}]}", data));
  }
}
