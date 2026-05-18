package io.github.jamsesso.jsonlogic.utils;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MapHelpers {
  private MapHelpers() { }

  public static Map<?, ?> toMap(Object data) {
    if (data instanceof Map) {
      return (Map<?, ?>) data;
    }
    else if (data instanceof JsonObject) {
      return (Map<?, ?>) JsonValueExtractor.extract((JsonObject) data);
    }

    return null;
  }

  public static Map<String, Object> reduceContext(Object data, Object accumulator) {
    Map<String, Object> context = new HashMap<>();
    Map<?, ?> dataMap = toMap(data);
    if (dataMap != null) {
      for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
        if (entry.getKey() instanceof String) {
          context.put((String) entry.getKey(), entry.getValue());
        }
      }
    }
    context.put("accumulator", accumulator);
    return context;
  }

  public static List<Object> missingKeys(List<?> keys, Map<?, ?> data) {
    Set<String> providedKeys = getFlatKeys(data);
    Set<Object> requiredKeys = new LinkedHashSet<>();

    for (Object key : keys) {
      requiredKeys.add(key);
    }

    requiredKeys.removeAll(providedKeys);

    return new ArrayList<>(requiredKeys);
  }

  /**
   * Given a map structure such as:
   * {a: {b: 1}, c: 2}
   *
   * This method will return the following set:
   * ["a.b", "c"]
   */
  private static Set<String> getFlatKeys(Map<?, ?> map) {
    return getFlatKeys(map, "");
  }

  private static Set<String> getFlatKeys(Map<?, ?> map, String prefix) {
    Set<String> keys = new LinkedHashSet<>();

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = prefix + entry.getKey();
      Map<?, ?> child = toMap(entry.getValue());

      if (child != null) {
        keys.addAll(getFlatKeys(child, key + "."));
      }
      else {
        keys.add(key);
      }
    }

    return keys;
  }
}
