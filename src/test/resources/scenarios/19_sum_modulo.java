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
    final Object var_a_1 = resolveVarChecked(data, "a", null);
    final Object var_b_3 = resolveVarChecked(data, "b", null);
    final Object var_c_5 = resolveVarChecked(data, "c", null);
    final Object var_d_7 = resolveVarChecked(data, "d", null);
    final Object var_m_9 = resolveVarChecked(data, "m", null);
    Object arith_11;
    if (var_a_1 == null || !isNumeric(var_a_1) || var_b_3 == null || !isNumeric(var_b_3) || var_c_5 == null || !isNumeric(var_c_5) || var_d_7 == null || !isNumeric(var_d_7) || var_m_9 == null || !isNumeric(var_m_9)) {
      arith_11 = null;
    } else {
      double _d_2 = toDouble(var_a_1);
      double _d_4 = toDouble(var_b_3);
      double _d_6 = toDouble(var_c_5);
      double _d_8 = toDouble(var_d_7);
      double _d_10 = toDouble(var_m_9);
      arith_11 = ((_d_2 + _d_4 + _d_6 + _d_8) % _d_10);
    }
    Object result_0 = arith_11;
    return result_0;
  }
}

