package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;

import java.util.List;
import java.util.Map;

/**
 * Static helper methods shared by all compiled rule classes.
 *
 * <p>Generated rule classes import these via
 * {@code import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*}
 * so the call sites in generated code are unqualified (e.g. {@code looseEq(a, b)}).
 *
 * <p>Keeping the helpers here instead of inlining them into every generated class:
 * <ul>
 *   <li>Reduces the amount of bytecode generated per rule.</li>
 *   <li>Allows the helpers to be unit-tested directly.</li>
 * </ul>
 */
public final class RuleHelpers {

  private RuleHelpers() {}

  // ---- equality ----

  public static boolean looseEq(Object left, Object right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    if (left instanceof Number leftNum && right instanceof Number rightNum) {
      return Double.valueOf(leftNum.doubleValue()).equals(rightNum.doubleValue());
    }
    if (left instanceof Number leftNum && right instanceof String rightStr) {
      return numEqStr(leftNum, rightStr);
    }
    if (left instanceof Number leftNum && right instanceof Boolean rightBool) {
      return numEqBool(leftNum, rightBool);
    }
    if (left instanceof String leftStr && right instanceof String rightStr) {
      return leftStr.equals(rightStr);
    }
    if (left instanceof String leftStr && right instanceof Number rightNum) {
      return numEqStr(rightNum, leftStr);
    }
    if (left instanceof String leftStr && right instanceof Boolean rightBool) {
      return strEqBool(leftStr, rightBool);
    }
    if (left instanceof Boolean leftBool && right instanceof Boolean rightBool) {
      return leftBool.booleanValue() == rightBool.booleanValue();
    }
    if (left instanceof Boolean leftBool && right instanceof Number rightNum) {
      return numEqBool(rightNum, leftBool);
    }
    if (left instanceof Boolean leftBool && right instanceof String rightStr) {
      return strEqBool(rightStr, leftBool);
    }
    return !JsonLogic.truthy(left) && !JsonLogic.truthy(right);
  }

  public static boolean strictEq(Object left, Object right) {
    if (left instanceof Number leftNum && right instanceof Number rightNum) {
      return leftNum.doubleValue() == rightNum.doubleValue();
    }
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equals(right);
  }

  private static boolean numEqStr(Number num, String str) {
    try {
      final String trimmed = str.trim().isEmpty() ? "0" : str.trim();
      return Double.parseDouble(trimmed) == num.doubleValue();
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private static boolean numEqBool(Number num, Boolean bool) {
    return bool ? num.doubleValue() == 1.0 : num.doubleValue() == 0.0;
  }

  private static boolean strEqBool(String str, Boolean bool) {
    return JsonLogic.truthy(str) == bool;
  }

  // ---- numeric coercion ----

  public static double toDouble(Object value) {
    if (value instanceof Number num) {
      return num.doubleValue();
    }
    if (value instanceof String str) {
      try {
        return Double.parseDouble(str);
      } catch (NumberFormatException ex) {
        return Double.NaN;
      }
    }
    if (value instanceof Boolean bool) {
      return bool ? 1.0 : 0.0;
    }
    return Double.NaN;
  }

  /** Returns {@code null} for non-numeric / empty-array inputs (mirrors {@code MathExpression}). */
  public static Double toDoubleNullable(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number num) {
      return num.doubleValue();
    }
    if (value instanceof String str) {
      try {
        return Double.parseDouble(str);
      } catch (NumberFormatException ex) {
        return null;
      }
    }
    return null;
  }

  // ---- math ----

  /** Unwraps nested single-element arrays, mirroring {@code MathExpression}'s behaviour for {@code +} and {@code *}. */
  public static Object unwrapArrayArg(Object value) {
    while (ArrayLike.isEligible(value)) {
      final var list = new ArrayLike(value);
      if (list.isEmpty()) {
        return null;
      }
      value = list.get(0);
    }
    return value;
  }

  /**
   * Mirrors {@code MathExpression.evaluate()} for {@code +} and {@code *}: handles
   * single-array-argument unwrapping and per-element array unwrapping.
   */
  public static Object mathReduce(String op, List<Object> args) {
    List<Object> effective = args;
    if (args.size() == 1 && ArrayLike.isEligible(args.get(0))) {
      effective = new ArrayLike(args.get(0));
    }
    Double acc = null;
    for (final Object raw : effective) {
      final Double num = toDoubleNullable(unwrapArrayArg(raw));
      if (num == null) {
        return null;
      }
      if (acc == null) {
        acc = num;
        continue;
      }
      if (op.equals("+")) {
        acc = acc + num;
      } else {
        acc = acc * num;
      }
    }
    return acc;
  }

  // ---- string ----

  public static String catStr(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Double dbl) {
      if (dbl == Math.floor(dbl) && !Double.isInfinite(dbl)) {
        return String.valueOf(dbl.longValue());
      }
    }
    return String.valueOf(value);
  }

  // ---- variable resolution ----

  public static Object resolveVar(Object data, Object key, Object defaultValue) {
    if (data == null) {
      return defaultValue;
    }
    if (key == null) {
      return (data instanceof Number num) ? num.doubleValue() : data;
    }
    if (key instanceof Number numKey) {
      final int idx = numKey.intValue();
      if (ArrayLike.isEligible(data)) {
        final var list = new ArrayLike(data);
        return (idx >= 0 && idx < list.size()) ? list.get(idx) : defaultValue;
      }
      return defaultValue;
    }
    if (key instanceof String strKey) {
      if (strKey.isEmpty()) {
        return data;
      }
      final String[] parts = strKey.split("\\.", -1);
      Object cur = data;
      for (final String part : parts) {
        if (cur == null) {
          return null;
        }
        if (ArrayLike.isEligible(cur)) {
          try {
            final var list = new ArrayLike(cur);
            final int idx = Integer.parseInt(part);
            if (idx < 0 || idx >= list.size()) {
              return defaultValue;
            }
            cur = list.get(idx);
          } catch (NumberFormatException ex) {
            return null;
          }
        } else if (cur instanceof Map<?, ?> map) {
          if (!map.containsKey(part)) {
            return defaultValue;
          }
          final Object raw = map.get(part);
          cur = (raw instanceof Number rawNum) ? rawNum.doubleValue() : raw;
        } else {
          return null;
        }
      }
      return cur;
    }
    return defaultValue;
  }
}
