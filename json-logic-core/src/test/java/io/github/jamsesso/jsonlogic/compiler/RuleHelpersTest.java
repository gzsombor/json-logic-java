package io.github.jamsesso.jsonlogic.compiler;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
import static org.junit.jupiter.api.Assertions.*;

public class RuleHelpersTest {

  // ---- looseEq ----

  @Test
  public void looseEq_nullNull() {
    assertTrue(looseEq(null, null));
  }

  @Test
  public void looseEq_nullNonNull() {
    assertFalse(looseEq(null, 1.0));
    assertFalse(looseEq("x", null));
  }

  @Test
  public void looseEq_twoNumbers() {
    assertTrue(looseEq(1.0, 1));
    assertFalse(looseEq(1.0, 2.0));
  }

  @Test
  public void looseEq_numberAndString() {
    assertTrue(looseEq(1.0, "1"));
    assertTrue(looseEq("0", 0.0));
    assertFalse(looseEq(1.0, "2"));
    assertTrue(looseEq(0.0, ""));
  }

  @Test
  public void looseEq_numberAndBoolean() {
    assertTrue(looseEq(1.0, true));
    assertTrue(looseEq(0.0, false));
    assertFalse(looseEq(2.0, true));
  }

  @Test
  public void looseEq_twoStrings() {
    assertTrue(looseEq("abc", "abc"));
    assertFalse(looseEq("abc", "def"));
  }

  @Test
  public void looseEq_stringAndBoolean() {
    assertTrue(looseEq("x", true));
    assertTrue(looseEq("", false));
  }

  @Test
  public void looseEq_twoBooleans() {
    assertTrue(looseEq(true, true));
    assertTrue(looseEq(false, false));
    assertFalse(looseEq(true, false));
  }

  // ---- strictEq ----

  @Test
  public void strictEq_twoNumbers() {
    assertTrue(strictEq(1.0, 1));
    assertFalse(strictEq(1.0, 2.0));
  }

  @Test
  public void strictEq_numberAndString() {
    assertFalse(strictEq(1.0, "1"));
  }

  @Test
  public void strictEq_nullNull() {
    assertTrue(strictEq(null, null));
  }

  @Test
  public void strictEq_nullNonNull() {
    assertFalse(strictEq(null, "x"));
    assertFalse(strictEq("x", null));
  }

  @Test
  public void strictEq_sameStrings() {
    assertTrue(strictEq("hello", "hello"));
    assertFalse(strictEq("hello", "world"));
  }

  // ---- toDouble ----

  @Test
  public void toDouble_fromNumber() {
    assertEquals(3.14, toDouble(3.14), 1e-12);
  }

  @Test
  public void toDouble_fromString() {
    assertEquals(42.0, toDouble("42"), 1e-12);
  }

  @Test
  public void toDouble_fromInvalidString() {
    assertTrue(Double.isNaN(toDouble("abc")));
  }

  @Test
  public void toDouble_fromBooleans() {
    assertEquals(1.0, toDouble(true), 0);
    assertEquals(0.0, toDouble(false), 0);
  }

  @Test
  public void toDouble_fromNull() {
    assertTrue(Double.isNaN(toDouble(null)));
  }

  @Test
  public void toComparableDouble_shouldCoerceNullAndBooleans() {
    assertEquals(0.0, toComparableDouble(null), 0);
    assertEquals(1.0, toComparableDouble(true), 0);
    assertEquals(0.0, toComparableDouble(false), 0);
  }

  @Test
  public void isNumeric_shouldAcceptNumbersNumericStringsAndBooleans() {
    assertTrue(isNumeric(1.0));
    assertTrue(isNumeric("1.5"));
    assertTrue(isNumeric(true));
    assertFalse(isNumeric("not-a-number"));
    assertFalse(isNumeric(null));
  }

  // ---- toDoubleNullable ----

  @Test
  public void toDoubleNullable_fromNull() {
    assertNull(toDoubleNullable(null));
  }

  @Test
  public void toDoubleNullable_fromNumber() {
    assertEquals(7.0, toDoubleNullable(7.0), 1e-12);
  }

  @Test
  public void toDoubleNullable_fromValidString() {
    assertEquals(3.0, toDoubleNullable("3"), 1e-12);
  }

  @Test
  public void toDoubleNullable_fromInvalidString() {
    assertNull(toDoubleNullable("not-a-number"));
  }

  @Test
  public void toDoubleNullable_fromBoolean() {
    assertNull(toDoubleNullable(true));
  }

  // ---- mathReduce ----

  @Test
  public void mathReduce_addIntegers() {
    assertEquals(6.0, ((Number) mathReduce("+", Arrays.asList(1.0, 2.0, 3.0))).doubleValue(), 0);
  }

  @Test
  public void mathReduce_multiplyIntegers() {
    assertEquals(24.0, ((Number) mathReduce("*", Arrays.asList(2.0, 3.0, 4.0))).doubleValue(), 0);
  }

  @Test
  public void mathReduce_emptyList() {
    assertNull(mathReduce("+", Collections.emptyList()));
  }

  @Test
  public void mathReduce_singleArrayArgIsUnwrapped() {
    final List<Object> inner = Arrays.asList(10.0, 5.0);
    assertEquals(15.0, ((Number) mathReduce("+", Collections.singletonList(inner))).doubleValue(), 0);
  }

  @Test
  public void mathReduce_nullElementReturnsNull() {
    assertNull(mathReduce("+", Arrays.asList(1.0, null)));
  }

  @Test
  public void unwrapArrayArg_shouldUnwrapNestedSingleElementArrays() {
    assertEquals(3.0, unwrapArrayArg(Collections.singletonList(Collections.singletonList(3.0))));
  }

  @Test
  public void unwrapArrayArg_shouldReturnNullForEmptyArray() {
    assertNull(unwrapArrayArg(Collections.emptyList()));
  }

  @Test
  public void mathReduce_shouldReturnNullForNonNumericNestedArrayValue() {
    assertNull(mathReduce("+", Collections.singletonList(Collections.singletonList("x"))));
  }

  // ---- catStr ----

  @Test
  public void catStr_null() {
    assertEquals("null", catStr(null));
  }

  @Test
  public void catStr_integerDouble() {
    assertEquals("3", catStr(3.0));
  }

  @Test
  public void catStr_fractionalDouble() {
    assertEquals("3.5", catStr(3.5));
  }

  @Test
  public void catStr_string() {
    assertEquals("hello", catStr("hello"));
  }

  @Test
  public void cat_shouldConcatenateMixedValues() {
    assertEquals("a1nulltrue2.5", cat(Arrays.asList("a", 1.0, null, true, 2.5)));
  }

  @Test
  public void substr_shouldReturnSuffixForNegativeStart() throws Exception {
    assertEquals("logic", substr("jsonlogic", -5.0, null, ".substr"));
  }

  @Test
  public void substr_shouldReturnEmptyWhenStartExceedsLength() throws Exception {
    assertEquals("", substr("jsonlogic", 20.0, null, ".substr"));
  }

  @Test
  public void substr_shouldThrowWithIndexedPathForInvalidStart() {
    final JsonLogicEvaluationException exception = assertThrows(
        JsonLogicEvaluationException.class,
        () -> substr("jsonlogic", "x", null, ".substr"));

    assertEquals(".substr[1]", exception.getJsonPath());
  }

  @Test
  public void in_shouldSupportStringHaystack() {
    assertTrue(in("logic", "jsonlogic"));
    assertFalse(in(null, "jsonlogic"));
    assertFalse(in("missing", "jsonlogic"));
  }

  @Test
  public void in_shouldSupportArrayHaystackWithStrictContainsSemantics() {
    assertTrue(in(1.0, Arrays.asList("1", 1.0, true)));
    assertFalse(in(1, Arrays.asList("1", 1.0, true)));
  }

  @Test
  public void in_shouldReturnFalseForUnsupportedHaystack() {
    assertFalse(in("a", Collections.singletonMap("a", true)));
  }

  @Test
  public void merge_shouldFlattenArrayLikeValuesAndKeepScalars() {
    assertEquals(Arrays.asList(1.0, 2.0, "x", null), merge(Arrays.asList(Arrays.asList(1.0, 2.0), "x", null)));
  }

  // ---- resolveVar ----

  @Test
  public void resolveVar_nullDataReturnsDefault() throws Exception {
    assertEquals("default", resolveVar(null, "key", "default"));
  }

  @Test
  public void resolveVar_nullKeyReturnsData() throws Exception {
    assertEquals(42.0, resolveVar(42.0, null, "default"));
  }

  @Test
  public void resolveVar_mapLookup() throws Exception {
    final var data = new HashMap<String, Object>();
    data.put("name", "Alice");
    assertEquals("Alice", resolveVar(data, "name", null));
  }

  @Test
  public void resolveVar_missingMapKeyReturnsDefault() throws Exception {
    final var data = new HashMap<String, Object>();
    data.put("name", "Alice");
    assertEquals("missing", resolveVar(data, "age", "missing"));
  }

  @Test
  public void resolveVar_nestedMapLookup() throws Exception {
    final var inner = new HashMap<String, Object>();
    inner.put("city", "Budapest");
    final var data = new HashMap<String, Object>();
    data.put("address", inner);
    assertEquals("Budapest", resolveVar(data, "address.city", null));
  }

  @Test
  public void resolveVar_listIndexLookup() throws Exception {
    final List<Object> data = Arrays.asList("a", "b", "c");
    assertEquals("b", resolveVar(data, 1.0, null));
  }

  @Test
  public void resolveVar_listIndexOutOfBoundsReturnsDefault() throws Exception {
    final List<Object> data = Arrays.asList("a", "b");
    assertEquals("default", resolveVar(data, 5.0, "default"));
  }

  @Test
  public void resolveVar_emptyStringKeyReturnsData() throws Exception {
    assertEquals("hello", resolveVar("hello", "", null));
  }

  @Test
  public void resolveVar_presentNullLeafReturnsNullNotDefault() throws Exception {
    final var data = new HashMap<String, Object>();
    data.put("name", null);

    assertNull(resolveVar(data, "name", "default"));
  }

  @Test
  public void resolveVar_intermediateNullReturnsNullNotDefault() throws Exception {
    final var data = new HashMap<String, Object>();
    data.put("user", null);

    assertNull(resolveVar(data, "user.name", "default"));
  }

  @Test
  public void resolveVar_shouldResolveNestedArrayPathFromMap() throws Exception {
    final var data = new HashMap<String, Object>();
    data.put("items", Arrays.asList("first", "second"));

    assertEquals("second", resolveVar(data, "items.1", "default"));
  }

  @Test
  public void missing_shouldReturnAbsentKeysFromMapData() {
    final var data = new LinkedHashMap<String, Object>();
    data.put("name", "Alice");

    assertEquals(Collections.singletonList("age"), missing(Arrays.asList("name", "age"), data));
  }

  @Test
  public void missing_shouldTreatSingleArrayArgumentAsKeys() {
    final var data = new LinkedHashMap<String, Object>();
    data.put("name", "Alice");

    assertEquals(Collections.singletonList("age"), missing(Collections.singletonList(Arrays.asList("name", "age")), data));
  }

  @Test
  public void missing_shouldReturnAllKeysWhenDataIsNotMapLike() {
    assertEquals(Arrays.asList("name", "age"), missing(Arrays.asList("name", "age"), null));
  }

  @Test
  public void missingSomeChecked_shouldReturnEmptyWhenEnoughKeysArePresent() throws Exception {
    final var data = new LinkedHashMap<String, Object>();
    data.put("email", "a@example.com");

    assertEquals(Collections.emptyList(), missingSomeChecked(1.0, Arrays.asList("email", "phone"), data));
  }

  @Test
  public void missingSomeChecked_shouldReturnMissingKeysWhenThresholdIsNotMet() throws Exception {
    final var data = new LinkedHashMap<String, Object>();
    data.put("email", "a@example.com");

    assertEquals(Collections.singletonList("phone"), missingSomeChecked(2.0, Arrays.asList("email", "phone"), data));
  }

  @Test
  public void missingSomeChecked_shouldThrowForInvalidArgumentTypes() {
    assertThrows(Exception.class, () -> missingSomeChecked("1", Arrays.asList("email"), Collections.emptyMap()));
    assertThrows(Exception.class, () -> missingSomeChecked(1.0, "email", Collections.emptyMap()));
  }
}
