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
    final Object var_a_2 = resolveVar(data, "a", null);
    final Object var_b_4 = resolveVar(data, "b", null);
    Object result_0 = null;
    Object andV_1 = (toDouble(var_a_2) > toDouble(Double.longBitsToDouble(4621819117588971520L)));
    result_0 = andV_1;
    if (JsonLogic.truthy(result_0)) {
      Object andV_3 = (toDouble(var_b_4) < toDouble(Double.longBitsToDouble(4636737291354636288L)));
      result_0 = andV_3;
    }
    return result_0;
  }
}

