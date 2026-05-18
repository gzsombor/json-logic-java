package io.github.jamsesso.jsonlogic;

import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Direct unit tests for {@link ArrayLike#equals}.
 *
 * <p>{@code ArrayLike.equals} is not reachable through {@code JsonLogic.apply()}: the evaluator
 * and compiled path never return an {@code ArrayLike} instance as the result of resolving a
 * variable — arrays fetched from data are returned as plain {@code java.util.List} instances.
 * The bug (unconditional {@code return false} after a successful element-wise comparison) must
 * therefore be covered at the unit level by constructing an {@code ArrayLike} directly.
 */
public class ArrayLikeTest {

  private static ArrayLike of(Object... elements) {
    return new ArrayLike(Arrays.asList(elements));
  }

  // ArrayLike transforms Number elements to Double via JsonLogicEvaluator.transform,
  // so comparisons against plain Lists must use Double values to match what's stored.

  @Test
  public void testEqualsSelf() {
    ArrayLike a = of(1.0, 2.0, 3.0);
    assertEquals(true, a.equals(a));
  }

  @Test
  public void testEqualsIdenticalList() {
    assertEquals(true, of(1.0, 2.0, 3.0).equals(Arrays.asList(1.0, 2.0, 3.0)));
  }

  @Test
  public void testEqualsIdenticalArrayLike() {
    assertEquals(true, of(1.0, 2.0, 3.0).equals(of(1.0, 2.0, 3.0)));
  }

  @Test
  public void testEqualsEmptyList() {
    assertEquals(true, of().equals(Collections.emptyList()));
  }

  @Test
  public void testEqualsEmptyArrayLike() {
    assertEquals(true, of().equals(of()));
  }

  @Test
  public void testNotEqualsDifferentElement() {
    assertEquals(false, of(1.0, 2.0, 3.0).equals(Arrays.asList(1.0, 2.0, 4.0)));
  }

  @Test
  public void testNotEqualsLeftLonger() {
    assertEquals(false, of(1.0, 2.0, 3.0).equals(Arrays.asList(1.0, 2.0)));
  }

  @Test
  public void testNotEqualsRightLonger() {
    assertEquals(false, of(1.0, 2.0).equals(Arrays.asList(1.0, 2.0, 3.0)));
  }

  @Test
  public void testNotEqualsNonIterable() {
    assertEquals(false, of(1.0, 2.0, 3.0).equals("not a list"));
  }
}
