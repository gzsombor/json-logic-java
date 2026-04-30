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

  private static final Set<Object> SET_1 = new HashSet<>(Arrays.asList("cust1", "cust2"));

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
    final Object var_customer_2 = resolveVarChecked(data, "customer", null);
    Object result_0;
    if (SET_1.contains(var_customer_2)) {
    Object ifCons_3 = "ok";
      result_0 = ifCons_3;
    } else {
    Object ifElse_4 = "not_ok";
    result_0 = ifElse_4;
    }
    return result_0;
  }
}

