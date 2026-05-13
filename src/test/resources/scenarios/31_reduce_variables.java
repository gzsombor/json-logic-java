package io.github.jamsesso.jsonlogic.compiler.gen;

import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.ast.JsonLogicNode;
import io.github.jamsesso.jsonlogic.compiler.CompiledRule;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.utils.ArrayLike;
import static io.github.jamsesso.jsonlogic.compiler.RuleHelpers.*;
import static io.github.jamsesso.jsonlogic.utils.MapHelpers.reduceContext;
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
    final Object var_data_6 = resolveVarChecked(data, "data", null);
    Object reduceArray_1 = var_data_6;
    Object reduceAccumulator_2 = 0.0;
    Object result_0 = reduceAccumulator_2;
    if (ArrayLike.isEligible(reduceArray_1)) {
      Map<String, Object> reduceContext_3 = reduceContext(data, reduceAccumulator_2);
      for (Object reduceItem_4 : new ArrayLike(reduceArray_1)) {
        reduceContext_3.put("current", reduceItem_4);
        final Object var_multiplicator_7 = resolveVarChecked(reduceContext_3, "multiplicator", null);
        final Object var_current_9 = resolveVarChecked(reduceContext_3, "current", null);
        final Object var_accumulator_11 = resolveVarChecked(reduceContext_3, "accumulator", null);
        Object arith_13;
        if (var_multiplicator_7 == null || !isNumeric(var_multiplicator_7) || var_current_9 == null || !isNumeric(var_current_9) || var_accumulator_11 == null || !isNumeric(var_accumulator_11)) {
          arith_13 = null;
        } else {
          double _d_8 = toDouble(var_multiplicator_7);
          double _d_10 = toDouble(var_current_9);
          double _d_12 = toDouble(var_accumulator_11);
          arith_13 = (_d_8 * (_d_10 + _d_12));
        }
        Object reduceBody_5 = arith_13;
        reduceContext_3.put("accumulator", reduceBody_5);
      }
      result_0 = reduceContext_3.get("accumulator");
    }
    return result_0;
  }
}
