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
    final Object var_customer_3 = resolveVarChecked(data, "customer", null);
    final Object var_action_5 = resolveVarChecked(data, "action", null);
    Object result_0;
    Object ifCond_1 = null;
    boolean andV_2 = strictEq(var_customer_3, "cus1");
    ifCond_1 = andV_2;
    if (JsonLogic.truthy(ifCond_1)) {
      boolean andV_4 = strictEq(var_action_5, "login");
      ifCond_1 = andV_4;
    }
    if (JsonLogic.truthy(ifCond_1)) {
    Object ifCons_6 = "log";
      result_0 = ifCons_6;
    } else {
    Object ifElse_7;
    Object ifCond_8 = null;
    boolean andV_9 = strictEq(var_customer_3, "cus2");
    ifCond_8 = andV_9;
    if (JsonLogic.truthy(ifCond_8)) {
      boolean andV_10 = strictEq(var_action_5, "delete");
      ifCond_8 = andV_10;
    }
    if (JsonLogic.truthy(ifCond_8)) {
    Object ifCons_11 = "log";
      ifElse_7 = ifCons_11;
    } else {
    Object ifElse_12 = "allow";
    ifElse_7 = ifElse_12;
    }
    result_0 = ifElse_7;
    }
    return result_0;
  }
}

