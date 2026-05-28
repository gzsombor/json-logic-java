package io.github.jamsesso.jsonlogic.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArrayLikeTest {

  @Test
  public void shouldConvertListToTransformedList() {
    final List<Object> result = ArrayLike.toList(Arrays.asList(1, "two", null));

    assertEquals(Arrays.asList(1.0, "two", null), result);
  }

  @Test
  public void shouldConvertObjectArrayToTransformedList() {
    final List<Object> result = ArrayLike.toList(new Object[]{1, "two", null});

    assertEquals(Arrays.asList(1.0, "two", null), result);
  }

  @Test
  public void shouldConvertPrimitiveArrayToTransformedList() {
    final List<Object> result = ArrayLike.toList(new int[]{1, 2, 3});

    assertEquals(Arrays.asList(1.0, 2.0, 3.0), result);
  }

  @Test
  public void shouldConvertJsonArrayToList() {
    final JsonArray jsonArray = new JsonArray();
    jsonArray.add(1);
    jsonArray.add("two");
    jsonArray.add((String) null);

    final List<Object> result = ArrayLike.toList(jsonArray);

    assertEquals(Arrays.asList(1.0, "two", null), result);
  }

  @Test
  public void shouldConvertIterableToTransformedList() {
    final Iterable<Object> iterable = () -> Arrays.<Object>asList(1, "two", null).iterator();

    final List<Object> result = ArrayLike.toList(iterable);

    assertEquals(Arrays.asList(1.0, "two", null), result);
  }

  @Test
  public void shouldRejectUnsupportedToListInput() {
    assertThrows(IllegalArgumentException.class, () -> ArrayLike.toList("not an array"));
  }

  @Test
  public void shouldIterateListWithoutChangingValuesExceptNumberTransform() {
    final Iterator<Object> iterator = ArrayLike.iterator(Arrays.asList(1, "two", null));

    assertTrue(iterator.hasNext());
    assertEquals(1.0, iterator.next());
    assertEquals("two", iterator.next());
    assertEquals(null, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void shouldIterateObjectArray() {
    final Iterator<Object> iterator = ArrayLike.iterator(new Object[]{1, "two"});

    assertEquals(1.0, iterator.next());
    assertEquals("two", iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void shouldIteratePrimitiveArray() {
    final Iterator<Object> iterator = ArrayLike.iterator(new int[]{1, 2});

    assertEquals(1.0, iterator.next());
    assertEquals(2.0, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void shouldIterateJsonArray() {
    final JsonArray jsonArray = new JsonArray();
    jsonArray.add(1);
    jsonArray.add("two");

    final Iterator<Object> iterator = ArrayLike.iterator(jsonArray);

    assertEquals(1.0, iterator.next());
    assertEquals("two", iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void shouldIterateIterable() {
    final Iterable<Object> iterable = () -> Arrays.<Object>asList(1, "two").iterator();

    final Iterator<Object> iterator = ArrayLike.iterator(iterable);

    assertEquals(1.0, iterator.next());
    assertEquals("two", iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void shouldExposeIterableView() {
    final Iterator<Object> iterator = ArrayLike.iterable(Arrays.asList(1, "two")).iterator();

    assertEquals(1.0, iterator.next());
    assertEquals("two", iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void shouldRejectUnsupportedIteratorInput() {
    assertThrows(IllegalArgumentException.class, () -> ArrayLike.iterator("not an array"));
  }

  @Test
  public void shouldReportEligibilityForSupportedTypes() {
    final JsonArray jsonArray = new JsonArray();

    assertTrue(ArrayLike.isEligible(Collections.emptyList()));
    assertTrue(ArrayLike.isEligible(new Object[0]));
    assertTrue(ArrayLike.isEligible(new int[0]));
    assertTrue(ArrayLike.isEligible(jsonArray));
    assertTrue(ArrayLike.isEligible((Iterable<Object>) Collections::emptyIterator));
  }

  @Test
  public void shouldReportIneligibilityForUnsupportedTypes() {
    assertFalse(ArrayLike.isEligible(null));
    assertFalse(ArrayLike.isEligible("not an array"));
  }

  @Test
  public void shouldReportEmptyForSupportedTypes() {
    final JsonArray jsonArray = new JsonArray();

    assertTrue(ArrayLike.isEmpty(Collections.emptyList()));
    assertTrue(ArrayLike.isEmpty(new Object[0]));
    assertTrue(ArrayLike.isEmpty(new int[0]));
    assertTrue(ArrayLike.isEmpty(jsonArray));
    assertTrue(ArrayLike.isEmpty((Iterable<Object>) Collections::emptyIterator));
  }

  @Test
  public void shouldReportNonEmptyForSupportedTypes() {
    final JsonArray jsonArray = new JsonArray();
    jsonArray.add(1);

    assertFalse(ArrayLike.isEmpty(Collections.singletonList(1)));
    assertFalse(ArrayLike.isEmpty(new Object[]{1}));
    assertFalse(ArrayLike.isEmpty(new int[]{1}));
    assertFalse(ArrayLike.isEmpty(jsonArray));
    assertFalse(ArrayLike.isEmpty((Iterable<Object>) () -> Collections.singletonList((Object) 1).iterator()));
  }

  @Test
  public void shouldRejectUnsupportedIsEmptyInput() {
    assertThrows(IllegalArgumentException.class, () -> ArrayLike.isEmpty("not an array"));
  }
}
