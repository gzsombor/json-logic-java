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
    final Object var_a_3 = resolveVar(data, "a", null);
    final Object var_b_5 = resolveVar(data, "b", null);
    final Object var_c_7 = resolveVar(data, "c", null);
    Object result_0 = null;
    Object orV_1 = null;
    Object andV_2 = (toDouble(var_a_3) > toDouble(Double.longBitsToDouble(0L)));
    orV_1 = andV_2;
    if (JsonLogic.truthy(orV_1)) {
      Object andV_4 = (toDouble(var_b_5) < toDouble(Double.longBitsToDouble(4621819117588971520L)));
      orV_1 = andV_4;
    }
    result_0 = orV_1;
    if (!JsonLogic.truthy(result_0)) {
      Object orV_6 = looseEq(var_c_7, "yes");
      result_0 = orV_6;
    }
    return result_0;
  }
}

