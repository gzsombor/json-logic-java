package io.github.jamsesso.jsonlogic.compiler;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
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

  public static <T> T fail(String message, String path) throws JsonLogicEvaluationException {
    throw new JsonLogicEvaluationException(message, path);
  }

  // ---- equality ----

  public static boolean looseEq(Object left, Object right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    if (left instanceof Number && right instanceof Number) {
      return Double.valueOf(((Number) left).doubleValue()).equals(((Number) right).doubleValue());
    }
    if (left instanceof Number && right instanceof String) {
      return numEqStr((Number) left, (String) right);
    }
    if (left instanceof Number && right instanceof Boolean) {
      return numEqBool((Number) left, (Boolean) right);
    }
    if (left instanceof String && right instanceof String) {
      return left.equals(right);
    }
    if (left instanceof String && right instanceof Number) {
      return numEqStr((Number) right, (String) left);
    }
    if (left instanceof String && right instanceof Boolean) {
      return strEqBool((String) left, (Boolean) right);
    }
    if (left instanceof Boolean && right instanceof Boolean) {
      return ((Boolean) left).booleanValue() == ((Boolean) right).booleanValue();
    }
    if (left instanceof Boolean && right instanceof Number) {
      return numEqBool((Number) right, (Boolean) left);
    }
    if (left instanceof Boolean && right instanceof String) {
      return strEqBool((String) right, (Boolean) left);
    }
    return !JsonLogic.truthy(left) && !JsonLogic.truthy(right);
  }

  public static boolean strictEq(Object left, Object right) {
    if (left instanceof Number && right instanceof Number) {
      return ((Number) left).doubleValue() == ((Number) right).doubleValue();
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
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
      } catch (NumberFormatException ex) {
        return Double.NaN;
      }
    }
    if (value instanceof Boolean) {
      return (Boolean) value ? 1.0 : 0.0;
    }
    return Double.NaN;
  }

  public static double toComparableDouble(Object value) {
    if (value == null) {
      return 0.0;
    }
    if (value instanceof Boolean) {
      return (Boolean) value ? 1.0 : 0.0;
    }
    return toDouble(value);
  }

  /**
   * Returns true when {@code value} can be coerced to a numeric double by {@link #toDouble}.
   * Mirrors {@code toDouble}'s coercion rules: {@link Number}, numeric {@link String}, and
   * {@link Boolean} (coerced to 1.0 / 0.0) are all accepted.
   */
  public static boolean isNumeric(Object value) {
    if (value instanceof Number) {
      return true;
    }
    if (value instanceof Boolean) {
      return true;
    }
    if (value instanceof String) {
      try {
        Double.parseDouble((String) value);
        return true;
      } catch (NumberFormatException ex) {
        return false;
      }
    }
    return false;
  }

  /** Returns {@code null} for non-numeric / empty-array inputs (mirrors {@code MathExpression}). */
  public static Double toDoubleNullable(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
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
      final ArrayLike list = new ArrayLike(value);
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
      if ("+".equals(op)) {
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
    if (value instanceof Double) {
      final Double dbl = (Double) value;
      if (dbl == Math.floor(dbl) && !Double.isInfinite(dbl)) {
        return String.valueOf(dbl.longValue());
      }
    }
    return String.valueOf(value);
  }

  public static String substr(Object strArg, Object startArg, Object lengthArg, String jsonPath)
      throws JsonLogicEvaluationException {
    if (!(startArg instanceof Double)) {
      throw new JsonLogicEvaluationException("second argument to substr must be a number", jsonPath + "[1]");
    }
    final String value = String.valueOf(strArg == null ? "" : strArg);
    final int len = value.length();
    int start = ((Double) startArg).intValue();
    if (start < 0) {
      start = len + start;
    }
    if (lengthArg == null) {
      // 2-arg form: start to end
      return start < 0 ? "" : value.substring(Math.min(start, len));
    }
    if (!(lengthArg instanceof Double)) {
      throw new JsonLogicEvaluationException("third argument to substr must be an integer", jsonPath + "[2]");
    }
    int end = ((Double) lengthArg).intValue();
    if (end < 0) {
      end = len + end;
    } else {
      end = start + end;
    }
    if (start > end || end > len || start < 0) {
      return "";
    }
    return value.substring(start, end);
  }

  // ---- variable resolution ----

  public static Object resolveVar(Object data, Object key, Object defaultValue) throws JsonLogicEvaluationException {
    if (data == null) {
      return defaultValue;
    }
    if (key == null) {
      return (data instanceof Number) ? ((Number) data).doubleValue() : data;
    }
    if (key instanceof Number) {
      final int idx = ((Number) key).intValue();
      if (ArrayLike.isEligible(data)) {
        final ArrayLike list = new ArrayLike(data);
        return (idx >= 0 && idx < list.size()) ? list.get(idx) : defaultValue;
      }
      return defaultValue;
    }
    if (key instanceof String) {
      final String strKey = (String) key;
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
            final ArrayLike list = new ArrayLike(cur);
            final int idx = Integer.parseInt(part);
            if (idx < 0 || idx >= list.size()) {
              return defaultValue;
            }
            cur = list.get(idx);
          } catch (NumberFormatException ex) {
            throw new JsonLogicEvaluationException(ex, "[0]");
          }
        } else if (cur instanceof Map) {
          final Map<?, ?> map = (Map<?, ?>) cur;
          if (!map.containsKey(part)) {
            return defaultValue;
          }
          final Object raw = map.get(part);
          cur = (raw instanceof Number) ? ((Number) raw).doubleValue() : raw;
        } else {
          return null;
        }
      }
      return cur;
    }
    return defaultValue;
  }

  /**
   * Variant of {@link #resolveVar} used in generated preamble code for hoisted var locals.
   * Catches any {@link JsonLogicEvaluationException} and prepends {@code ".var"} to the path,
   * matching the path that the tree-walking evaluator produces for {@code var} errors.
   */
  public static Object resolveVarChecked(Object data, String key, Object defaultValue)
      throws JsonLogicEvaluationException {
    try {
      return resolveVar(data, key, defaultValue);
    } catch (JsonLogicEvaluationException e) {
      e.prependPartialJsonPath(".var");
      throw e;
    }
  }
}
