package io.github.jamsesso.jsonlogic.compiler.gen;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
import java.util.*;

public final class TestRule implements CompiledRule {

  private final JsonLogicEvaluator fallback;
  private final JsonLogicNode[] fallbackNodes;
  private final String ruleJson;

  public TestRule(JsonLogicEvaluator fallback, JsonLogicNode[] fallbackNodes, String ruleJson) {
    this.fallback = fallback;
    this.fallbackNodes = fallbackNodes;
    this.ruleJson = ruleJson;
  }

  @Override
  public String toString() {
    return "CompiledRule(" + ruleJson + ")";
  }

  @Override
  public Object apply(Object data) throws JsonLogicEvaluationException {
    final Object var_enabled_2 = resolveVarChecked(data, "enabled", null);
    Object result_0;
    Object ifCond_1 = var_enabled_2;
    if (JsonLogic.truthy(ifCond_1)) {
    final Map<String, Object> object_4 = new LinkedHashMap<>();
    object_4.put("key_a", 1.0);
    object_4.put("key_b", "hello");
    Object ifCons_3 = object_4;
      result_0 = ifCons_3;
    } else {
    final Map<String, Object> object_6 = new LinkedHashMap<>();
    object_6.put("key_a", 2.0);
    object_6.put("key_b", "world");
    Object ifElse_5 = object_6;
    result_0 = ifElse_5;
    }
    return result_0;
  }
}
