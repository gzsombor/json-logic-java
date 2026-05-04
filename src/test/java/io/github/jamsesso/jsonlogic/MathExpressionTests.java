package io.github.jamsesso.jsonlogic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class MathExpressionTests {
  // ---- addition ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldAddTwoLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(6.0, jsonLogic.apply("{\"+\":[4,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldAddManyLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(10.0, jsonLogic.apply("{\"+\":[2,2,2,2,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldCoerceSingleStringToNumber(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(3.14, jsonLogic.apply("{\"+\" : \"3.14\"}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldUnwrapNestedArrayOnAdd(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(5.0, jsonLogic.apply("{\"+\":[2,[[3,4],5]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullForNonNumericAdd(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"+\" : \"foo\"}", null));
    assertNull(jsonLogic.apply("{\"+\" : [\"foo\"]}", null));
    assertNull(jsonLogic.apply("{\"+\" : [1, \"foo\"]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldAddVars(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 3, "y", 7);
    assertEquals(10.0, jsonLogic.apply("{\"+\":[{\"var\":\"x\"},{\"var\":\"y\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenFirstAddArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"+\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenSecondAddArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"+\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- subtraction ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSubtractTwoLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(2.0, jsonLogic.apply("{\"-\":[4,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldNegateSingleLiteral(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(-2.0, jsonLogic.apply("{\"-\": 2 }", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldNegateSingleStringLiteral(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(-2.0, jsonLogic.apply("{\"-\": \"2\" }", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldSubtractVarFromLiteral(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 10);
    assertEquals(7.0, jsonLogic.apply("{\"-\":[{\"var\":\"x\"},3]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldNegateVar(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 5);
    assertEquals(-5.0, jsonLogic.apply("{\"-\":{\"var\":\"x\"}}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenFirstSubtractArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"-\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenSecondSubtractArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"-\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenNegatingNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"-\":{\"var\":\"x\"}}", Map.of()));
  }

  // ---- multiplication ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldMultiplyTwoLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(8.0, jsonLogic.apply("{\"*\":[4,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldMultiplyManyLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(32.0, jsonLogic.apply("{\"*\":[2,2,2,2,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldUnwrapNestedArrayOnMultiply(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(6.0, jsonLogic.apply("{\"*\":[2,[[3, 4], 5]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenMultiplyingByEmptyArray(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"*\":[2,[]]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldMultiplyVarByLiteral(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 6);
    assertEquals(12.0, jsonLogic.apply("{\"*\":[{\"var\":\"x\"},2]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenFirstMultiplyArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"*\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenSecondMultiplyArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"*\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- division ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldDivideTwoLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(2.0, jsonLogic.apply("{\"/\":[4,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnInfinityOnDivideByZero(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(Double.POSITIVE_INFINITY, jsonLogic.apply("{\"/\":[4,0]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullForDivideWithSingleArg(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"/\": [0]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldDivideVarByLiteral(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 9);
    assertEquals(3.0, jsonLogic.apply("{\"/\":[{\"var\":\"x\"},3]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenDividendIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"/\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenDivisorIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"/\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- modulo ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldComputeModulo(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(1.0, jsonLogic.apply("{\"%\": [101,2]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldComputeModuloWithVar(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 10);
    assertEquals(1.0, jsonLogic.apply("{\"%\":[{\"var\":\"x\"},3]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenModuloDividendIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"%\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenModuloDivisorIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"%\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- min / max ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnMinOfLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(1.0, jsonLogic.apply("{\"min\":[1,2,3]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnMaxOfLiterals(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertEquals(3.0, jsonLogic.apply("{\"max\":[1,2,3]}", null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnMinWithVar(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 5);
    assertEquals(2.0, jsonLogic.apply("{\"min\":[{\"var\":\"x\"},10,2]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnMaxWithVar(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 99);
    assertEquals(99.0, jsonLogic.apply("{\"max\":[1,{\"var\":\"x\"},3]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenFirstMinArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"min\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenSecondMinArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"min\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenFirstMaxArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"max\":[{\"var\":\"x\"},5]}", Map.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldReturnNullWhenSecondMaxArgIsNull(String label, JsonLogic jsonLogic) throws JsonLogicException {
    assertNull(jsonLogic.apply("{\"max\":[5,{\"var\":\"x\"}]}", Map.of()));
  }

  // ---- composed expressions ----

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldComputeLinearExpression(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 4, "y", 3);
    assertEquals(11.0, jsonLogic.apply("{\"+\":[{\"*\":[{\"var\":\"x\"},2]},{\"var\":\"y\"}]}", data));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("io.github.jamsesso.jsonlogic.JsonLogicTestEngines#engines")
  public void shouldComputeNestedArithmetic(String label, JsonLogic jsonLogic) throws JsonLogicException {
    Map<String, Object> data = Map.of("x", 4, "y", 3);
    assertEquals(2.0, jsonLogic.apply("{\"/\":[{\"-\":[10,{\"var\":\"x\"}]},{\"var\":\"y\"}]}", data));
  }
}
