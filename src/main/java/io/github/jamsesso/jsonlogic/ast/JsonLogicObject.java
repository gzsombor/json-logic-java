package io.github.jamsesso.jsonlogic.ast;

import java.util.Map;

public class JsonLogicObject implements JsonLogicNode {
  private final Map<String, JsonLogicNode> entries;

  public JsonLogicObject(Map<String, JsonLogicNode> entries) {
    this.entries = entries;
  }

  @Override
  public JsonLogicNodeType getType() {
    return JsonLogicNodeType.OBJECT;
  }

  public Map<String, JsonLogicNode> getEntries() {
    return entries;
  }
}
