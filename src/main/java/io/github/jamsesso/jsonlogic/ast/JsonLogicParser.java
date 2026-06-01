package io.github.jamsesso.jsonlogic.ast;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonLogicParser {
  private JsonLogicParser() {
    // Utility class has no public constructor.
  }

  public static JsonLogicNode parse(String json) throws JsonLogicParseException {
    try {
      return parse(JsonParser.parseString(json), false);
    } catch (JsonLogicParseException e) {
      e.prependPartialJsonPath("$");
      throw e;
    } catch (JsonSyntaxException e) {
      throw new JsonLogicParseException(e, "$");
    }
  }

  private static JsonLogicNode parse(JsonElement root, boolean allowObjectLiteral) throws JsonLogicParseException {
    // Handle null
    if (root.isJsonNull()) {
      return JsonLogicNull.NULL;
    }

    // Handle primitives
    if (root.isJsonPrimitive()) {
      JsonPrimitive primitive = root.getAsJsonPrimitive();

      if (primitive.isString()) {
        return new JsonLogicString(primitive.getAsString());
      }

      if (primitive.isNumber()) {
        return new JsonLogicNumber(primitive.getAsNumber());
      }

      if (primitive.isBoolean() && primitive.getAsBoolean()) {
        return JsonLogicBoolean.TRUE;
      } else {
        return JsonLogicBoolean.FALSE;
      }
    }

    // Handle arrays
    if (root.isJsonArray()) {
      JsonArray array = root.getAsJsonArray();
      List<JsonLogicNode> elements = new ArrayList<>(array.size());

      for (int index = 0; index < array.size(); index++) {
        JsonElement element = array.get(index);
        JsonLogicNode arrayNode;
        try {
          arrayNode = parse(element, false);
        } catch (JsonLogicParseException e) {
          e.prependPartialJsonPath("[" + (index) + "]");
          throw e;
        }
        elements.add(arrayNode);
      }

      return new JsonLogicArray(elements);
    }

    // Handle objects & variables
    JsonObject object = root.getAsJsonObject();

    if (object.keySet().size() != 1) {
      if (allowObjectLiteral && object.keySet().size() > 1) {
        return parseObjectLiteral(object);
      }

      throw new JsonLogicParseException("objects must have exactly 1 key defined, found " + object.keySet().size());
    }

    String key = object.keySet().stream().findAny().get();
    JsonLogicNode argumentNode;
    JsonLogicArray arguments;

    try {
      JsonElement argumentElement = object.get(key);
      if (("if".equals(key) || "?:".equals(key)) && argumentElement.isJsonArray()) {
        arguments = parseIfArguments(argumentElement.getAsJsonArray());
      } else {
        argumentNode = parse(argumentElement, false);
        // Always coerce single-argument operations into a JsonLogicArray with a single element.
        if (argumentNode instanceof JsonLogicArray) {
          arguments = (JsonLogicArray) argumentNode;
        } else {
          arguments = new JsonLogicArray(Collections.singletonList(argumentNode));
        }
      }
    } catch (JsonLogicParseException e) {
      e.prependPartialJsonPath("." + key);
      throw e;
    }

    // Special case for variable handling
    if ("var".equals(key)) {
      JsonLogicNode defaultValue = arguments.size() > 1 ? arguments.get(1) : JsonLogicNull.NULL;
      return new JsonLogicVariable(arguments.size() < 1 ? JsonLogicNull.NULL : arguments.get(0), defaultValue);
    }

    // Handle regular operations
    return new JsonLogicOperation(key, arguments);
  }

  private static JsonLogicObject parseObjectLiteral(JsonObject object) throws JsonLogicParseException {
    Map<String, JsonLogicNode> entries = new LinkedHashMap<>();
    for (String objectKey : object.keySet()) {
      try {
        entries.put(objectKey, parse(object.get(objectKey), true));
      } catch (JsonLogicParseException e) {
        e.prependPartialJsonPath("." + objectKey);
        throw e;
      }
    }

    return new JsonLogicObject(entries);
  }

  private static JsonLogicArray parseIfArguments(JsonArray array) throws JsonLogicParseException {
    List<JsonLogicNode> elements = new ArrayList<>(array.size());

    for (int index = 0; index < array.size(); index++) {
      try {
        boolean isResultPosition = index % 2 == 1;
        boolean isTrailingElse = index > 0 && index == array.size() - 1 && array.size() % 2 == 1;
        elements.add(parse(array.get(index), isResultPosition || isTrailingElse));
      } catch (JsonLogicParseException e) {
        e.prependPartialJsonPath("[" + index + "]");
        throw e;
      }
    }

    return new JsonLogicArray(elements);
  }
}
