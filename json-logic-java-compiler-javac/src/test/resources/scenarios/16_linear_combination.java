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
    Object var_a_1 = UNRESOLVED;
    Object var_b_3 = UNRESOLVED;
    Object var_c_5 = UNRESOLVED;
    Object arith_7;
    if ((var_a_1 != UNRESOLVED ? var_a_1 : (var_a_1 = resolveVarChecked(data, "a", null))) == null || !isNumeric((var_a_1 != UNRESOLVED ? var_a_1 : (var_a_1 = resolveVarChecked(data, "a", null)))) || (var_b_3 != UNRESOLVED ? var_b_3 : (var_b_3 = resolveVarChecked(data, "b", null))) == null || !isNumeric((var_b_3 != UNRESOLVED ? var_b_3 : (var_b_3 = resolveVarChecked(data, "b", null)))) || (var_c_5 != UNRESOLVED ? var_c_5 : (var_c_5 = resolveVarChecked(data, "c", null))) == null || !isNumeric((var_c_5 != UNRESOLVED ? var_c_5 : (var_c_5 = resolveVarChecked(data, "c", null))))) {
      arith_7 = null;
    } else {
      double _d_2 = toDouble((var_a_1 != UNRESOLVED ? var_a_1 : (var_a_1 = resolveVarChecked(data, "a", null))));
      double _d_4 = toDouble((var_b_3 != UNRESOLVED ? var_b_3 : (var_b_3 = resolveVarChecked(data, "b", null))));
      double _d_6 = toDouble((var_c_5 != UNRESOLVED ? var_c_5 : (var_c_5 = resolveVarChecked(data, "c", null))));
      arith_7 = (((_d_2 + _d_4) * _d_6) - 100.0);
    }
    Object result_0 = arith_7;
    return result_0;
  }
}
